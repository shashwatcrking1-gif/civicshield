# CivicShield REST API — v1

Base URL: `http://localhost:8080/api/v1` (frontend config: `CIVICSHIELD_API_BASE_URL`).

This is the contract between the CivicShield backend (Spring Boot, port 8080) and any client,
including the Vaadin frontend (port 8081). It is implemented by the `com.civicshield.api.v1`
package and is backed by the same services as the legacy Thymeleaf portal.

## Conventions

| Topic | Rule |
|---|---|
| Format | JSON, UTF-8. CSV only where noted. |
| Auth | `Authorization: Bearer <accessToken>` on every endpoint except `POST /auth/login` and `GET /system/status`. |
| Sessions | Stateless. No cookies, so no CSRF token is required. Tokens are opaque, random (256-bit), stored server-side as SHA-256 hashes, expire after `civicshield.api.token-ttl-minutes` (default 480). Restarting the backend invalidates all tokens. |
| Timestamps | ISO-8601 local date-time `yyyy-MM-ddTHH:mm:ss` in the server time-zone (reported by `/system/status.timezone`). Dates: `yyyy-MM-dd`. |
| Enums | Upper-case names (e.g. `HIGH`). |
| Pagination | `page` (0-based, default 0), `size` (default 20, max 100). Response: `Page<T>` below. |
| Data provenance | Every analytical response carries `dataProvenance`: `SIMULATED` (seeded demo data / simulated zone profiles), `OPERATIONAL`, or `HYPOTHETICAL` (What-If output). Clients **must** label `SIMULATED`/`HYPOTHETICAL` data visibly. |
| Risk score | 0–100 index from the deterministic cascade engine. It is **not** a calibrated probability (`scoreIsCalibratedProbability: false`). |

### Page&lt;T&gt;
```json
{ "content": [T], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
```

### Error body (all 4xx/5xx)
```json
{
  "timestamp": "2026-10-16T02:30:00",
  "status": 400,
  "error": "validation_failed",
  "message": "Validation failed",
  "path": "/api/v1/incidents",
  "fieldErrors": { "latitude": "Latitude must be between -90 and 90" }
}
```
`error` codes: `validation_failed` (400), `bad_request` (400), `unauthorized` (401), `invalid_credentials` (401),
`forbidden` (403), `not_found` (404), `conflict` (409), `too_many_requests` (429, with `Retry-After` seconds header),
`internal_error` (500). Stack traces are never returned.

### Enums
- `Role`: `ADMIN`, `AUTHORITY`, `EMERGENCY_SERVICE`, `HOSPITAL`
- `IncidentType`: `HEAVY_RAIN`, `FLOOD`, `WATERLOGGING`, `DRAINAGE_OVERFLOW`, `ROAD_BLOCKAGE`, `TREE_FALL`, `ROAD_ACCIDENT`, `TRAFFIC_CONGESTION`, `POWER_OUTAGE`, `MEDICAL_EMERGENCY`, `FIRE`
- `Severity` / `Priority` / `RiskLevel`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
- `IncidentStatus`: `REPORTED`, `VERIFIED`, `IN_PROGRESS`, `RESOLVED`
- `AlertStatus`: `ACTIVE`, `ACKNOWLEDGED`, `RESOLVED`
- `RecommendationStatus`: `PENDING`, `APPROVED`, `DISMISSED`
- `DataStatus`: `ONLINE`, `DELAYED`, `OFFLINE`

Risk level thresholds: Low &lt; 35 ≤ Medium &lt; 55 ≤ High &lt; 75 ≤ Critical.

### Role matrix (enforced by the backend)

| Area | ADMIN | AUTHORITY | EMERGENCY_SERVICE | HOSPITAL |
|---|---|---|---|---|
| Dashboard, map, zones, alerts (read), data sources, system | ✔ | ✔ | ✔ | ✔ |
| Incidents (read), incident alerts/recommendations | ✔ | ✔ | ✔ | ✘ |
| Incidents (create/edit/status) | ✔ | ✔ | ✘ | ✘ |
| Cascade analysis (read / re-run), risk model | ✔ | ✔ | ✘ | ✘ |
| Alerts acknowledge / resolve | ✔ | ✔ | ✔ | ✘ |
| Recommendations read / approve / dismiss | ✔ | ✔ | ✔ | ✘ |
| Simulations, reports, CSV export | ✔ | ✔ | ✘ | ✘ |
| Users | ✔ | ✘ | ✘ | ✘ |

---

## Shared objects

