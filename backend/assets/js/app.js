(function (NC) {
  'use strict';

  const { qs, qsa, escapeHTML, debounce, getHashRoute, routeTo } = NC.utils;
  let currentView = null;
  let searchRequest = 0;
  let navigationCounter = 0;

  function setTheme(theme, persist = true) {
    const resolved = theme === 'light' ? 'light' : 'dark';
    document.documentElement.classList.toggle('dark', resolved === 'dark');
    document.documentElement.dataset.theme = resolved;
    if (persist) NC.utils.writePreference('theme', resolved);
    const toggle = qs('[data-theme-toggle]');
    if (toggle) {
      toggle.setAttribute('aria-label', `Switch to ${resolved === 'dark' ? 'light' : 'dark'} theme`);
      toggle.setAttribute('title', `Switch to ${resolved === 'dark' ? 'light' : 'dark'} theme`);
      toggle.innerHTML = `<i class="fa-regular ${resolved === 'dark' ? 'fa-sun-bright' : 'fa-moon-stars'}" aria-hidden="true"></i>`;
    }
    window.dispatchEvent(new CustomEvent('nc:theme-change', { detail: { theme: resolved } }));
  }

  function toggleTheme() {
    setTheme(document.documentElement.classList.contains('dark') ? 'light' : 'dark');
  }

  function setDensity(density, persist = true) {
    const value = density === 'compact' ? 'compact' : 'comfortable';
    document.documentElement.dataset.density = value;
    if (persist) NC.utils.writePreference('table-density', value);
  }

  function renderNavigation() {
    const nav = qs('#sidebar-navigation');
    const groups = [
      { id: 'overview', label: '', items: NC_CONFIG.routes.filter((item) => item.group === 'overview') },
      { id: 'content', label: 'Content', items: NC_CONFIG.routes.filter((item) => item.group === 'content') },
      { id: 'system', label: 'System', items: NC_CONFIG.routes.filter((item) => item.group === 'system') }
    ];
    nav.innerHTML = groups.map((group) => `
      <div class="nav-group">
        ${group.label ? `<p class="nav-group-label">${escapeHTML(group.label)}</p>` : ''}
        ${group.items.map((item) => `<a class="nav-link" href="#/${escapeHTML(item.id)}" data-nav-route="${escapeHTML(item.id)}"><i class="fa-regular ${escapeHTML(item.icon)}" aria-hidden="true"></i><span>${escapeHTML(item.label)}</span>${item.id === 'submissions' ? '<span class="nav-count hidden" data-submission-nav-count></span>' : ''}</a>`).join('')}
      </div>`).join('');
  }

  function setActiveNavigation(route) {
    qsa('[data-nav-route]').forEach((link) => {
      const active = link.dataset.navRoute === route;
      link.classList.toggle('is-active', active);
      if (active) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
    });
    const item = NC_CONFIG.routes.find((entry) => entry.id === route);
    document.title = `${item?.label || 'Dashboard'} — ${NC_CONFIG.app.name}`;
  }

  function showLogin(reason = '') {
    qs('#login-view').classList.remove('hidden');
    qs('#app-view').classList.add('hidden');
    document.body.classList.add('login-mode');
    qs('#toast-container')?.replaceChildren();
    currentView?.destroy?.(); currentView = null;
    if (reason === 'expired') {
      const message = qs('#login-message');
      message.textContent = 'Your session expired. Sign in again to continue.';
      message.classList.remove('hidden');
    }
    window.setTimeout(() => qs('#login-username')?.focus(), 80);
  }

  function showApp() {
    qs('#login-view').classList.add('hidden');
    qs('#app-view').classList.remove('hidden');
    document.body.classList.remove('login-mode');
    const session = NC.state.session;
    qsa('[data-current-user-name]').forEach((node) => { node.textContent = session?.user?.name || NC_CONFIG.auth.displayName; });
    qsa('[data-current-user-role]').forEach((node) => { node.textContent = session?.user?.role || NC_CONFIG.auth.role; });
    qsa('[data-current-user-initials]').forEach((node) => { node.textContent = NC.utils.initials(session?.user?.name || NC_CONFIG.auth.displayName); });
  }

  function closeSidebar() {
    qs('#sidebar')?.classList.remove('is-open');
    qs('#sidebar-overlay')?.classList.remove('is-visible');
    qsa('[data-sidebar-toggle]').forEach((button) => button.setAttribute('aria-expanded', 'false'));
  }

  function toggleSidebar() {
    const sidebar = qs('#sidebar');
    const open = !sidebar.classList.contains('is-open');
    sidebar.classList.toggle('is-open', open);
    qs('#sidebar-overlay').classList.toggle('is-visible', open);
    qsa('[data-sidebar-toggle]').forEach((button) => button.setAttribute('aria-expanded', String(open)));
  }

  function renderNotFound(container, route) {
    container.innerHTML = `${NC.components.pageHeader({ eyebrow: '404', title: 'View not found', description: `“${route}” is not a dashboard section.` })}<section class="surface">${NC.components.emptyState({ icon: 'fa-compass-slash', title: 'This route does not exist', description: 'Use the sidebar to return to a valid dashboard section.', action: '<a class="btn btn-primary" href="#/dashboard"><i class="fa-regular fa-gauge-high" aria-hidden="true"></i>Go to dashboard</a>' })}</section>`;
  }

  async function navigate() {
    if (!NC.auth.isAuthenticated()) { showLogin(); return; }
    const navigationId = ++navigationCounter;
    NC.state.navigationId = navigationId;
    showApp(); closeSidebar(); NC.components.closeModal('route-change'); closeGlobalSearch();
    const { route, params } = getHashRoute();
    const resolvedRoute = NC_CONFIG.routes.some((item) => item.id === route) ? route : route;
    setActiveNavigation(resolvedRoute);
    currentView?.destroy?.();
    const view = NC.views[resolvedRoute];
    const content = qs('#main-content');
    content.setAttribute('aria-busy', 'true');
    content.scrollTop = 0;
    try {
      if (!view) renderNotFound(content, resolvedRoute);
      else {
        currentView = view;
        await view.render(content, { route: resolvedRoute, params, navigationId });
      }
    } catch (error) {
      if (navigationId !== NC.state.navigationId) return;
      console.error(error);
      content.innerHTML = NC.components.errorState(error, { retry: false });
      NC.components.toast(NC.api.userMessage(error, 'This dashboard section could not be opened.'), 'error');
    } finally {
      if (navigationId === NC.state.navigationId) content.removeAttribute('aria-busy');
    }
  }

  async function handleLogin(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const username = form.elements.username.value;
    const password = form.elements.password.value;
    const remember = form.elements.remember.checked;
    const button = qs('[data-login-submit]', form);
    const error = qs('#login-error');
    error.classList.add('hidden'); error.textContent = '';
    NC.utils.setButtonLoading(button, true, 'Signing in…');
    try {
      await NC.auth.login(username, password, remember);
      form.reset();
      const landing = NC.utils.readPreference('landing-page', NC_CONFIG.app.defaultRoute);
      if (!window.location.hash || window.location.hash.includes('login')) routeTo(landing);
      else await navigate();
      NC.components.toast('Welcome back. Your editorial workspace is ready.', 'success');
      checkSchemaOnce(); updateSubmissionCount();
    } catch (loginError) {
      console.error(loginError);
      error.textContent = loginError.message || 'Unable to sign in.';
      error.classList.remove('hidden');
      form.elements.password.select();
    } finally { NC.utils.setButtonLoading(button, false); }
  }

  function handleLogout() {
    NC.components.confirm({ title: 'Log out of the dashboard?', description: 'Any unsaved form changes will be lost.', danger: false, confirmLabel: 'Log out', confirmIcon: 'fa-arrow-right-from-bracket' }).then((accepted) => {
      if (accepted) NC.auth.logout('manual');
    });
  }

  async function checkSchemaOnce() {
    if (!NC.auth.isAuthenticated() || NC.state.schemaChecked) return;
    NC.state.schemaChecked = true;
    try {
      const result = await NC.api.schemaProbe();
      const banner = qs('#schema-banner');
      if (!result.ok && result.missing.length) {
        banner.classList.remove('hidden');
        banner.querySelector('[data-schema-message]').textContent = `${result.missing.length} required database table${result.missing.length === 1 ? ' is' : 's are'} missing. Run the included Supabase schema before using CRUD features.`;
      } else banner.classList.add('hidden');
    } catch (error) { console.warn('Schema probe failed:', error); }
  }

  async function updateSubmissionCount() {
    if (!NC.auth.isAuthenticated()) return;
    try {
      const count = await NC.api.count('submissions', { status: 'Pending' });
      qsa('[data-submission-nav-count]').forEach((node) => { node.textContent = count > 99 ? '99+' : String(count); node.classList.toggle('hidden', !count); });
    } catch (_) { /* The schema banner explains unavailable tables. */ }
  }

  function openGlobalSearch() {
    if (!NC.auth.isAuthenticated()) return;
    const root = qs('#global-search');
    root.classList.remove('hidden');
    requestAnimationFrame(() => { root.classList.add('is-open'); qs('#global-search-input').focus(); });
  }

  function closeGlobalSearch() {
    const root = qs('#global-search');
    if (!root || root.classList.contains('hidden')) return;
    root.classList.remove('is-open');
    window.setTimeout(() => root.classList.add('hidden'), 140);
  }

  function searchResultTitle(result) {
    const item = result.item;
    if (result.table === 'comments') return item.name ? `Comment by ${item.name}` : 'Comment';
    if (result.table === 'submissions') return item.title || item.content_title || 'Submission';
    return item.title || item.name || 'Untitled';
  }

  function searchResultMeta(result) {
    const item = result.item;
    if (result.table === 'blogs') return `${item.status || 'Draft'} · ${item.author_name || 'Unassigned'}`;
    if (result.table === 'comments') return item.blog_title || NC.utils.truncate(item.content, 60);
    if (result.table === 'submissions') return `${item.status || 'Pending'} · ${item.writer_name || 'Unknown writer'}`;
    return item.designation || item.category || item.platform || item.slug || NC.utils.truncate(item.description, 60) || result.label;
  }

  async function performGlobalSearch(query) {
    const output = qs('#global-search-results');
    const currentRequest = ++searchRequest;
    const term = query.trim();
    if (term.length < 2) {
      output.innerHTML = '<div class="search-idle"><i class="fa-regular fa-command" aria-hidden="true"></i><p>Type at least two characters to search every content section.</p></div>';
      return;
    }
    output.innerHTML = '<div class="search-loading"><i class="fa-regular fa-spinner-third fa-spin" aria-hidden="true"></i><span>Searching Supabase…</span></div>';
    try {
      const results = await NC.api.searchAll(term);
      if (currentRequest !== searchRequest) return;
      if (!results.length) {
        output.innerHTML = NC.components.emptyState({ icon: 'fa-magnifying-glass', title: 'No results found', description: `Nothing matched “${term}”.` });
        return;
      }
      output.innerHTML = `<div class="search-results-list">${results.map((result) => `
        <button type="button" class="search-result" data-search-route="${escapeHTML(result.table)}" data-search-id="${escapeHTML(result.item.id)}">
          <span class="search-result-icon"><i class="fa-regular fa-${escapeHTML(result.icon)}" aria-hidden="true"></i></span>
          <span class="search-result-copy"><strong>${escapeHTML(searchResultTitle(result))}</strong><small>${escapeHTML(searchResultMeta(result))}</small></span>
          <span class="search-result-type">${escapeHTML(result.label)}</span><i class="fa-regular fa-arrow-right" aria-hidden="true"></i>
        </button>`).join('')}</div>`;
      qsa('[data-search-route]', output).forEach((button) => button.addEventListener('click', () => {
        const route = button.dataset.searchRoute;
        const action = route === 'categories' ? 'edit' : 'view';
        closeGlobalSearch(); routeTo(route, { action, id: button.dataset.searchId });
      }));
    } catch (error) {
      if (currentRequest === searchRequest) output.innerHTML = NC.components.errorState(error, { retry: false });
    }
  }

  function updateNetworkStatus() {
    const offline = !navigator.onLine;
    const banner = qs('#offline-banner');
    banner.classList.toggle('hidden', !offline);
    const indicator = qs('[data-network-indicator]');
    if (indicator) {
      indicator.classList.toggle('is-offline', offline);
      indicator.title = offline ? 'Offline' : 'Connected';
    }
  }

  function bindGlobalEvents() {
    qs('#login-form').addEventListener('submit', handleLogin);
    qs('[data-password-toggle]').addEventListener('click', (event) => {
      const input = qs('#login-password'); const visible = input.type === 'text'; input.type = visible ? 'password' : 'text';
      event.currentTarget.innerHTML = `<i class="fa-regular ${visible ? 'fa-eye' : 'fa-eye-slash'}" aria-hidden="true"></i>`;
      event.currentTarget.setAttribute('aria-label', visible ? 'Show password' : 'Hide password');
    });
    qsa('[data-logout]').forEach((button) => button.addEventListener('click', handleLogout));
    qs('[data-theme-toggle]').addEventListener('click', toggleTheme);
    qsa('[data-sidebar-toggle]').forEach((button) => button.addEventListener('click', toggleSidebar));
    qs('#sidebar-overlay').addEventListener('click', closeSidebar);
    qs('[data-search-open]').addEventListener('click', openGlobalSearch);
    qs('[data-search-close]').addEventListener('click', closeGlobalSearch);
    qs('#global-search-backdrop').addEventListener('click', closeGlobalSearch);
    qs('#global-search-input').addEventListener('input', debounce((event) => performGlobalSearch(event.target.value), 280));
    qs('[data-schema-dismiss]').addEventListener('click', () => qs('#schema-banner').classList.add('hidden'));
    qs('[data-user-menu-toggle]').addEventListener('click', (event) => {
      const menu = qs('#user-menu'); const open = menu.classList.toggle('is-open'); event.currentTarget.setAttribute('aria-expanded', String(open));
    });
    document.addEventListener('click', (event) => {
      if (!event.target.closest('.user-menu-wrap')) { qs('#user-menu').classList.remove('is-open'); qs('[data-user-menu-toggle]').setAttribute('aria-expanded', 'false'); }
    });
    document.addEventListener('keydown', (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); openGlobalSearch(); }
      if (event.key === 'Escape') closeGlobalSearch();
    });
    window.addEventListener('hashchange', navigate);
    window.addEventListener('online', updateNetworkStatus);
    window.addEventListener('offline', updateNetworkStatus);
    window.addEventListener('nc:auth-change', (event) => {
      if (!event.detail.authenticated) {
        showLogin(event.detail.reason);
        if (event.detail.reason === 'expired') NC.components.toast('Your session expired. Please sign in again.', 'warning');
      }
    });
    window.addEventListener('unhandledrejection', (event) => console.error('Unhandled promise rejection:', event.reason));
  }

  function init() {
    renderNavigation(); bindGlobalEvents();
    setTheme(NC.utils.readPreference('theme', NC_CONFIG.app.defaultTheme), false);
    setDensity(NC.utils.readPreference('table-density', 'comfortable'), false);
    updateNetworkStatus();
    NC.auth.restore();
    if (NC.auth.isAuthenticated()) {
      showApp();
      if (!window.location.hash) routeTo(NC.utils.readPreference('landing-page', NC_CONFIG.app.defaultRoute));
      else navigate();
      checkSchemaOnce(); updateSubmissionCount();
    } else showLogin();
  }

  NC.app = Object.freeze({ init, navigate, setTheme, setDensity, openGlobalSearch });
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, { once: true });
  else init();
})(window.NC);
