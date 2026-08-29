# Ningshing Che — Editorial Dashboard

A responsive, no-build administration dashboard for **Ningshing Che — Bishnupriya Manipuri Magazine**. It is built with semantic HTML, Tailwind CSS via CDN, modular Vanilla JavaScript, Chart.js, Quill, DOMPurify, Font Awesome 6 Pro, Supabase REST, ImgBB, and Supabase Storage.

The dashboard is located entirely inside `backend/`, as requested.

## Highlights

- Dark-first editorial interface with a persistent light theme
- Expiring client-side administrator session and protected dashboard routes
- Real Supabase CRUD for authors, blogs, categories, comments, galleries, PDF books, public submissions, videos, and site settings
- Relationship-aware blog editing with categories and authors
- WYSIWYG article and submission editors with sanitized HTML/source mode
- Slug generation and server-backed uniqueness checks
- Before-save article, gallery, book, submission, and video previews
- Reusable ImgBB image uploader with drag/drop, progress, validation, metadata, and delete URL preservation
- PDF uploads to a 32 MB Supabase Storage bucket
- Transactional submission-to-blog conversion with author de-duplication
- Real Chart.js metrics and activity calculated from Supabase records
- Global `Ctrl + K` search
- Accessible modals, confirmations, toasts, loading states, empty states, and keyboard navigation
- Responsive behavior from 320 px through large desktop screens

## Project structure

```text
backend/
├── index.html
├── README.md
├── assets/
│   ├── css/
│   │   └── styles.css
│   └── js/
│       ├── config.js          # Central browser-safe configuration
│       ├── utils.js           # Formatting, validation, sanitization helpers
│       ├── auth.js            # Demo session abstraction and expiry
│       ├── api.js             # Central Supabase REST/Storage API layer
│       ├── components.js      # Modal, toast, table, badges, states
│       ├── media.js           # ImgBB, PDF, and safe video previews
│       ├── editor.js          # Quill/fallback rich-text abstraction
│       ├── crud.js            # Shared list and CRUD behavior
│       ├── dashboard.js       # Metrics, charts, activity
│       ├── authors.js
│       ├── blogs.js
│       ├── categories.js
│       ├── comments.js
│       ├── galleries.js
│       ├── books.js
│       ├── submissions.js
│       ├── videos.js
│       ├── settings.js
│       └── app.js             # Shell, routing, theme, search
└── supabase/
    ├── schema.sql
    └── migrations/
        └── 002_production_rls.sql
```

## Database setup

### What was inspected

A read-only request was made to the configured Supabase REST API on **2026-08-29**. None of these required public tables existed in the PostgREST schema cache:

- `authors`
- `categories`
- `blogs`
- `comments`
- `galleries`
- `pdf_books`
- `submitted_blogs`
- `videos`
- `settings`

No data was deleted or modified during inspection. The supplied publishable key cannot run SQL migrations, so database installation must be performed once in the Supabase SQL Editor by a project owner.

### Install the schema

