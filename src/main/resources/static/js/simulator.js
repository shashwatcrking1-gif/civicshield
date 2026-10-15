/**
 * CivicShield – Interactive What-If Simulator
 * Used by Public Demo (/demo, home teaser) and Portal Simulator (/portal/simulator).
 */

(function () {
    'use strict';

    function initSimulator(containerId, options) {
        const container = document.getElementById(containerId);
        if (!container) return;

        const isPortal = options && options.isPortal;
        const apiUrl = isPortal ? '/api/simulations/run' : '/api/demo/simulate';

        // Elements
        const zoneSelect = container.querySelector('[data-sim="zone"]');
        const rainSlider = container.querySelector('[data-sim="rainfall"]');
        const trafficSlider = container.querySelector('[data-sim="traffic"]');
        const drainageSlider = container.querySelector('[data-sim="drainage"]');
        const scenarioInput = container.querySelector('[data-sim="scenarioName"]');

        const rainVal = container.querySelector('[data-sim-val="rainfall"]');
        const trafficVal = container.querySelector('[data-sim-val="traffic"]');
        const drainageVal = container.querySelector('[data-sim-val="drainage"]');

        const runBtn = container.querySelector('[data-sim="run-btn"]');
        const saveToggle = container.querySelector('[data-sim="save"]');

        // Checkboxes for interventions
        const pumpsCheck = container.querySelector('[data-sim="pumps"]');
        const diversionCheck = container.querySelector('[data-sim="diversion"]');
        const corridorCheck = container.querySelector('[data-sim="greenCorridor"]');
        const surgeCheck = container.querySelector('[data-sim="hospitalSurge"]');

        // Results displays
        const scoreEl = container.querySelector('[data-result="riskScore"]');
        const levelBadgeEl = container.querySelector('[data-result="riskLevel"]');
        const gaugeCircle = container.querySelector('[data-result="gaugeCircle"]');
        const explanationEl = container.querySelector('[data-result="explanation"]');
        const breakPointEl = container.querySelector('[data-result="breakPoint"]');
        const nodesContainer = container.querySelector('[data-result="nodes"]');
        const factorsContainer = container.querySelector('[data-result="factors"]');
        const actionsContainer = container.querySelector('[data-result="actions"]');
        const engineBadge = container.querySelector('[data-result="engineBadge"]');

        // Before vs After comparison in portal
        const beforeScoreEl = container.querySelector('[data-result="beforeScore"]');
        const afterScoreEl = container.querySelector('[data-result="afterScore"]');
        const scoreDeltaEl = container.querySelector('[data-result="scoreDelta"]');

        function updateSliderLabels() {
            if (rainSlider && rainVal) rainVal.innerText = rainSlider.value + ' mm/hr';
            if (trafficSlider && trafficVal) trafficVal.innerText = trafficSlider.value + '%';
            if (drainageSlider && drainageVal) drainageVal.innerText = drainageSlider.value + '%';
        }

        [rainSlider, trafficSlider, drainageSlider].forEach(slider => {
            if (slider) {
                slider.addEventListener('input', updateSliderLabels);
            }
        });

        // Debounce helper
        let debounceTimer = null;
        function debouncedRun() {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(runSimulation, 250);
        }

        // Live slider updates on change
        [rainSlider, trafficSlider, drainageSlider].forEach(slider => {
            if (slider) {
                slider.addEventListener('change', debouncedRun);
            }
        });

        if (zoneSelect) {
            zoneSelect.addEventListener('change', runSimulation);
        }

        [pumpsCheck, diversionCheck, corridorCheck, surgeCheck].forEach(chk => {
            if (chk) chk.addEventListener('change', runSimulation);
        });

        if (runBtn) {
            runBtn.addEventListener('click', function (e) {
                e.preventDefault();
                runSimulation();
            });
        }

        function getPayload() {
            return {
                scenarioName: scenarioInput ? scenarioInput.value : '',
                zone: zoneSelect ? zoneSelect.value : 'Velachery',
                rainfall: rainSlider ? parseFloat(rainSlider.value) : 90.0,
                traffic: trafficSlider ? parseFloat(trafficSlider.value) : 75.0,
                drainage: drainageSlider ? parseFloat(drainageSlider.value) : 40.0,
                pumps: pumpsCheck ? pumpsCheck.checked : false,
                diversion: diversionCheck ? diversionCheck.checked : false,
                greenCorridor: corridorCheck ? corridorCheck.checked : false,
                hospitalSurge: surgeCheck ? surgeCheck.checked : false,
                save: saveToggle ? saveToggle.checked : false
            };
        }

        function runSimulation() {
            if (runBtn) {
                runBtn.classList.add('opacity-75', 'cursor-not-allowed');
                runBtn.innerHTML = '<span class="inline-block animate-spin mr-2">⟳</span> Simulating...';
            }

            const payload = getPayload();
            const headers = { 'Content-Type': 'application/json' };

            // Include CSRF token if present
            const csrfToken = document.querySelector('meta[name="_csrf"]');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
            if (csrfToken && csrfHeader) {
                headers[csrfHeader.getAttribute('content')] = csrfToken.getAttribute('content');
            }

            fetch(apiUrl, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(payload)
            })
            .then(res => {
                if (!res.ok) throw new Error('Simulation failed with status ' + res.status);
                return res.json();
            })
            .then(data => {
                // If portal response (wrapper with before/after/curve)
                if (isPortal && data.after) {
                    renderResult(data.after);
                    renderPortalComparison(data);
                } else {
                    renderResult(data);
                }
            })
            .catch(err => {
                console.error('Simulator error:', err);
            })
            .finally(() => {
                if (runBtn) {
                    runBtn.classList.remove('opacity-75', 'cursor-not-allowed');
                    runBtn.innerHTML = '<i data-lucide="play" class="w-4 h-4 mr-2 inline"></i> Run Simulation';
                    if (window.lucide) window.lucide.createIcons();
                }
            });
        }

        function getLevelColor(level) {
            switch (level) {
                case 'LOW': return '#22C55E';
                case 'MEDIUM': return '#F59E0B';
                case 'HIGH': return '#F97316';
                case 'CRITICAL': return '#EF4444';
                default: return '#22D3EE';
            }
        }

        function renderResult(result) {
            const color = getLevelColor(result.riskLevel);

            if (scoreEl) {
                scoreEl.innerText = result.riskScore;
                scoreEl.style.color = color;
            }

            if (levelBadgeEl) {
                levelBadgeEl.innerText = result.riskLevel;
                levelBadgeEl.style.backgroundColor = color + '22';
                levelBadgeEl.style.color = color;
                levelBadgeEl.style.borderColor = color + '66';
            }

            if (gaugeCircle) {
                const radius = 45;
                const circumference = 2 * Math.PI * radius;
                const offset = circumference - (result.riskScore / 100) * circumference;
                gaugeCircle.style.strokeDasharray = `${circumference}`;
                gaugeCircle.style.strokeDashoffset = `${offset}`;
                gaugeCircle.style.stroke = color;
            }

            if (explanationEl) {
                explanationEl.innerText = result.explanation;
            }

            if (breakPointEl && result.breakPoint) {
                breakPointEl.innerHTML = `<i data-lucide="shield-alert" class="w-4 h-4 inline mr-1 text-teal-400"></i> ${result.breakPoint}`;
            }

            if (engineBadge) {
                engineBadge.innerText = (result.engine === 'PYTHON_AI') ? 'Python AI Active' : 'Java Engine Active';
                engineBadge.className = 'text-xs px-2.5 py-0.5 rounded-full font-mono border ' + 
                    (result.engine === 'PYTHON_AI' ? 'bg-purple-900/30 text-purple-300 border-purple-500/30' : 'bg-cyan-900/30 text-cyan-300 border-cyan-500/30');
            }

            // Render Animated Nodes
            if (nodesContainer && result.nodes) {
                nodesContainer.innerHTML = '';
                result.nodes.forEach((node, idx) => {
                    const nodeColor = getLevelColor(node.level);
                    const isLast = idx === result.nodes.length - 1;
                    const isCritical = node.probability >= 75;

                    const nodeDiv = document.createElement('div');
                    nodeDiv.className = `cascade-node ${isCritical ? 'pulse-critical' : ''} flex-1 text-center`;
                    nodeDiv.style.borderTopColor = nodeColor;
                    nodeDiv.style.borderTopWidth = '3px';

                    nodeDiv.innerHTML = `
                        <div class="text-xs text-slate-400 uppercase tracking-wider mb-1 flex items-center justify-center gap-1">
                            <i data-lucide="${node.icon || 'activity'}" class="w-3.5 h-3.5"></i>
                            <span>${node.label}</span>
                        </div>
                        <div class="text-2xl font-bold font-mono my-1" style="color: ${nodeColor}">
                            ${node.probability}%
                        </div>
                        <div class="text-[11px] text-slate-400 font-mono">
                            severity ${node.severity}
                        </div>
                    `;

                    nodesContainer.appendChild(nodeDiv);

                    // Add connector arrow between nodes
                    if (!isLast) {
                        const arrowDiv = document.createElement('div');
                        arrowDiv.className = 'hidden sm:flex items-center justify-center text-slate-500 px-1';
                        arrowDiv.innerHTML = `
                            <svg class="w-5 h-5 text-cyan-400/70" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6" />
                            </svg>
                        `;
                        nodesContainer.appendChild(arrowDiv);
                    }
                });
            }

            // Render Factor Bars
            if (factorsContainer && result.factors) {
                factorsContainer.innerHTML = '';
                result.factors.forEach(f => {
                    const fDiv = document.createElement('div');
                    fDiv.className = 'space-y-1';
                    fDiv.innerHTML = `
                        <div class="flex justify-between text-xs">
                            <span class="text-slate-300 font-medium">${f.name}</span>
                            <span class="font-mono text-cyan-400">${f.contribution}% weight</span>
                        </div>
                        <div class="w-full bg-slate-800 rounded-full h-2 overflow-hidden border border-slate-700/60">
                            <div class="bg-gradient-to-r from-cyan-400 to-indigo-500 h-2 rounded-full transition-all duration-500" style="width: ${f.contribution}%"></div>
                        </div>
                        <p class="text-[11px] text-slate-400">${f.description}</p>
                    `;
                    factorsContainer.appendChild(fDiv);
                });
            }

            // Render Actions
            if (actionsContainer && result.actions) {
                actionsContainer.innerHTML = '';
                if (result.actions.length === 0) {
                    actionsContainer.innerHTML = '<p class="text-sm text-slate-400 italic">No critical actions required at current risk level.</p>';
                } else {
                    result.actions.forEach(a => {
                        const actColor = getLevelColor(a.priority);
                        const aDiv = document.createElement('div');
                        aDiv.className = 'p-3 rounded-lg border border-slate-700/60 bg-slate-800/40 flex items-start gap-3';
                        aDiv.innerHTML = `
                            <span class="px-2 py-0.5 rounded text-[10px] font-mono font-bold uppercase mt-0.5" style="background-color: ${actColor}22; color: ${actColor}; border: 1px solid ${actColor}44">
                                ${a.priority}
                            </span>
                            <div class="flex-1">
                                <p class="text-xs sm:text-sm text-slate-200">${a.text}</p>
                                <span class="text-[10px] text-cyan-400 font-mono">Targets: ${a.target}</span>
                            </div>
                        `;
                        actionsContainer.appendChild(aDiv);
                    });
                }
            }

            if (window.lucide) window.lucide.createIcons();
        }

        function renderPortalComparison(data) {
            const before = data.before;
            const after = data.after;
            if (beforeScoreEl) beforeScoreEl.innerText = before.riskScore;
            if (afterScoreEl) afterScoreEl.innerText = after.riskScore;
            if (scoreDeltaEl) {
                const delta = before.riskScore - after.riskScore;
                if (delta > 0) {
                    scoreDeltaEl.innerText = `−${delta} pts reduced`;
                    scoreDeltaEl.className = 'text-emerald-400 font-mono text-sm font-semibold';
                } else if (delta < 0) {
                    scoreDeltaEl.innerText = `+${Math.abs(delta)} pts`;
                    scoreDeltaEl.className = 'text-rose-400 font-mono text-sm font-semibold';
                } else {
                    scoreDeltaEl.innerText = 'No change';
                    scoreDeltaEl.className = 'text-slate-400 font-mono text-sm';
                }
            }

            // Update portal Chart if available
            if (window.CivicShieldCharts && data.curve) {
                window.CivicShieldCharts.updateCurveChart(data.curve);
            }
        }

        // Run initial calculation
        updateSliderLabels();
    }

    window.CivicShieldSimulator = {
        init: initSimulator
    };
})();
