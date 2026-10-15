/**
 * CivicShield – Interactive Crisis Map (Leaflet.js)
 * Supports colored markers with pulse, zone risk circles, filtering, and summary drawer.
 */

(function () {
    'use strict';

    function initMap(elementId, options) {
        const mapContainer = document.getElementById(elementId);
        if (!mapContainer || typeof L === 'undefined') return;

        const defaultLat = (options && options.lat) || 13.0200;
        const defaultLng = (options && options.lng) || 80.2300;
        const defaultZoom = (options && options.zoom) || 12;

        const map = L.map(elementId, {
            zoomControl: true,
            scrollWheelZoom: !(options && options.static)
        }).setView([defaultLat, defaultLng], defaultZoom);

        // CartoDB Dark Matter / Positron based on theme
        const isLight = document.documentElement.classList.contains('light');
        const tileLayerUrl = isLight 
            ? 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png'
            : 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';

        L.tileLayer(tileLayerUrl, {
            attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
            maxZoom: 19
        }).addTo(map);

        const markersLayer = L.layerGroup().addTo(map);
        const zonesLayer = L.layerGroup().addTo(map);

        let allIncidents = [];

        // Severity / Risk color mapper
        function getRiskColor(level) {
            switch (level) {
                case 'Low': return '#22C55E';
                case 'Medium': return '#F59E0B';
                case 'High': return '#F97316';
                case 'Critical': return '#EF4444';
                default: return '#22D3EE';
            }
        }

        function createPulsingIcon(color, isCritical) {
            const pulseClass = isCritical ? 'pulse-critical' : '';
            return L.divIcon({
                className: 'custom-map-marker',
                html: `
                    <div style="position: relative; width: 28px; height: 28px;">
                        <div class="${pulseClass}" style="
                            position: absolute; width: 24px; height: 24px; border-radius: 50%;
                            background-color: ${color}; opacity: 0.85; border: 2px solid #ffffff;
                            box-shadow: 0 0 10px ${color}; display: flex; align-items: center; justify-content: center;
                        ">
                            <div style="width: 8px; height: 8px; border-radius: 50%; background: #ffffff;"></div>
                        </div>
                    </div>
                `,
                iconSize: [28, 28],
                iconAnchor: [14, 14],
                popupAnchor: [0, -14]
            });
        }

        // Fetch incidents from /api/map/incidents
        fetch('/api/map/incidents')
            .then(res => res.json())
            .then(data => {
                allIncidents = data;
                renderIncidents(allIncidents);
                renderRiskZones(options && options.zones);
            })
            .catch(err => console.error('Error fetching map incidents:', err));

        function renderIncidents(incidents) {
            markersLayer.clearLayers();

            incidents.forEach(item => {
                const color = getRiskColor(item.riskLevel);
                const isCritical = item.riskLevel === 'Critical';
                const marker = L.marker([item.latitude, item.longitude], {
                    icon: createPulsingIcon(color, isCritical)
                });

                const popupHtml = `
                    <div class="p-1 space-y-2 min-w-[200px]">
                        <div class="flex items-center justify-between border-b border-slate-700/60 pb-1.5">
                            <span class="font-mono text-xs font-bold text-cyan-400">${item.code}</span>
                            <span class="text-[10px] px-2 py-0.5 rounded-full font-bold uppercase" 
                                  style="background-color: ${color}22; color: ${color}; border: 1px solid ${color}44">
                                ${item.riskLevel} (${item.riskScore})
                            </span>
                        </div>
                        <h4 class="text-sm font-semibold text-slate-100">${item.type}</h4>
                        <p class="text-xs text-slate-300 leading-snug">${item.location}</p>
                        <div class="text-[11px] text-cyan-300/90 font-mono bg-cyan-950/40 p-1.5 rounded border border-cyan-800/40">
                            <strong>Cascade:</strong> ${item.chainSummary}
                        </div>
                        <div class="pt-1 flex items-center justify-between">
                            <span class="text-[10px] text-slate-400">${item.status}</span>
                            <a href="/portal/incidents/${item.incidentId}" 
                               class="text-xs text-teal-400 hover:text-teal-300 font-semibold underline">
                                Inspect Chain →
                            </a>
                        </div>
                    </div>
                `;

                marker.bindPopup(popupHtml);

                marker.on('click', () => {
                    if (window.CivicShieldMapDrawer) {
                        window.CivicShieldMapDrawer.open(item);
                    }
                });

                markersLayer.addLayer(marker);
            });
        }

        // Draw circles around predefined Chennai zones
        function renderRiskZones(zones) {
            if (!zones || !zones.length) return;
            zonesLayer.clearLayers();

            zones.forEach(z => {
                const color = z.roadVulnerability > 60 ? '#EF4444' : '#F59E0B';
                const circle = L.circle([z.lat, z.lng], {
                    color: color,
                    fillColor: color,
                    fillOpacity: 0.12,
                    radius: 1200,
                    weight: 1.5,
                    dashArray: '4 4'
                });
                circle.bindTooltip(`<b>${z.name}</b><br>Drainage: ${z.drainage}% | Traffic: ${z.baseTraffic}%`, {
                    sticky: true,
                    className: 'glass-panel text-xs text-slate-200'
                });
                zonesLayer.addLayer(circle);
            });
        }

        // Filtering
        function filterByLevel(level) {
            if (!level || level === 'ALL') {
                renderIncidents(allIncidents);
            } else {
                const filtered = allIncidents.filter(i => i.riskLevel.toUpperCase() === level.toUpperCase());
                renderIncidents(filtered);
            }
        }

        // Wire map filters if present
        document.querySelectorAll('[data-map-filter]').forEach(btn => {
            btn.addEventListener('click', function () {
                document.querySelectorAll('[data-map-filter]').forEach(b => b.classList.remove('bg-cyan-500/20', 'border-cyan-500'));
                this.classList.add('bg-cyan-500/20', 'border-cyan-500');
                filterByLevel(this.getAttribute('data-map-filter'));
            });
        });

        return {
            map: map,
            filterByLevel: filterByLevel
        };
    }

    window.CivicShieldMap = {
        init: initMap
    };
})();