```jsonc
// UserSummary
{ "id": 1, "name": "Aarav Menon", "email": "admin@civicshield.in", "role": "ADMIN",
  "roleLabel": "Administrator", "enabled": true, "initials": "AM" }

// RiskSnapshot (latest stored analysis of an incident)
{ "analysisId": 7, "score": 68, "level": "HIGH", "analysedAt": "2026-10-15T21:04:00" }

// IncidentSummary
{ "id": 3, "code": "INC-0003", "type": "FLOOD", "location": "Velachery Main Road, Velachery",
  "zone": "Velachery", "latitude": 12.98, "longitude": 80.21, "severity": "HIGH",
  "status": "VERIFIED", "reportedAt": "2026-10-15T20:41:00", "reportedBy": "Priya Raghavan",
  "latestRisk": RiskSnapshot | null }

// IncidentDetail = IncidentSummary + 
{ "predictedImpact": "…text of latest analysis…", "analysisCount": 2 }

// CascadeNode
{ "key": "flood", "label": "Flood", "icon": "waves", "probability": 64, "severity": 0.70,
  "level": "HIGH", "consequence": "Waterlogging of 1–2 ft …", "upstream": "rain", "downstream": "road" }

// CascadeEdge — propagation link between consecutive nodes
{ "from": "flood", "to": "road", "baseWeight": 0.75, "effectiveWeight": 0.6375,
  "description": "Flood → Road Blockage dependency (traffic diversion ×0.85)" }

// Factor
{ "key": "rainfall", "name": "Rainfall intensity", "value": 72, "contribution": 34,
  "description": "90 mm/hr forecast for Velachery" }

// Action (engine-recommended mitigation)
{ "text": "Deploy mobile pumping units …", "priority": "HIGH", "target": "Flood" }

// EngineInputs
{ "zone": "Velachery", "rainfall": 90, "drainage": 35, "traffic": 82, "roadVulnerability": 68,
  "historical": 66, "hospitalLoad": 60, "exposure": 0.9,
  "pumps": false, "diversion": false, "greenCorridor": false, "hospitalSurge": false }

// Method — how the score was computed
{ "propagation": "next = previous × effectiveWeight + localFactor × k",
  "nodeRisk": "probability × severity × exposure",
  "score": "100 × (1 − Π(1 − damping × nodeRisk))",
  "damping": 0.55,
  "localFactorCoefficients": { "drainage": 0.25, "road": 0.20, "traffic": 0.30, "history": 0.20, "hospital": 0.20 },
  "rainFactor": 0.80,
  "thresholds": { "MEDIUM": 35, "HIGH": 55, "CRITICAL": 75 },
  "scoreIsCalibratedProbability": false }

// Outcome — full engine result
{ "riskScore": 71, "riskLevel": "HIGH", "zone": "Velachery", "engine": "JAVA_CASCADE_ENGINE",
  "explanation": "…", "breakPoint": "Break the chain at Flood: …",
  "nodes": [CascadeNode], "edges": [CascadeEdge], "factors": [Factor], "actions": [Action],
  "consequences": ["…"], "inputs": EngineInputs }
```

---

## Authentication

### `POST /auth/login` (public)
Body `{ "email": "admin@civicshield.in", "password": "…" }` →
`200 { "accessToken": "…", "tokenType": "Bearer", "expiresAt": "2026-10-16T10:30:00", "user": UserSummary }`

- `401 invalid_credentials` for unknown e-mail, wrong password or disabled account (same message, no enumeration).
- `429 too_many_requests` after 5 failed attempts for the same e-mail within 5 minutes (`Retry-After` header).

### `POST /auth/logout` → `204` (revokes the presented token)
### `GET /auth/me` → `200 UserSummary`

No password-recovery, remember-me, refresh-token or two-factor endpoints exist.

## System

### `GET /system/status` (public)
```json
{ "status": "UP", "application": "civicshield-backend", "version": "1.0.0", "apiVersion": "v1",
  "serverTime": "2026-10-16T02:30:00", "timezone": "Asia/Kolkata",
  "dataProvenance": "SIMULATED", "riskEngine": "JAVA_CASCADE_ENGINE", "aiServiceEnabled": false,
  "dataSources": { "ONLINE": 5, "DELAYED": 1, "OFFLINE": 0 } }
```
### `GET /data-sources` → `[ { "id", "name", "type", "location", "status": DataStatus } ]`

## Dashboard

