(function (NC) {
  'use strict';

  const { escapeHTML, formatDate, relativeTime, groupMonthly, number, routeTo } = NC.utils;
  const { pageHeader, statusBadge, emptyState, skeleton } = NC.components;
  let charts = [];
  let lastData = null;

  const definitions = Object.freeze([
    ['authors', 'Total Authors', 'fa-user-pen', 'violet'],
    ['blogs', 'Total Blogs', 'fa-newspaper', 'brand'],
    ['published', 'Published Blogs', 'fa-circle-check', 'emerald'],
    ['drafts', 'Draft Blogs', 'fa-pen-ruler', 'amber'],
    ['categories', 'Categories', 'fa-layer-group', 'sky'],
    ['comments', 'Comments', 'fa-comments', 'indigo'],
    ['pendingComments', 'Pending Comments', 'fa-message-exclamation', 'rose'],
    ['galleries', 'Galleries', 'fa-images', 'fuchsia'],
    ['books', 'PDF Books', 'fa-books', 'orange'],
    ['submissions', 'Submitted Blogs', 'fa-file-pen', 'cyan'],
    ['videos', 'Videos', 'fa-video', 'pink']
  ]);

  function destroyCharts() {
    charts.forEach((chart) => chart?.destroy?.());
    charts = [];
  }

  async function fetchDashboardData() {
    // Analytics never downloads article bodies or other heavy fields. A bounded
    // timeline sample powers charts while Content-Range provides exact totals.
    const entities = [
      ['authors', 'id,title,image,designation,created_at'],
      ['blogs', 'id,title,status,author_name,published_date,created_at'],
      ['categories', 'id,title,created_at'],
      ['comments', 'id,name,blog_title,status,created_at'],
      ['galleries', 'id,title,created_at'],
      ['books', 'id,title,created_at'],
      ['submissions', 'id,title,writer_name,status,created_at'],
      ['videos', 'id,title,created_at']
    ];
    const settled = await Promise.allSettled(entities.map(([key, select]) => NC.api.list(key, {
      select, order: 'created_at.desc', limit: 1000, count: true
    })));
    const data = { errors: [], counts: {} };
    settled.forEach((result, index) => {
      const key = entities[index][0];
      if (result.status === 'fulfilled') {
        data[key] = result.value.data;
        data.counts[key] = result.value.count;
      } else {
        data[key] = [];
        data.counts[key] = 0;
        data.errors.push({ key, error: result.reason });
      }
    });

    const countRequests = await Promise.allSettled([
      NC.api.count('blogs', { status: 'Publish' }),
      NC.api.count('blogs', { status: 'Draft' }),
      NC.api.count('comments', { status: 'Unpublish' }),
      NC.api.count('submissions', { status: 'Pending' })
    ]);
    data.counts.published = countRequests[0].status === 'fulfilled' ? countRequests[0].value : data.blogs.filter((item) => item.status === 'Publish').length;
    data.counts.drafts = countRequests[1].status === 'fulfilled' ? countRequests[1].value : data.blogs.filter((item) => item.status === 'Draft').length;
    data.counts.pendingComments = countRequests[2].status === 'fulfilled' ? countRequests[2].value : data.comments.filter((item) => item.status === 'Unpublish').length;
    data.counts.pendingSubmissions = countRequests[3].status === 'fulfilled' ? countRequests[3].value : data.submissions.filter((item) => item.status === 'Pending').length;
    return data;
  }

  function metricCard([key, label, icon, tone], counts) {
    return `
      <article class="metric-card metric-${tone}">
        <span class="metric-icon"><i class="fa-duotone fa-solid ${icon}" aria-hidden="true"></i></span>
        <div class="min-w-0"><p class="metric-label">${escapeHTML(label)}</p><p class="metric-value">${number(counts[key] || 0)}</p></div>
        <span class="metric-detail"><i class="fa-regular fa-arrow-trend-up" aria-hidden="true"></i>Live data</span>
      </article>`;
  }

  function quickActions() {
    const actions = [
      ['blogs', 'new', 'Add Blog', 'fa-plus', 'primary'],
      ['authors', 'new', 'Add Author', 'fa-user-plus', ''],
      ['categories', 'new', 'Add Category', 'fa-layer-plus', ''],
      ['galleries', 'new', 'Add Gallery', 'fa-image-polaroid', ''],
      ['books', 'new', 'Add Book', 'fa-book-circle-plus', ''],
      ['submissions', '', 'Review Submissions', 'fa-file-magnifying-glass', ''],
      ['videos', 'new', 'Add Video', 'fa-video-plus', '']
    ];
    return `<div class="quick-actions">${actions.map(([route, action, label, icon, tone]) => `
      <button type="button" class="quick-action ${tone === 'primary' ? 'quick-action-primary' : ''}" data-quick-route="${route}" data-quick-action="${action}">
        <i class="fa-regular ${icon}" aria-hidden="true"></i><span>${escapeHTML(label)}</span><i class="fa-regular fa-arrow-right quick-arrow" aria-hidden="true"></i>
      </button>`).join('')}</div>`;
  }

  function buildActivities(data) {
    const map = [
      ['blogs', 'Blog', 'newspaper', (item) => item.title, (item) => item.status],
      ['comments', 'Comment', 'comments', (item) => item.blog_title || item.name, (item) => item.status],
      ['submissions', 'Submission', 'file-pen', (item) => item.title, (item) => item.status],
      ['books', 'PDF Book', 'books', (item) => item.title, () => 'Added'],
      ['videos', 'Video', 'video', (item) => item.title, () => 'Added']
    ];
    return map.flatMap(([key, type, icon, title, status]) => (data[key] || []).slice(0, 8).map((item) => ({
      id: item.id, route: key, type, icon, title: title(item) || 'Untitled', status: status(item), created_at: item.created_at
    }))).sort((a, b) => (new Date(b.created_at).getTime() || 0) - (new Date(a.created_at).getTime() || 0)).slice(0, 10);
  }

  function recentActivity(data) {
    const activities = buildActivities(data);
    if (!activities.length) return emptyState({ icon: 'fa-wave-pulse', title: 'No recent activity', description: 'New and updated content will appear here.' });
    return `<div class="activity-list">${activities.map((item) => `
      <button type="button" class="activity-item" data-activity-route="${escapeHTML(item.route)}" data-activity-id="${escapeHTML(item.id)}">
        <span class="activity-icon"><i class="fa-regular fa-${escapeHTML(item.icon)}" aria-hidden="true"></i></span>
        <span class="activity-copy"><strong>${escapeHTML(item.title)}</strong><small>${escapeHTML(item.type)} · ${escapeHTML(relativeTime(item.created_at))}</small></span>
        ${statusBadge(item.status)}
      </button>`).join('')}</div>`;
  }

  function attentionPanel(data) {
    const items = [
      { count: data.counts.drafts, label: 'draft blogs awaiting publication', route: 'blogs', filter: 'Draft', icon: 'fa-pen-ruler', tone: 'amber' },
      { count: data.counts.pendingComments, label: 'comments awaiting moderation', route: 'comments', filter: 'Unpublish', icon: 'fa-message-exclamation', tone: 'rose' },
      { count: data.counts.pendingSubmissions, label: 'new submissions to review', route: 'submissions', filter: 'Pending', icon: 'fa-file-magnifying-glass', tone: 'sky' }
    ];
    if (!items.some((item) => item.count)) {
      return `<div class="all-clear"><span><i class="fa-solid fa-circle-check" aria-hidden="true"></i></span><div><strong>Editorial queue is clear</strong><p>There is no pending content that needs attention.</p></div></div>`;
    }
    return `<div class="attention-list">${items.map((item) => `
      <button type="button" class="attention-item attention-${item.tone}" data-attention-route="${item.route}" data-attention-filter="${item.filter}">
        <span><i class="fa-regular ${item.icon}" aria-hidden="true"></i></span>
        <strong>${number(item.count)}</strong><p>${escapeHTML(item.label)}</p><i class="fa-regular fa-chevron-right" aria-hidden="true"></i>
      </button>`).join('')}</div>`;
  }

  function latestCards(data) {
    const latestBlog = data.blogs.find((item) => item.status === 'Publish');
    const latestSubmission = data.submissions[0];
    const latestAuthor = data.authors[0];
    const cards = [
      { label: 'Latest published article', item: latestBlog, title: latestBlog?.title, meta: latestBlog ? formatDate(latestBlog.published_date || latestBlog.created_at) : '', icon: 'fa-newspaper', route: 'blogs' },
      { label: 'Most recent submission', item: latestSubmission, title: latestSubmission?.title, meta: latestSubmission ? `${latestSubmission.writer_name || 'Unknown writer'} · ${relativeTime(latestSubmission.created_at)}` : '', icon: 'fa-file-pen', route: 'submissions' },
      { label: 'Recently added author', item: latestAuthor, title: latestAuthor?.title, meta: latestAuthor?.designation || (latestAuthor ? formatDate(latestAuthor.created_at) : ''), icon: 'fa-user-pen', route: 'authors' }
    ];
    return `<div class="latest-grid">${cards.map((card) => `
      <article class="latest-card">
        <div class="latest-label"><i class="fa-regular ${card.icon}" aria-hidden="true"></i>${escapeHTML(card.label)}</div>
        ${card.item ? `<h3>${escapeHTML(card.title || 'Untitled')}</h3><p>${escapeHTML(card.meta)}</p><button type="button" data-latest-route="${card.route}" data-latest-id="${escapeHTML(card.item.id)}">Open record <i class="fa-regular fa-arrow-right" aria-hidden="true"></i></button>` : `<p class="text-muted-foreground mt-4">No record yet.</p>`}
      </article>`).join('')}</div>`;
  }

  function chartCard(title, description, canvasId, className = '') {
    return `<article class="surface chart-card ${className}"><div class="surface-header"><div><h2>${escapeHTML(title)}</h2><p>${escapeHTML(description)}</p></div></div><div class="chart-wrap"><canvas id="${escapeHTML(canvasId)}" role="img" aria-label="${escapeHTML(title)} chart"></canvas></div></article>`;
  }

  function renderCharts(data) {
    destroyCharts();
    if (!window.Chart) {
      document.querySelectorAll('.chart-wrap').forEach((node) => { node.innerHTML = NC.components.notice('Chart.js did not load. Check your connection and refresh.', 'warning'); });
      return;
    }
    const css = getComputedStyle(document.documentElement);
    const text = css.getPropertyValue('--muted-foreground').trim() || '#94a3b8';
    const grid = css.getPropertyValue('--border').trim() || 'rgba(148,163,184,.15)';
    const brand = '#8b5cf6';
    const months = groupMonthly(data.blogs).map((item) => item.label);
    const common = {
      responsive: true, maintainAspectRatio: false,
      animation: { duration: 500 },
      plugins: { legend: { labels: { color: text, usePointStyle: true, boxWidth: 8, padding: 18 } } },
      scales: {
        x: { ticks: { color: text }, grid: { display: false }, border: { display: false } },
        y: { beginAtZero: true, ticks: { color: text, precision: 0 }, grid: { color: grid }, border: { display: false } }
      }
    };
    const growth = document.getElementById('chart-growth');
    if (growth) charts.push(new Chart(growth, {
      type: 'line',
      data: {
        labels: months,
        datasets: [
          ['Blogs', data.blogs, '#8b5cf6'], ['Authors', data.authors, '#22c55e'],
          ['Comments', data.comments, '#38bdf8'], ['Submissions', data.submissions, '#f59e0b']
        ].map(([label, records, color]) => ({ label, data: groupMonthly(records).map((item) => item.count), borderColor: color, backgroundColor: `${color}22`, tension: .35, fill: false, pointRadius: 3, pointHoverRadius: 5 }))
      }, options: common
    }));
    const status = document.getElementById('chart-status');
    if (status) charts.push(new Chart(status, {
      type: 'doughnut',
      data: { labels: ['Published', 'Draft'], datasets: [{ data: [data.counts.published, data.counts.drafts], backgroundColor: ['#22c55e', '#f59e0b'], borderWidth: 0, hoverOffset: 4 }] },
      options: { responsive: true, maintainAspectRatio: false, cutout: '72%', plugins: { legend: common.plugins.legend } }
    }));
    const distribution = document.getElementById('chart-distribution');
    if (distribution) charts.push(new Chart(distribution, {
      type: 'bar',
      data: {
        labels: ['Blogs', 'Authors', 'Categories', 'Galleries', 'Books', 'Videos', 'Comments'],
        datasets: [{ label: 'Content', data: ['blogs', 'authors', 'categories', 'galleries', 'books', 'videos', 'comments'].map((key) => data.counts[key]), backgroundColor: [brand, '#22c55e', '#38bdf8', '#d946ef', '#f97316', '#ec4899', '#6366f1'], borderRadius: 8, borderSkipped: false }]
      }, options: { ...common, plugins: { legend: { display: false } } }
    }));
  }

  function renderContent(container, data, analyticsOnly = false) {
    const schemaError = data.errors.find((item) => item.error?.isSchemaMissing)?.error;
    const errorBanner = data.errors.length ? `<div class="mb-6">${NC.components.notice(schemaError ? 'Database setup is required. Run backend/supabase/schema.sql in the Supabase SQL Editor; this page will then load real data.' : `${data.errors.length} data source${data.errors.length === 1 ? '' : 's'} could not be loaded. Available metrics are still shown.`, 'warning', 'fa-database')}</div>` : '';
    container.innerHTML = `
      ${pageHeader({
        eyebrow: analyticsOnly ? 'Insights' : 'Editorial overview',
        title: analyticsOnly ? 'Analytics' : 'Good to see you, Editor',
        description: analyticsOnly ? 'Real-time content trends calculated from Supabase records.' : 'Here is what is happening across Ningshing Che today.',
        breadcrumb: analyticsOnly ? [{ label: 'Analytics' }] : [],
        actions: `<button type="button" class="btn btn-secondary" data-refresh-dashboard><i class="fa-regular fa-arrows-rotate" aria-hidden="true"></i>Refresh</button>${analyticsOnly ? '' : '<button type="button" class="btn btn-primary" data-quick-route="blogs" data-quick-action="new"><i class="fa-regular fa-plus" aria-hidden="true"></i>New blog</button>'}`
      })}
      ${errorBanner}
      <section class="metrics-grid" aria-label="Content metrics">${definitions.map((item) => metricCard(item, data.counts)).join('')}</section>
      <section class="dashboard-chart-grid mt-6">
        ${chartCard('Content growth', 'Blogs, authors, comments, and submissions over six months.', 'chart-growth', 'chart-wide')}
        ${chartCard('Blog status', 'Published content compared with drafts.', 'chart-status')}
        ${chartCard('Content distribution', 'Current records by editorial content type.', 'chart-distribution', 'chart-full')}
      </section>
      ${analyticsOnly ? '' : `
        <section class="dashboard-columns mt-6">
          <article class="surface"><div class="surface-header"><div><p class="eyebrow">Get things done</p><h2>Quick actions</h2></div></div>${quickActions()}</article>
          <article class="surface"><div class="surface-header"><div><p class="eyebrow">Editorial queue</p><h2>Needs attention</h2></div></div>${attentionPanel(data)}</article>
        </section>
        <section class="surface mt-6"><div class="surface-header"><div><p class="eyebrow">At a glance</p><h2>Latest content</h2></div></div>${latestCards(data)}</section>
        <section class="surface mt-6"><div class="surface-header"><div><p class="eyebrow">Live feed</p><h2>Recent activity</h2></div><button type="button" class="btn btn-ghost btn-sm" data-refresh-dashboard><i class="fa-regular fa-rotate" aria-hidden="true"></i>Refresh</button></div>${recentActivity(data)}</section>`}
    `;
    bindEvents(container, analyticsOnly);
    renderCharts(data);
  }

  function bindEvents(container, analyticsOnly) {
    container.querySelectorAll('[data-refresh-dashboard]').forEach((button) => button.addEventListener('click', () => render(container, analyticsOnly)));
    container.querySelectorAll('[data-quick-route]').forEach((button) => button.addEventListener('click', () => routeTo(button.dataset.quickRoute, { action: button.dataset.quickAction })));
    container.querySelectorAll('[data-activity-route], [data-latest-route]').forEach((button) => button.addEventListener('click', () => {
      const route = button.dataset.activityRoute || button.dataset.latestRoute;
      const id = button.dataset.activityId || button.dataset.latestId;
      routeTo(route, { action: 'view', id });
    }));
    container.querySelectorAll('[data-attention-route]').forEach((button) => button.addEventListener('click', () => routeTo(button.dataset.attentionRoute, { filter: button.dataset.attentionFilter })));
  }

  async function render(container, analyticsOnly = false, context = {}) {
    destroyCharts();
    container.innerHTML = `${pageHeader({ eyebrow: 'Loading', title: analyticsOnly ? 'Analytics' : 'Dashboard', description: 'Retrieving live editorial data from Supabase…' })}<div class="metrics-grid">${Array.from({ length: 8 }, () => '<div class="metric-card"><span class="skeleton-line h-14 w-full"></span></div>').join('')}</div><div class="mt-6">${skeleton(5, 4)}</div>`;
    try {
      const data = await fetchDashboardData();
      if (NC.crud.isStaleNavigation(context)) return;
      lastData = data;
      renderContent(container, data, analyticsOnly);
    } catch (error) {
      NC.crud.handleLoadError(container, error, () => render(container, analyticsOnly, context), context);
    }
  }

  window.addEventListener('nc:theme-change', () => {
    if (lastData && ['dashboard', 'analytics'].includes(NC.utils.getHashRoute().route)) window.setTimeout(() => renderCharts(lastData), 50);
  });

  NC.views.dashboard = { render: (container, context) => render(container, false, context), destroy: destroyCharts };
  NC.views.analytics = { render: (container, context) => render(container, true, context), destroy: destroyCharts };
})(window.NC);
