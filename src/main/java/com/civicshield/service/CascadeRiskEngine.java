package com.civicshield.service;

import com.civicshield.dto.CascadeNodeDto;
import com.civicshield.dto.FactorDto;
import com.civicshield.dto.SimulationRequest;
import com.civicshield.dto.SimulationResult;
import com.civicshield.dto.SimulationResult.ActionDto;
import com.civicshield.model.Priority;
import com.civicshield.model.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * CivicShield's core: a deterministic, explainable cascade risk engine written in Java.
 *
 * <h2>Model</h2>
 * The ordered chain is Rain → Flood → Road Blockage → Traffic → Ambulance Delay → Hospital Impact.
 * <ul>
 *   <li><b>Propagation</b> – each node's probability is derived from the previous one:
 *       {@code next = previous × dependencyWeight + localFactor × k}. The local factor is a
 *       zone/condition input (drainage deficit, road vulnerability, traffic, history, hospital load).</li>
 *   <li><b>Node risk</b> – {@code Probability × Severity × Exposure}.</li>
 *   <li><b>Overall score</b> – node risks are combined as a damped "at least one impact"
 *       probability {@code 100 × (1 − Π(1 − d × r_i))}, normalised to 0-100.</li>
 * </ul>
 * Interventions (pumps, diversion, green corridor, hospital surge) modify the inputs or weaken a
 * dependency link – that is how authorities "break the chain" in the simulator.
 */
@Component
public class CascadeRiskEngine {

    // --- Dependency weights between consecutive nodes ---
    static final double RAIN_FACTOR = 0.80;   // mm/hr → rain-node probability
    static final double W_FLOOD = 0.70;       // rain → flood
    static final double W_ROAD = 0.75;        // flood → road blockage
    static final double W_TRAFFIC = 0.60;     // road blockage → traffic
    static final double W_AMBULANCE = 0.65;   // traffic → ambulance delay
    static final double W_HOSPITAL = 0.55;    // ambulance delay → hospital impact

    // --- Local factor coefficients ---
    static final double K_DRAINAGE = 0.25;
    static final double K_ROAD = 0.20;
    static final double K_TRAFFIC = 0.30;
    static final double K_HISTORY = 0.20;
    static final double K_HOSPITAL = 0.20;

    /** Damping used when combining node risks into one score. */
    static final double DAMPING = 0.55;

    /** Chain definition: key, label, Lucide icon, severity weight. */
    private static final String[][] CHAIN = {
            {"rain", "Rain", "cloud-rain", "0.40"},
            {"flood", "Flood", "waves", "0.70"},
            {"road", "Road Blockage", "construction", "0.60"},
            {"traffic", "Traffic", "car", "0.50"},
            {"ambulance", "Ambulance Delay", "ambulance", "0.90"},
            {"hospital", "Hospital Impact", "hospital", "1.00"},
    };

    /** Static profile of a city zone (simulated Chennai data). */
    public record ZoneProfile(String name, double lat, double lng, int drainage, int roadVulnerability,
                              int historical, int hospitalLoad, double exposure, int baseTraffic,
                              String diversionRoute, String hospital) {
    }

    /** Fully resolved engine input. */
    public record EngineInput(String zone, double rainfall, double drainage, double traffic,
                              double roadVulnerability, double historical, double hospitalLoad,
                              double exposure, boolean pumps, boolean diversion,
                              boolean greenCorridor, boolean hospitalSurge) {

        EngineInput with(boolean p, boolean d, boolean g, boolean h) {
            return new EngineInput(zone, rainfall, drainage, traffic, roadVulnerability, historical,
                    hospitalLoad, exposure, p, d, g, h);
        }
    }

    private static final Map<String, ZoneProfile> ZONES = new LinkedHashMap<>();
    private static final ZoneProfile DEFAULT_ZONE =
            new ZoneProfile("Chennai", 13.0300, 80.2300, 50, 55, 45, 55, 0.80, 70,
                    "the nearest arterial road", "the nearest government hospital");