### `GET /dashboard/summary`
```jsonc
{ "generatedAt": "…", "dataProvenance": "SIMULATED",
  "activeIncidents": 9,            // incidents not RESOLVED
  "highCriticalIncidents": 4,      // open incidents whose latest risk level is HIGH or CRITICAL
  "activeAlerts": 6,               // alerts with status ACTIVE
  "criticalActiveAlerts": 2,       // ACTIVE alerts with priority CRITICAL
  "areasRequiringAttention": 3,    // distinct zones with ≥1 open HIGH/CRITICAL incident
  "pendingRecommendations": 11,    // recommendations PENDING review
  "totalIncidents": 14, "simulationsRun": 3,
  "averageResponseMinutes": null,  // not tracked by the backend – always null
  "riskDistribution": { "LOW": 3, "MEDIUM": 4, "HIGH": 5, "CRITICAL": 2 },  // latest analysis of all incidents
  "areas": [ { "zone": "Velachery", "openIncidents": 3, "maxRiskScore": 81, "maxRiskLevel": "CRITICAL" } ],
  "topThreat": { "incidentId": 3, "code": "INC-0003", "location": "…", "type": "FLOOD",
                 "riskScore": 81, "riskLevel": "CRITICAL", "explanation": "…", "nodes": [CascadeNode] } | null }
```
### `GET /dashboard/trend?days=14` (1–90)
`{ "from": "2026-10-03", "to": "2026-10-16", "points": [ { "date": "2026-10-03", "incidents": 2, "highRisk": 1 } ] }`
### `GET /dashboard/activity?limit=20` (1–100)
Newest first, built from stored incident, analysis and alert timestamps:
`[ { "type": "INCIDENT_REPORTED" | "RISK_ANALYSED" | "ALERT_RAISED", "timestamp": "…", "title": "…", "description": "…", "incidentId": 3, "level": "HIGH" } ]`

## Incidents

### `GET /incidents`
Query: `q` (location text or code like `INC-0003`), `type`, `severity`, `status`, `zone`, `page`, `size`,
`sort` = `reportedAt|severity|status|location|type` + `,asc|,desc` (default `reportedAt,desc`). → `Page<IncidentSummary>`
### `GET /incidents/{id}` → `IncidentDetail`
### `POST /incidents` → `201 IncidentDetail` (+ `Location` header). Runs a cascade analysis immediately (creates alerts/recommendations).
### `PUT /incidents/{id}` → `200 IncidentDetail`. Re-runs the analysis. `409` if the incident is `RESOLVED`.
Body (`IncidentRequest`, both POST and PUT):
```json
{ "type": "FLOOD", "location": "Velachery Main Road, Velachery", "latitude": 12.98, "longitude": 80.21, "severity": "HIGH" }
```
Validation: all fields required; location 3–200 chars; latitude −90…90; longitude −180…180.
### `PATCH /incidents/{id}/status` body `{ "status": "IN_PROGRESS" }` → `200 IncidentDetail`. Setting `RESOLVED` resolves its open alerts.
### `GET /incidents/{id}/alerts` → `[Alert]`
### `GET /incidents/{id}/recommendations` → `[Recommendation]`

## Cascade risk

### `GET /incidents/{id}/cascade`
```jsonc
{ "incidentId": 3, "incidentCode": "INC-0003", "location": "…",
  "analysisId": 7, "analysedAt": "…",        // latest stored analysis (null if none)
  "storedRiskScore": 81,                     // score stored with that analysis
  "evaluatedAt": "…",                        // when this response was computed
  "dataProvenance": "SIMULATED",
  "outcome": Outcome, "method": Method }
```
### `POST /incidents/{id}/cascade` → `200` same body, after storing a fresh analysis (also creates alerts/recommendations).
### `GET /risk/model` → `{ "method": Method, "chain": [ { "key", "label", "severity" } ], "baseWeights": [CascadeEdge] }`

## Map & zones

### `GET /map/incidents?includeResolved=false`
`[ { "id", "code", "type", "location", "zone", "latitude", "longitude", "severity", "status", "riskScore", "riskLevel", "chainSummary", "reportedAt" } ]`
### `GET /zones`
`[ { "key": "velachery", "name": "Velachery", "latitude", "longitude", "drainage", "roadVulnerability", "historical", "hospitalLoad", "exposure", "baseTraffic", "diversionRoute", "hospital", "simulatedProfile": true } ]`

## Alerts

`Alert`:
```json
{ "id": 4, "message": "…", "priority": "CRITICAL", "status": "ACTIVE", "createdAt": "…",
  "source": "CASCADE_RISK_ENGINE", "incidentId": 3, "incidentCode": "INC-0003", "incidentType": "FLOOD",
  "location": "…", "zone": "Velachery", "riskScore": 81, "riskLevel": "CRITICAL" }
```
### `GET /alerts?status&priority&q&page&size` → `Page<Alert>` (newest first; `q` matches message/location/code)
### `GET /alerts/counts` → `{ "ACTIVE": 6, "ACKNOWLEDGED": 2, "RESOLVED": 9 }`
### `POST /alerts/{id}/acknowledge` → `Alert` (`409` unless `ACTIVE`)
### `POST /alerts/{id}/resolve` → `Alert` (`409` if already `RESOLVED`)
No read/unread tracking and no bulk endpoints exist.

