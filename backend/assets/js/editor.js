(function (NC) {
  'use strict';

  const { escapeHTML, qs, qsa, sanitizeHTML, stripHTML, uuid } = NC.utils;

  function editorHTML({ id = uuid(), label = 'Content', hint = 'Use the toolbar to structure the article.', required = false } = {}) {
    return `
      <div class="rich-editor-field" data-rich-editor id="${escapeHTML(id)}">
        <div class="field-heading">
          <label class="field-label" id="${escapeHTML(id)}-label">${escapeHTML(label)}${required ? '<span aria-hidden="true"> *</span>' : ''}</label>
          <span class="field-hint">${escapeHTML(hint)}</span>
        </div>
        <div class="editor-toolbar-extra">
          <button type="button" class="editor-extra-button" data-insert-table title="Insert a 2 × 2 table"><i class="fa-regular fa-table" aria-hidden="true"></i><span>Table</span></button>
          <button type="button" class="editor-extra-button" data-source-toggle title="Edit sanitized HTML source"><i class="fa-regular fa-code" aria-hidden="true"></i><span>HTML</span></button>
        </div>
        <div class="editor-host-wrap"><div class="editor-host" data-editor-host aria-labelledby="${escapeHTML(id)}-label"></div></div>
        <textarea class="editor-source hidden" data-editor-source spellcheck="false" aria-label="HTML source"></textarea>
        <div class="editor-footer"><span data-editor-count>0 words</span><span>HTML is sanitized before preview and save</span></div>
        <p class="field-error hidden" data-editor-error role="alert"></p>
      </div>`;
  }

  function requestUrl({ title, label, placeholder }) {
    return new Promise((resolve) => {
      let settled = false;
      const modal = NC.components.openModal({
        title,
        size: 'sm',
        content: `<label class="field-label" for="editor-url-input">${escapeHTML(label)}</label><input id="editor-url-input" class="form-input mt-2" type="url" placeholder="${escapeHTML(placeholder)}" autofocus><p class="field-error hidden mt-2" id="editor-url-error"></p>`,
        footer: '<button type="button" class="btn btn-secondary" data-url-cancel>Cancel</button><button type="button" class="btn btn-primary" data-url-apply>Insert</button>',
        onClose: () => { if (!settled) resolve(''); }
      });
      const input = qs('#editor-url-input', modal.element);
      const finish = (value) => { if (settled) return; settled = true; resolve(value); NC.components.closeModal(); };
      qs('[data-url-cancel]', modal.element).addEventListener('click', () => finish(''));
      qs('[data-url-apply]', modal.element).addEventListener('click', () => {
        const value = input.value.trim();
        if (!NC.utils.isValidUrl(value, { allowEmpty: false })) {
          const error = qs('#editor-url-error', modal.element);
          error.textContent = 'Enter a complete http:// or https:// URL.';
          error.classList.remove('hidden');
          return;
        }
        finish(value);
      });
      input.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') { event.preventDefault(); qs('[data-url-apply]', modal.element).click(); }
      });
    });
  }

  function mountEditor(root, options = {}) {
    const element = root?.matches?.('[data-rich-editor]') ? root : qs('[data-rich-editor]', root);
    if (!element) throw new Error('Rich text editor element was not found.');
    const host = qs('[data-editor-host]', element);
    const source = qs('[data-editor-source]', element);
    const count = qs('[data-editor-count]', element);
    const errorNode = qs('[data-editor-error]', element);
    const sourceButton = qs('[data-source-toggle]', element);
    let sourceMode = false;
    let quill = null;

    function setError(message = '') {
      errorNode.textContent = message;
      errorNode.classList.toggle('hidden', !message);
      element.classList.toggle('has-error', Boolean(message));
    }

    function updateCount(html) {
      const text = stripHTML(html || '');
      const words = text ? text.split(/\s+/).length : 0;
      count.textContent = `${words.toLocaleString()} word${words === 1 ? '' : 's'}`;
    }

    function rawValue() {
      if (sourceMode) return source.value;
      if (quill) return quill.root.innerHTML === '<p><br></p>' ? '' : quill.root.innerHTML;
      return host.innerHTML;
    }

    function getValue() {
      return sanitizeHTML(rawValue());
    }

    function setValue(value) {
      const clean = sanitizeHTML(value || '');
      if (quill) quill.clipboard.dangerouslyPasteHTML(clean);
      else host.innerHTML = clean;
      source.value = clean;
      updateCount(clean);
    }

    function toggleSource() {
      if (!sourceMode) {
        source.value = getValue();
        source.classList.remove('hidden');
        host.parentElement?.classList.add('hidden');
        sourceMode = true;
        sourceButton.classList.add('is-active');
        sourceButton.setAttribute('aria-pressed', 'true');
        source.focus();
      } else {
        const clean = sanitizeHTML(source.value);
        if (quill) quill.clipboard.dangerouslyPasteHTML(clean);
        else host.innerHTML = clean;
        source.classList.add('hidden');
        host.parentElement?.classList.remove('hidden');
        sourceMode = false;
        sourceButton.classList.remove('is-active');
        sourceButton.setAttribute('aria-pressed', 'false');
        updateCount(clean);
        quill?.focus();
      }
    }

    function insertTable() {
      const markup = '<table><tbody><tr><th>Heading</th><th>Heading</th></tr><tr><td>Cell</td><td>Cell</td></tr></tbody></table><p><br></p>';
      if (sourceMode) {
        const start = source.selectionStart;
        source.setRangeText(markup, start, source.selectionEnd, 'end');
        source.dispatchEvent(new Event('input'));
      } else if (quill) {
        const range = quill.getSelection(true) || { index: quill.getLength() - 1 };
        quill.clipboard.dangerouslyPasteHTML(range.index, markup, 'user');
      } else {
        host.focus(); document.execCommand('insertHTML', false, markup);
      }
    }

    if (window.Quill) {
      quill = new window.Quill(host, {
        theme: 'snow',
        placeholder: options.placeholder || 'Start writing…',
        modules: {
          toolbar: {
            container: [
              [{ header: [2, 3, 4, false] }],
              ['bold', 'italic', 'underline'],
              [{ list: 'ordered' }, { list: 'bullet' }],
              ['blockquote', 'link'],
              [{ align: [] }],
              ['image', 'clean']
            ],
            handlers: {
              link: async function linkHandler(value) {
                if (!value) { this.quill.format('link', false); return; }
                const url = await requestUrl({ title: 'Insert link', label: 'Destination URL', placeholder: 'https://example.com' });
                if (url) this.quill.format('link', url);
              },
              image: async function imageHandler() {
                const url = await requestUrl({ title: 'Insert image', label: 'Public image URL', placeholder: 'https://example.com/image.jpg' });
                if (!url) return;
                const range = this.quill.getSelection(true) || { index: this.quill.getLength() - 1 };
                this.quill.insertEmbed(range.index, 'image', url, 'user');
                this.quill.setSelection(range.index + 1, 0, 'silent');
              }
            }
          }
        }
      });
      quill.on('text-change', () => { setError(''); updateCount(rawValue()); options.onChange?.(getValue()); });
    } else {
      host.contentEditable = 'true';
      host.classList.add('editor-fallback');
      host.setAttribute('role', 'textbox');
      host.setAttribute('aria-multiline', 'true');
      host.dataset.placeholder = options.placeholder || 'Start writing…';
      const toolbar = document.createElement('div');
      toolbar.className = 'fallback-toolbar';
      toolbar.innerHTML = [
        ['bold', 'fa-bold', 'Bold'], ['italic', 'fa-italic', 'Italic'],
        ['underline', 'fa-underline', 'Underline'], ['insertUnorderedList', 'fa-list-ul', 'Bullet list'],
        ['formatBlock', 'fa-quote-left', 'Blockquote']
      ].map(([command, icon, label]) => `<button type="button" data-command="${command}" aria-label="${label}" title="${label}"><i class="fa-regular ${icon}" aria-hidden="true"></i></button>`).join('');
      host.before(toolbar);
      qsa('[data-command]', toolbar).forEach((button) => button.addEventListener('click', () => {
        host.focus();
        document.execCommand(button.dataset.command, false, button.dataset.command === 'formatBlock' ? 'blockquote' : null);
      }));
      host.addEventListener('input', () => { setError(''); updateCount(rawValue()); options.onChange?.(getValue()); });
    }

    sourceButton.setAttribute('aria-pressed', 'false');
    sourceButton.addEventListener('click', toggleSource);
    qs('[data-insert-table]', element).addEventListener('click', insertTable);
    source.addEventListener('input', () => { setError(''); updateCount(source.value); options.onChange?.(sanitizeHTML(source.value)); });
    setValue(options.initial || '');

    return Object.freeze({
      getValue,
      setValue,
      focus: () => sourceMode ? source.focus() : (quill ? quill.focus() : host.focus()),
      validate: () => {
        const empty = !stripHTML(getValue());
        if (options.required && empty) { setError(`${options.label || 'Content'} is required.`); return false; }
        setError(''); return true;
      },
      destroy: () => { quill = null; }
    });
  }

  NC.editor = Object.freeze({ editorHTML, mountEditor });
})(window.NC);
