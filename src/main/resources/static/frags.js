/**
 * frags.js – Shared fragment loader for HRM System
 *
 * Each page only needs:
 *   <header class="topbar" id="topbar-placeholder"></header>
 *   <aside  class="sidebar" id="navbar-placeholder"></aside>
 *   <script src="/frags.js"></script>
 *
 * Provides global HRMFrags object for auth utilities.
 */
(function () {
  'use strict';

  /* ── Auth helpers ──────────────────────────────────────────────────────── */
  const TOKEN_KEY   = 'hrm_access_token';
  const USER_KEY    = 'hrm_user';
  const REFRESH_KEY = 'hrm_refresh_token';

  const HRMFrags = {
    getToken() { return localStorage.getItem(TOKEN_KEY); },

    getUser() {
      try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); }
      catch (_) { return null; }
    },

    async logout() {
      try {
        await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
      } catch (_) { /* ignore */ }
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      localStorage.removeItem(REFRESH_KEY);
      window.location.href = '/login';
    },

    redirectLogin() {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      localStorage.removeItem(REFRESH_KEY);
      window.location.href = '/login';
    }
  };

  /* expose globally so inline onclick="HRMFrags.logout()" works */
  window.HRMFrags = HRMFrags;

  /* also keep legacy logout() alias used by older pages */
  if (typeof window.logout !== 'function') {
    window.logout = HRMFrags.logout;
  }

  /* ── Active-page detection ─────────────────────────────────────────────── */
  function resolveCurrentNav() {
    const path = window.location.pathname;
    const hash = window.location.hash;
    if (path === '/overview' || path === '/overview.html' ||
        path === '/dashboard' || path === '/dashboard.html') return 'overview';
    if (path.includes('employees'))   return 'employees';
    if (path.includes('departments')) return 'departments';
    if (path.includes('worktime') || path.includes('attendance')) {
      return hash === '#leave' ? 'leave' : 'attendance';
    }
    return '';
  }

  /* ── Fragment injection ─────────────────────────────────────────────────── */
  async function fetchFragment(url) {
    const res = await fetch(url);
    if (!res.ok) throw new Error('Fragment load failed: ' + url);
    return res.text();
  }

  async function injectTopbar(user, role) {
    const el = document.getElementById('topbar-placeholder');
    if (!el) return;
    try {
      el.innerHTML = await fetchFragment('/fragments/topbar.html');
    } catch (_) { return; }

    /* Highlight active topbar link */
    const path = window.location.pathname;
    el.querySelectorAll('.topbar-nav a').forEach(a => {
      const h = a.getAttribute('href');
      if (h === '/overview' && (path === '/overview' || path === '/overview.html')) {
        a.classList.add('active');
      }
      if (h === '/overview' && (path === '/dashboard' || path === '/dashboard.html')) {
        a.classList.add('active');
      }
    });

    /* Fill user info */
    const username = user ? (user.username || '–') : '–';
    const usernameEl = el.querySelector('#topbar-username');
    const avatarEl   = el.querySelector('#topbar-avatar');
    if (usernameEl) usernameEl.textContent = username + (role ? ' (' + role + ')' : '');
    if (avatarEl)   avatarEl.textContent   = username.charAt(0).toUpperCase();
  }

  async function injectNavbar(user, role) {
    const el = document.getElementById('navbar-placeholder');
    if (!el) return;
    try {
      el.innerHTML = await fetchFragment('/fragments/navbar.html');
    } catch (_) { return; }

    const currentNav = resolveCurrentNav();

    el.querySelectorAll('[data-nav]').forEach(link => {
      /* active highlight */
      if (link.dataset.nav === currentNav) link.classList.add('active');

      /* role-based visibility */
      const allowed = link.dataset.roles;
      if (allowed && role && !allowed.split(',').includes(role)) {
        link.style.display = 'none';
      }
    });
  }

  /* ── Boot ───────────────────────────────────────────────────────────────── */
  document.addEventListener('DOMContentLoaded', async function () {
    const user = HRMFrags.getUser();
    const role = user ? String(user.role || '').trim().toUpperCase() : '';

    await Promise.all([
      injectTopbar(user, role),
      injectNavbar(user, role)
    ]);

    /* Dispatch event so pages can hook in after fragments are ready */
    document.dispatchEvent(new CustomEvent('hrm:frags-ready', { detail: { user, role } }));
  });
}());
