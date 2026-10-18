/**
 * CivicShield – Charts Engine (Chart.js)
 * Trend line charts, risk-distribution donuts, and What-If curve charts.
 */

(function () {
    'use strict';

    let curveChartInstance = null;

    function initTrendChart(canvasId) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || typeof Chart === 'undefined') return;

        fetch('/api/dashboard/trend?days=14')
            .then(res => res.json())
            .then(data => {
                new Chart(canvas.getContext('2d'), {
                    type: 'line',
                    data: {
                        labels: data.labels,
                        datasets: [
                            {
                                label: 'Total Incidents',
                                data: data.incidents,
                                borderColor: '#6366F1',
                                backgroundColor: 'rgba(99, 102, 241, 0.1)',
                                fill: true,
                                tension: 0.35,
                                pointBackgroundColor: '#6366F1',
                                pointRadius: 4
                            },
                            {
                                label: 'High / Critical Risk',
                                data: data.highRisk,
                                borderColor: '#EF4444',
                                backgroundColor: 'rgba(239, 68, 68, 0.1)',
                                fill: true,
                                tension: 0.35,
                                pointBackgroundColor: '#EF4444',
                                pointRadius: 4
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        interaction: { mode: 'index', intersect: false },
                        plugins: {
                            legend: {
                                labels: { color: '#94A3B8', font: { family: 'Inter', size: 11 } }
                            }
                        },
                        scales: {
                            x: {
                                grid: { color: 'rgba(31, 42, 68, 0.5)' },
                                ticks: { color: '#64748B', font: { family: 'Inter', size: 10 } }
                            },
                            y: {
                                beginAtZero: true,
                                grid: { color: 'rgba(31, 42, 68, 0.5)' },
                                ticks: { color: '#64748B', font: { family: 'JetBrains Mono', size: 10 } }
                            }
                        }
                    }
                });
            })
            .catch(err => console.error('Trend chart fetch error:', err));
    }

    function initDonutChart(canvasId, stats) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || typeof Chart === 'undefined') return;

        const labels = Object.keys(stats || {});
        const dataValues = Object.values(stats || {});

        new Chart(canvas.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: dataValues,
                    backgroundColor: ['#22C55E', '#F59E0B', '#F97316', '#EF4444'],
                    borderColor: '#111A2E',
                    borderWidth: 2,
                    hoverOffset: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '72%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { color: '#94A3B8', font: { family: 'Inter', size: 11 } }
                    }
                }
            }
        });
    }

    function initCurveChart(canvasId, initialCurve) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || typeof Chart === 'undefined') return;

        const ctx = canvas.getContext('2d');
        if (curveChartInstance) curveChartInstance.destroy();

        curveChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels: initialCurve ? initialCurve.labels.map(l => l + ' mm') : [],
                datasets: [
                    {
                        label: 'Without Interventions (Baseline)',
                        data: initialCurve ? initialCurve.before : [],
                        borderColor: '#EF4444',
                        backgroundColor: 'rgba(239, 68, 68, 0.08)',
                        borderDash: [5, 5],
                        fill: false,
                        tension: 0.3
                    },
                    {
                        label: 'With Interventions (Broken Chain)',
                        data: initialCurve ? initialCurve.after : [],
                        borderColor: '#22D3EE',
                        backgroundColor: 'rgba(34, 211, 238, 0.15)',
                        fill: true,
                        tension: 0.3
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        labels: { color: '#94A3B8', font: { family: 'Inter', size: 11 } }
                    }
                },
                scales: {
                    x: {
                        title: { display: true, text: 'Rainfall Intensity (mm/hr)', color: '#64748B', font: { size: 11 } },
                        grid: { color: 'rgba(31, 42, 68, 0.5)' },
                        ticks: { color: '#64748B', font: { size: 10 } }
                    },
                    y: {
                        min: 0,
                        max: 100,
                        title: { display: true, text: 'Overall Risk Score', color: '#64748B', font: { size: 11 } },
                        grid: { color: 'rgba(31, 42, 68, 0.5)' },
                        ticks: { color: '#64748B', font: { size: 10 } }
                    }
                }
            }
        });
    }

    function updateCurveChart(curveData) {
        if (!curveChartInstance || !curveData) return;
        curveChartInstance.data.labels = curveData.labels.map(l => l + ' mm');
        curveChartInstance.data.datasets[0].data = curveData.before;
        curveChartInstance.data.datasets[1].data = curveData.after;
        curveChartInstance.update();
    }

    window.CivicShieldCharts = {
        initTrendChart: initTrendChart,
        initDonutChart: initDonutChart,
        initCurveChart: initCurveChart,
        updateCurveChart: updateCurveChart
    };
})();
