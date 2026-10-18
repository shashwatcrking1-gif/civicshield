/**
 * CivicShield – Core Client Script
 * Handles theme toggle, sticky navbar, mobile menus, and Lucide icons initialization.
 */

(function () {
    'use strict';

    // 1. Theme Management (dark default, persists in localStorage)
    const THEME_KEY = 'civicshield-theme';
    const htmlEl = document.documentElement;

    function applyTheme(theme) {
        if (theme === 'light') {
            htmlEl.classList.remove('dark');
            htmlEl.classList.add('light');
        } else {
            htmlEl.classList.remove('light');
            htmlEl.classList.add('dark');
        }
        localStorage.setItem(THEME_KEY, theme);
        updateThemeToggleIcons(theme);
    }

    function toggleTheme() {
        const current = htmlEl.classList.contains('light') ? 'light' : 'dark';
        applyTheme(current === 'light' ? 'dark' : 'light');
    }

    function updateThemeToggleIcons(theme) {
        document.querySelectorAll('.theme-icon-sun').forEach(el => {
            el.style.display = (theme === 'light') ? 'inline-block' : 'none';
        });
        document.querySelectorAll('.theme-icon-moon').forEach(el => {
            el.style.display = (theme === 'light') ? 'none' : 'inline-block';
        });
    }

    // Initialize theme
    const savedTheme = localStorage.getItem(THEME_KEY) || 'dark';
    applyTheme(savedTheme);

    // 2. DOM Ready Listeners
    document.addEventListener('DOMContentLoaded', function () {
        // Wire all theme toggle buttons
        document.querySelectorAll('[data-action="toggle-theme"]').forEach(btn => {
            btn.addEventListener('click', toggleTheme);
        });
        updateThemeToggleIcons(savedTheme);

        // Sticky Navbar scroll effect
        const navbar = document.getElementById('main-navbar');
        if (navbar) {
            window.addEventListener('scroll', function () {
                if (window.scrollY > 30) {
                    navbar.classList.add('navbar-scrolled');
                } else {
                    navbar.classList.remove('navbar-scrolled');
                }
            }, { passive: true });
        }

        // Mobile Menu Drawer
        const mobileMenuBtn = document.getElementById('mobile-menu-btn');
        const mobileMenu = document.getElementById('mobile-menu');
        if (mobileMenuBtn && mobileMenu) {
            mobileMenuBtn.addEventListener('click', function () {
                mobileMenu.classList.toggle('hidden');
            });
        }

        // Portal Sidebar Mobile Toggle
        const sidebarToggle = document.getElementById('sidebar-toggle');
        const portalSidebar = document.getElementById('portal-sidebar');
        if (sidebarToggle && portalSidebar) {
            sidebarToggle.addEventListener('click', function () {
                portalSidebar.classList.toggle('-translate-x-full');
            });
        }

        // Initialize Lucide icons if available
        if (window.lucide) {
            window.lucide.createIcons();
        }

        // Animated number counters on scroll
        initCounters();
    });

    // 3. Simple Number Counter Animation
    function initCounters() {
        const counters = document.querySelectorAll('[data-counter]');
        if (!counters.length) return;

        const observer = new IntersectionObserver((entries, obs) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const target = entry.target;
                    const finalVal = parseFloat(target.getAttribute('data-counter'));
                    const isPercent = target.innerText.includes('%') || target.getAttribute('data-counter-suffix') === '%';
                    const prefix = target.getAttribute('data-counter-prefix') || '';
                    const suffix = target.getAttribute('data-counter-suffix') || (isPercent ? '%' : '');
                    
                    let start = 0;
                    const duration = 1200;
                    const startTime = performance.now();

                    function update(now) {
                        const progress = Math.min((now - startTime) / duration, 1);
                        const easeOut = 1 - Math.pow(1 - progress, 3);
                        const current = Math.floor(start + (finalVal - start) * easeOut);
                        target.innerText = prefix + current + suffix;
                        if (progress < 1) {
                            requestAnimationFrame(update);
                        } else {
                            target.innerText = prefix + finalVal + suffix;
                        }
                    }
                    requestAnimationFrame(update);
                    obs.unobserve(target);
                }
            });
        }, { threshold: 0.2 });

        counters.forEach(c => observer.observe(c));
    }

    // Expose helpers globally
    window.CivicShield = {
        toggleTheme: toggleTheme,
        applyTheme: applyTheme
    };
})();