    static {
        ZONES.put("velachery", new ZoneProfile("Velachery", 12.9815, 80.2180, 35, 68, 66, 60, 0.90, 82,
                "Velachery Bypass → 100 Feet Road", "Velachery Govt. Hospital"));
        ZONES.put("adyar", new ZoneProfile("Adyar", 13.0012, 80.2565, 55, 52, 48, 55, 0.80, 64,
                "Sardar Patel Road → Kotturpuram", "Adyar Cancer Institute & Malar Hospital"));
        ZONES.put("t. nagar", new ZoneProfile("T. Nagar", 13.0418, 80.2341, 50, 60, 54, 65, 0.95, 88,
                "Anna Salai → Habibullah Road", "Govt. Hospital, T. Nagar"));
        ZONES.put("guindy", new ZoneProfile("Guindy", 13.0067, 80.2206, 60, 58, 40, 50, 0.85, 76,
                "Inner Ring Road → Mount Road", "Govt. Hospital for Thoracic Medicine, Guindy"));
    }

    // ------------------------------------------------------------------ zones

    public List<ZoneProfile> zones() {
        return new ArrayList<>(ZONES.values());
    }

    public List<String> zoneNames() {
        return ZONES.values().stream().map(ZoneProfile::name).toList();
    }

    /** Resolves a zone by exact or partial name, falling back to a city-wide default profile. */
    public ZoneProfile zone(String name) {
        if (name == null || name.isBlank()) return ZONES.get("velachery");
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (ZONES.containsKey(n)) return ZONES.get(n);
        return zoneForLocation(name);
    }

