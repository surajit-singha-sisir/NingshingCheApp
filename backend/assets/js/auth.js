(function (NC) {
  'use strict';

  const SESSION_KEY = 'nc:admin-session';
  let expiryTimer = null;

  function getStoredSession() {
    for (const storage of [sessionStorage, localStorage]) {
      try {
        const raw = storage.getItem(SESSION_KEY);
        if (raw) return { session: JSON.parse(raw), storage };
      } catch (_) {
        // Continue with the other storage mechanism.
      }
    }
    return null;
  }

  function clearStoredSession() {
    for (const storage of [sessionStorage, localStorage]) {
      try { storage.removeItem(SESSION_KEY); } catch (_) { /* ignored */ }
    }
  }

  function scheduleExpiry(session) {
    window.clearTimeout(expiryTimer);
    const delay = Math.max(0, Math.min(session.expiresAt - Date.now(), 2147483647));
    expiryTimer = window.setTimeout(() => {
      if (Date.now() >= session.expiresAt) logout('expired');
      else scheduleExpiry(session);
    }, delay);
  }

  function setSession(session, remember = false) {
    clearStoredSession();
    const storage = remember ? localStorage : sessionStorage;
    storage.setItem(SESSION_KEY, JSON.stringify(session));
    NC.state.session = session;
    scheduleExpiry(session);
    window.dispatchEvent(new CustomEvent('nc:auth-change', { detail: { authenticated: true } }));
  }

  function restore() {
    const stored = getStoredSession();
    if (!stored?.session || stored.session.expiresAt <= Date.now()) {
      clearStoredSession();
      NC.state.session = null;
      return null;
    }
    const session = stored.session;
    if (!session.user || !session.dashboardToken || session.version !== 1) {
      clearStoredSession();
      NC.state.session = null;
      return null;
    }
    NC.state.session = session;
    scheduleExpiry(session);
    return session;
  }

  async function login(username, password, remember = false) {
    const normalizedUsername = String(username || '').trim().toLocaleLowerCase();
    const passwordDigest = await NC.utils.sha256(String(password || ''));
    const valid = normalizedUsername === NC_CONFIG.auth.username.toLocaleLowerCase()
      && passwordDigest === NC_CONFIG.auth.passwordHash;

    // Keep timing reasonably similar for invalid username and invalid password.
    if (!valid) {
      await new Promise((resolve) => window.setTimeout(resolve, 350));
      throw new Error('The username or password is incorrect.');
    }

    const now = Date.now();
    const duration = remember
      ? NC_CONFIG.app.rememberedSessionDays * 24 * 60 * 60 * 1000
      : NC_CONFIG.app.sessionHours * 60 * 60 * 1000;
    const session = {
      version: 1,
      id: NC.utils.uuid(),
      mode: 'demo-client',
      user: {
        username: normalizedUsername,
        name: NC_CONFIG.auth.displayName,
        role: NC_CONFIG.auth.role
      },
      // Used by the initial RLS helper. It is not a replacement for Supabase
      // Auth and is intentionally removed by the production RLS migration.
      dashboardToken: await NC.utils.sha256(`${normalizedUsername}:${password}`),
      issuedAt: now,
      expiresAt: now + duration
    };
    setSession(session, remember);
    return session;
  }

  function logout(reason = 'manual') {
    window.clearTimeout(expiryTimer);
    clearStoredSession();
    NC.state.session = null;
    window.dispatchEvent(new CustomEvent('nc:auth-change', {
      detail: { authenticated: false, reason }
    }));
  }

  function isAuthenticated() {
    const session = NC.state.session || restore();
    if (!session || session.expiresAt <= Date.now()) {
      if (session) logout('expired');
      return false;
    }
    return true;
  }

  function getDashboardToken() {
    return isAuthenticated() ? NC.state.session.dashboardToken : '';
  }

  function getAccessToken() {
    return isAuthenticated() ? (NC.state.session.accessToken || '') : '';
  }

  function remainingMilliseconds() {
    return isAuthenticated() ? Math.max(0, NC.state.session.expiresAt - Date.now()) : 0;
  }

  NC.auth = Object.freeze({
    restore, login, logout, isAuthenticated, getDashboardToken,
    getAccessToken, remainingMilliseconds
  });
})(window.NC);
