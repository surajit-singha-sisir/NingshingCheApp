-- Ningshing Che Dashboard — production authentication hardening
-- Apply ONLY after creating Supabase Auth users and adding one of these roles
-- to app_metadata.role: administrator, editor, moderator.
--
-- This removes the demo x-dashboard-token authorization path. Existing RLS
-- policies continue to call is_dashboard_request(), so no content is dropped.

begin;

create or replace function public.is_dashboard_request()
returns boolean
language sql
stable
security definer
set search_path = public, auth
as $$
  select
    auth.uid() is not null
    and lower(coalesce(
      auth.jwt() -> 'app_metadata' ->> 'role',
      auth.jwt() -> 'user_metadata' ->> 'role',
      ''
    )) in ('administrator', 'admin', 'editor', 'moderator');
$$;

revoke all on function public.is_dashboard_request() from public;
grant execute on function public.is_dashboard_request() to anon, authenticated;

commit;