    /** Finds the zone whose name appears in a free-text location ("LB Road, Adyar" → Adyar). */
    public ZoneProfile zoneForLocation(String location) {
        if (location == null) return DEFAULT_ZONE;
        String l = location.toLowerCase(Locale.ROOT).replace("tnagar", "t. nagar").replace("t nagar", "t. nagar");
        return ZONES.entrySet().stream()
                .filter(e -> l.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(DEFAULT_ZONE);
    }

    // ------------------------------------------------------------------ public API

    /** Runs a What-If request using the zone profile for the inputs the user did not set. */
    public SimulationResult simulate(SimulationRequest r) {
        return analyze(inputFor(r));
    }

    public EngineInput inputFor(SimulationRequest r) {
        ZoneProfile z = zone(r.getZone());
        return new EngineInput(z.name(),
                nz(r.getRainfall(), 20),
                nz(r.getDrainage(), z.drainage()),
                nz(r.getTraffic(), z.baseTraffic()),
                z.roadVulnerability(), z.historical(), z.hospitalLoad(), z.exposure(),
                r.isPumps(), r.isDiversion(), r.isGreenCorridor(), r.isHospitalSurge());
    }

    /** Full analysis including the "best break point" search. */
    public SimulationResult analyze(EngineInput in) {
        return analyze(in, true);
    }

    /** Only the 0-100 score – used for curves (risk vs rainfall). */
    public int score(EngineInput in) {
        return computeScore(propagate(in), in.exposure());
    }

    // ------------------------------------------------------------------ internals

    private SimulationResult analyze(EngineInput in, boolean findBreakPoint) {
        double[] p = propagate(in);
        int score = computeScore(p, in.exposure());
        RiskLevel level = RiskLevel.fromScore(score);
        ZoneProfile z = zone(in.zone());

        List<CascadeNodeDto> nodes = new ArrayList<>();
        for (int i = 0; i < CHAIN.length; i++) {
            int prob = (int) Math.round(p[i]);
            nodes.add(CascadeNodeDto.builder()
                    .key(CHAIN[i][0]).label(CHAIN[i][1]).icon(CHAIN[i][2])
                    .severity(Double.parseDouble(CHAIN[i][3]))
                    .probability(prob)
                    .level(RiskLevel.fromScore(prob))
                    .consequence(consequence(CHAIN[i][0], prob, z))
                    .build());
        }

        List<FactorDto> factors = factors(in, p);
        SimulationResult result = SimulationResult.builder()
                .riskScore(score)
                .riskLevel(level)
                .zone(in.zone())
                .engine("JAVA_CASCADE_ENGINE")
                .nodes(nodes)
                .factors(factors)
                .explanation(explain(in, nodes, factors, level))
                .consequences(nodes.stream().filter(n -> n.getProbability() >= 35)
                        .map(CascadeNodeDto::getConsequence).toList())
                .actions(actions(nodes, z, in))
                .build();

        if (findBreakPoint) result.setBreakPoint(breakPoint(in, score));
        return result;
    }

    /** Propagates probabilities down the chain. Returns 6 values in [0, 99]. */
    double[] propagate(EngineInput in) {
        double drainage = clamp(in.drainage() + (in.pumps() ? 25 : 0));
        double traffic = clamp(in.traffic() - (in.diversion() ? 20 : 0));
        double wRoad = W_ROAD * (in.diversion() ? 0.85 : 1.0);
        double wAmb = W_AMBULANCE * (in.greenCorridor() ? 0.70 : 1.0);
        double wHosp = W_HOSPITAL * (in.hospitalSurge() ? 0.70 : 1.0);

        double rain = clamp(in.rainfall() * RAIN_FACTOR);
        double flood = clamp(rain * W_FLOOD + (100 - drainage) * K_DRAINAGE);
        double road = clamp(flood * wRoad + in.roadVulnerability() * K_ROAD);
        double traf = clamp(road * W_TRAFFIC + traffic * K_TRAFFIC);
        double amb = clamp(traf * wAmb + in.historical() * K_HISTORY);
        double hosp = clamp(amb * wHosp + in.hospitalLoad() * K_HOSPITAL);
        return new double[]{rain, flood, road, traf, amb, hosp};
    }

    /** Risk = Probability × Severity × Exposure per node, combined and normalised to 0-100. */
    int computeScore(double[] p, double exposure) {
        double survive = 1.0;
        for (int i = 0; i < p.length; i++) {
            double nodeRisk = (p[i] / 100.0) * Double.parseDouble(CHAIN[i][3]) * exposure;
            survive *= (1 - DAMPING * nodeRisk);
        }
        return (int) Math.round(Math.max(0, Math.min(100, 100 * (1 - survive))));
    }

    private List<FactorDto> factors(EngineInput in, double[] p) {
        double drainage = clamp(in.drainage() + (in.pumps() ? 25 : 0));
        double traffic = clamp(in.traffic() - (in.diversion() ? 20 : 0));
        Object[][] raw = {
                {"rainfall", "Rainfall intensity", p[0], 0.30,
                        String.format("%.0f mm/hr forecast for %s", in.rainfall(), in.zone())},
                {"drainage", "Drainage deficit", 100 - drainage, 0.25,
                        String.format("Storm-water drains at %.0f%% effective capacity", drainage)},
                {"traffic", "Traffic density", traffic, 0.20,
                        String.format("%.0f%% of peak road occupancy", traffic)},
                {"road", "Road vulnerability", in.roadVulnerability(), 0.15,
                        "Low-lying stretches, underpasses and narrow carriageways"},
                {"history", "Historical incidents", in.historical(), 0.10,
                        "Frequency of past flooding and gridlock in this zone"},
        };
        double total = 0;
        for (Object[] f : raw) total += ((Double) f[2]) * (Double) f[3];
        List<FactorDto> list = new ArrayList<>();
        for (Object[] f : raw) {
            double v = (Double) f[2];
            int share = total <= 0 ? 0 : (int) Math.round(v * (Double) f[3] / total * 100);
            list.add(FactorDto.builder().key((String) f[0]).name((String) f[1])
                    .value((int) Math.round(v)).contribution(share).description((String) f[4]).build());
        }
        list.sort(Comparator.comparingInt(FactorDto::getContribution).reversed());
        return list;
    }

    private String explain(EngineInput in, List<CascadeNodeDto> n, List<FactorDto> factors, RiskLevel level) {
        FactorDto top = factors.get(0);
        if (level == RiskLevel.LOW) {
            return String.format("Conditions in %s are stable: %s is the largest driver (%d%%), but the chain "
                    + "loses strength quickly after %s (%d%%).", in.zone(), top.getName().toLowerCase(),
                    top.getContribution(), n.get(0).getLabel(), n.get(0).getProbability());
        }
        return String.format("%s is the dominant driver (%d%% of risk). A %d%% flood probability is likely to "
                        + "cascade into road blockage (%d%%) and ambulance delays (%d%%) in %s.",
                top.getName(), top.getContribution(), n.get(1).getProbability(), n.get(2).getProbability(),
                n.get(4).getProbability(), in.zone());
    }

    /** Tests each intervention and reports the one that lowers the score the most. */
    private String breakPoint(EngineInput in, int baseScore) {
        record Option(String node, String label, EngineInput input) {}
        List<Option> options = List.of(
                new Option("Flood", "deploying mobile pumps", in.with(true, in.diversion(), in.greenCorridor(), in.hospitalSurge())),
                new Option("Road Blockage", "activating traffic diversion", in.with(in.pumps(), true, in.greenCorridor(), in.hospitalSurge())),
                new Option("Ambulance Delay", "opening a green corridor", in.with(in.pumps(), in.diversion(), true, in.hospitalSurge())),
                new Option("Hospital Impact", "triggering hospital surge protocol", in.with(in.pumps(), in.diversion(), in.greenCorridor(), true)));
        Option best = null;
        int bestDrop = 0;
        for (Option o : options) {
            if (o.input().equals(in)) continue; // already applied
            int drop = baseScore - score(o.input());
            if (drop > bestDrop) {
                bestDrop = drop;
                best = o;
            }
        }
        if (best == null || bestDrop < 2) {
            return "No single intervention changes the outlook significantly – keep monitoring.";
        }
        return String.format("Break the chain at %s: %s lowers the overall risk by %d points.",
                best.node(), best.label(), bestDrop);
    }

    private String consequence(String key, int p, ZoneProfile z) {
        return switch (key) {
            case "rain" -> String.format("Intense rainfall over %s with a %d%% likelihood of exceeding drain design limits", z.name(), p);
            case "flood" -> String.format("Waterlogging of 1–2 ft on low-lying streets of %s (%d%%)", z.name(), p);
            case "road" -> String.format("Underpasses and arterial roads become impassable (%d%%)", p);
            case "traffic" -> String.format("Gridlock spreading to neighbouring junctions, travel time ×2–3 (%d%%)", p);
            case "ambulance" -> String.format("108 ambulance response delayed by 12–25 minutes (%d%%)", p);
            case "hospital" -> String.format("Emergency load surge at %s (%d%%)", z.hospital(), p);
            default -> "";
        };
    }

    /** Rule-based recommendations, ordered by urgency. */
    private List<ActionDto> actions(List<CascadeNodeDto> n, ZoneProfile z, EngineInput in) {
        List<ActionDto> a = new ArrayList<>();
        int rain = n.get(0).getProbability(), flood = n.get(1).getProbability(), road = n.get(2).getProbability(),
                traffic = n.get(3).getProbability(), amb = n.get(4).getProbability(), hosp = n.get(5).getProbability();

        if (flood >= 55 && !in.pumps())
            a.add(new ActionDto("Deploy mobile pumping units to low-lying streets of " + z.name() + " within 30 minutes",
                    Priority.fromProbability(flood), "Flood"));
        if (road >= 50 && !in.diversion())
            a.add(new ActionDto("Close flooded underpasses and divert traffic via " + z.diversionRoute(),
                    Priority.fromProbability(road), "Road Blockage"));
        if (amb >= 45 && !in.greenCorridor())
            a.add(new ActionDto("Pre-position 108 ambulances on the dry side of " + z.name() + " and open a green corridor",
                    Priority.fromProbability(amb), "Ambulance Delay"));
        if (hosp >= 35 && !in.hospitalSurge())
            a.add(new ActionDto("Alert " + z.hospital() + " to activate surge protocol and reserve emergency beds",
                    Priority.fromProbability(hosp), "Hospital Impact"));
        if (traffic >= 55)
            a.add(new ActionDto("Station traffic police at key junctions and push route advisories to commuters",
                    Priority.fromProbability(traffic), "Traffic"));
        if (rain >= 70)
            a.add(new ActionDto("Issue public advisory: avoid non-essential travel in " + z.name() + " for the next 6 hours",
                    Priority.fromProbability(rain), "Rain"));
        if (a.isEmpty())
            a.add(new ActionDto("Continue routine monitoring – no immediate cascade expected in " + z.name(),
                    Priority.LOW, "Rain"));

        a.sort(Comparator.comparing((ActionDto x) -> x.getPriority().ordinal()).reversed());
        return a;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(99, v));
    }

    private static double nz(Double v, double fallback) {
        return v == null ? fallback : v;
    }
}