1. Sign in to the correct [Supabase dashboard](https://supabase.com/dashboard).
2. Open **SQL Editor** for the project.
3. Review `supabase/schema.sql`.
4. Run the complete file.
5. Return to this dashboard and open **Settings → Authentication & database → Run check**.

The migration is additive and uses `CREATE TABLE IF NOT EXISTS`; it does not intentionally drop content tables or rows. It creates:

- UUID primary keys
- Foreign keys for blog author/category and comment/blog relationships
- Status and value constraints
- Case-insensitive unique slug indexes
- Search and relationship indexes
- `created_at` / `updated_at` fields and update triggers
- Compatibility snapshot fields maintained by database triggers for the existing Android client
- RLS policies
- A `pdf-books` Storage bucket (PDF only, 32 MB)
- A transactional `approve_submission(...)` RPC

Always review and back up an established production database before running a migration.

## Supabase configuration

Browser-safe configuration is centralized in `assets/js/config.js`:

```js
supabase: {
  url: 'https://YOUR_PROJECT.supabase.co',
  publishableKey: 'YOUR_PUBLISHABLE_KEY',
  pdfBucket: 'pdf-books'
}
```

Only a **publishable browser key** belongs here. Never add any of the following to frontend files:

- `service_role` key
- database password
- Supabase management token
- private server credential

`api.js` centralizes REST headers, filters, CRUD, RPC calls, Storage uploads, timeouts, and user-friendly API errors.

## Authentication

Initial demo access:

```text
Username: admin
Password: admin123
```

`auth.js` performs a centralized SHA-256 comparison, creates a random expiring session record, and stores it in:

- `sessionStorage` for the standard 8-hour session, or
- `localStorage` for the optional 7-day remembered session.

All routes return to Login after expiration. The plain password is not stored in session data.

### Important security limitation

This is **client-side demo authentication**. Anyone who can inspect browser code can bypass a purely client-side gate. The initial schema uses a demo request digest so CRUD works without exposing a service-role key, but that digest is also discoverable by a determined browser user.

Before placing the dashboard at a public URL:

1. Create administrator/editor users with Supabase Auth.
2. Put authorization roles in `app_metadata.role`.
3. Update `auth.js` to use Supabase email/password authentication and store the returned access token.
4. Apply `supabase/migrations/002_production_rls.sql`.
5. Remove the demo credential hint from `index.html`.
6. Rotate the publishable and ImgBB keys if they were used in an untrusted environment.

The production migration replaces the demo header check with `auth.uid()` and JWT role checks without deleting content.

### Changing demo credentials

For temporary local/staging use only:

1. Generate SHA-256 of the new password and replace `auth.passwordHash` in `assets/js/config.js`.
2. Generate SHA-256 of `username:new-password`.
3. Replace the expected digest in `public.is_dashboard_request()` inside `supabase/schema.sql`, then run only that function definition in SQL Editor.
4. Change `auth.username` if required.
5. Sign out and clear old browser sessions.

Do not treat this as production security. Prefer Supabase Auth rather than maintaining demo credentials.

## ImgBB setup

The ImgBB client configuration is centralized in `assets/js/config.js`:

```js
imgbb: {
  endpoint: 'https://api.imgbb.com/1/upload',
  apiKey: 'YOUR_IMGBB_UPLOAD_KEY',
  maxBytes: 32 * 1024 * 1024
}
```

The reusable uploader captures and preserves:

- `url`
- `display_url`
- `delete_url`
- `filename`
- `size`
- `mime`
- provider and upload timestamp

When replacing media, the dashboard uploads and saves the new image first, then attempts to remove the old image.

### ImgBB deletion behavior

ImgBB commonly supplies a human deletion page rather than a documented cross-origin deletion API. The dashboard:

1. Deletes the database record/reference.
2. Attempts a remote `DELETE` using the saved deletion URL.
3. Reports failures honestly.
4. Offers the saved ImgBB deletion page for manual completion.

It never silently claims remote deletion succeeded when ImgBB or browser CORS does not confirm it.

## PDF uploads

ImgBB is an image host and is not a reliable PDF host. Cover images use ImgBB; actual PDF files use the `pdf-books` Supabase Storage bucket created by `schema.sql`.

- Accepted file: PDF
- Maximum size: 32 MB
- Direct public PDF links remain supported
- Storage paths are preserved for later cleanup
- Old stored files are deleted only after the replacement record saves successfully

## Running locally

A local HTTP server is recommended so browser security APIs and CDN assets work consistently.

```bash
cd backend
python3 -m http.server 8080
```

Open:

```text
http://localhost:8080
```

No `npm install`, compiler, bundler, or build step is required.

Opening `index.html` directly may work in some browsers, but a local server avoids browser restrictions around secure hashing, modules/CDNs, and cross-origin requests.

## CDN dependencies

- Tailwind CSS Play CDN
- Font Awesome 6 Pro from the provided project URL
- Chart.js `4.4.7`
- Quill `2.0.3`
- DOMPurify `3.2.4`

Pinned versions are used where the CDN package supports it. If a CDN is blocked, core error and empty states remain readable; rich-text editing falls back to a basic contenteditable editor.

## Rich text and XSS safety

Blog and submission HTML is treated as untrusted. The editor abstraction sanitizes content with DOMPurify:

- before returning editor content
- before preview rendering
- before injecting stored content into dashboard previews

External URLs are validated as `http:` or `https:`. Video previews use provider-specific, encoded iframe URLs; arbitrary embed HTML is never accepted.

For defense in depth, the public website should sanitize content again on render and deploy a restrictive Content Security Policy.

## Submission moderation

Supported states:

- Pending
- Reviewed
- Approved
- Rejected

Approval calls the transactional `approve_submission(...)` database function. It:

1. Locks and reads the submission.
2. Finds an author by case-insensitive writer name.
3. Creates an unverified author only if no match exists.
4. Creates the related Blog as Draft or Publish.
5. Preserves thumbnail, content title, and article content.
6. Marks the submission Approved and stores `converted_blog_id`.

The frontend has a compatibility fallback if the RPC has not yet been installed, but the SQL RPC is preferred because it is atomic.

## Theme and dashboard preferences

- Dark is the default.
- The topbar theme button and Settings page both update the theme.
- Theme, landing page, page size, and table density are stored as browser preferences.
- Public-site settings are stored in the Supabase `settings` table.

## Deployment

The dashboard is static and can be deployed to:

- GitHub Pages
- Cloudflare Pages
- Netlify
- Vercel static hosting
- Any HTTPS web server

Deploy the contents of `backend/` as the site root. Then:

1. Verify Supabase project CORS/origin settings.
2. Use HTTPS.
3. Configure a strict Content Security Policy that permits only the required CDNs, Supabase project, ImgBB, and supported video providers.
4. Complete the Supabase Auth migration before exposing the dashboard publicly.
5. Keep the dashboard on a restricted admin subdomain where practical.

## Data handling guarantees

The implementation follows these rules:

- PATCH updates only known form fields rather than replacing entire records.
- Existing optional values and relationships remain unless explicitly changed.
- Old ImgBB media is not deleted before a replacement upload and save succeeds.
- Foreign keys, not duplicated labels, are authoritative relationships.
- Compatibility labels for the Android client are maintained by database triggers.
- API failures remain visible and do not break the entire dashboard shell.
- No fake dashboard statistics are used; charts display zero/empty states until real records exist.

## QA performed

The implementation was checked with a local HTTP server and headless Chromium at desktop and mobile widths.

Completed checks:

- All JavaScript files pass `node --check`.
- Both SQL files parse successfully as PostgreSQL statements.
- Login, logout/session transition, dark/light switching, global search dialog, schema warning, and mobile sidebar were exercised.
- Dashboard rendering was checked at 320, 375, 414, 768, 1024, 1280, and 1440 px; no document-level horizontal overflow was detected.
- Every entity list and add/edit form was rendered against intercepted Supabase-shaped fixtures.
- Author and Category POST payloads, Blog draft POST payload, Comment status PATCH, and Video DELETE confirmation were exercised end to end through the API abstraction.
- Blog preview, rich editor, two-image submission form, submission approval form, PDF uploader, and provider-safe video preview were opened successfully.
- Automated axe accessibility checks reported zero violations for the login and dashboard views at desktop and mobile sizes.
- No uncaught browser page errors occurred in the fixture-backed navigation pass.

Live Supabase CRUD cannot be verified until `supabase/schema.sql` is installed because the configured project currently returns “table not found” for every required entity. No ImgBB test upload was created during QA, avoiding orphaned third-party media. Perform the final live add/edit/delete/upload checklist in a staging project after schema installation.

## Troubleshooting

### “Database setup required”

Run `supabase/schema.sql` in the project SQL Editor and then use the Settings database check.

### `401` / `403` saving records

- Sign out and sign in again.
- Confirm the initial RLS helper was installed.
- If production RLS is active, authenticate through Supabase Auth with an allowed role.

### Image upload fails

- Confirm the file is an accepted image type.
- Confirm it is below 32 MB.
- Check ImgBB key restrictions and browser network logs.

### PDF upload fails

- Confirm `schema.sql` created the `pdf-books` bucket and Storage policies.
- Use a PDF below 32 MB.
- Use a direct public PDF URL as a fallback.

### Charts or editor do not load

Check whether the browser or content blocker is blocking the pinned CDN URLs.

## Production recommendations

- Replace demo login with Supabase Auth immediately.
- Require MFA for administrators if available.
- Store roles in `app_metadata`, not user-editable profile fields.
- Add audit logs for destructive and publishing actions.
- Add rate limits and CAPTCHA to public comment/submission forms.
- Run ImgBB deletion through a trusted server/Edge Function if automatic deletion is essential.
- Add automated browser tests against a staging Supabase project.
- Back up content and Storage objects on a schedule.