## Recommendations

`Recommendation`:
```json
{ "id": 9, "action": "…", "priority": "HIGH", "status": "PENDING",
  "incidentId": 3, "incidentCode": "INC-0003", "incidentType": "FLOOD", "location": "…", "zone": "Velachery",
  "analysisId": 7, "riskScore": 81, "riskLevel": "CRITICAL", "analysedAt": "…",
  "rationale": "…predicted impact text of the analysis that produced it…" }
```
### `GET /recommendations?status&priority&q&page&size` → `Page<Recommendation>` (newest first)
### `GET /recommendations/counts` → `{ "PENDING": 11, "APPROVED": 3, "DISMISSED": 1 }`
### `POST /recommendations/{id}/approve` / `POST /recommendations/{id}/dismiss` → `Recommendation` (`409` unless `PENDING`)

Approving records a human decision only; CivicShield never dispatches resources or controls infrastructure.

## What-If simulations

### `GET /simulations/parameters`
```jsonc
{ "parameters": [
    { "key": "rainfall", "label": "Rainfall intensity", "unit": "mm/hr", "min": 0, "max": 200, "step": 1 },
    { "key": "traffic",  "label": "Traffic density",     "unit": "% of peak", "min": 0, "max": 100, "step": 1 },
    { "key": "drainage", "label": "Drainage capacity",   "unit": "% effective", "min": 0, "max": 100, "step": 1 } ],
  "interventions": [
    { "key": "pumps", "label": "Mobile pumps", "effect": "+25 points drainage capacity" },
    { "key": "diversion", "label": "Traffic diversion", "effect": "−20 points traffic; Flood→Road weight ×0.85" },
    { "key": "greenCorridor", "label": "Green corridor", "effect": "Traffic→Ambulance weight ×0.70" },
    { "key": "hospitalSurge", "label": "Hospital surge protocol", "effect": "Ambulance→Hospital weight ×0.70" } ],
  "zones": [ { "name": "Velachery", "defaultDrainage": 35, "defaultTraffic": 82 } ] }
```
### `POST /simulations/run`
Body: `{ "scenarioName": "optional ≤120", "zone": "Velachery", "rainfall": 90, "traffic": 80, "drainage": 40,
"pumps": true, "diversion": false, "greenCorridor": true, "hospitalSurge": false, "save": false }`
(rainfall 0–200, traffic 0–100, drainage 0–100 all required).
```jsonc
{ "dataProvenance": "HYPOTHETICAL",
  "baseline": Outcome,     // same inputs, no interventions
  "scenario": Outcome,     // inputs + selected interventions
  "curve": { "rainfall": [0,10,…,200], "baseline": [..], "scenario": [..] },
  "interventions": ["Mobile pumps", "Green corridor"],
  "assumptions": ["Zone profile values (road vulnerability, history, hospital load, exposure) are simulated", "…"],
  "savedId": 12 | null }
```
Simulations never modify incidents, analyses, alerts or recommendations.
### `GET /simulations?page&size` → `Page<{ "id", "scenarioName", "rainfall", "traffic", "baselineRisk", "summary", "createdBy" }>`
### `DELETE /simulations/{id}` → `204`

## Reports

### `GET /reports/summary?from&to&type&level&q`
```jsonc
{ "generatedAt": "…", "dataProvenance": "SIMULATED", "totalIncidents": 14,
  "byType":   { "FLOOD": 4, … },  "byLevel": { "LOW": 3, … }, "byStatus": { "REPORTED": 2, … },
  "byZone":   { "Velachery": 5, … },
  "alerts":   { "ACTIVE": 6, … },   "recommendations": { "PENDING": 11, … }, "simulationsRun": 3,
  "rows": [IncidentSummary] }
```
### `GET /reports/incidents.csv?from&to&type&level&q` → `text/csv` attachment (formula-injection safe).

## Users (ADMIN)

### `GET /users?q&role&enabled` → `[UserSummary]`
### `POST /users` → `201 UserSummary`; body `{ "name", "email", "password" (8–72, required), "role", "enabled" }`; `409` duplicate e-mail
### `PUT /users/{id}` → `200 UserSummary`; same body, `password` optional; `409` duplicate e-mail
### `POST /users/{id}/toggle-enabled` → `200 UserSummary`; `409` when disabling your own account
