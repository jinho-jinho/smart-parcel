-- Keep already-applied V1-V3 checksums intact. Archive old rows; use one live domain.
CREATE SCHEMA legacy;
ALTER TABLE public.rule_chutes SET SCHEMA legacy;
ALTER TABLE public.sorting_history SET SCHEMA legacy;
ALTER TABLE public.error_logs SET SCHEMA legacy;
ALTER TABLE public.user_notifications SET SCHEMA legacy;
ALTER TABLE public.sorting_rules SET SCHEMA legacy;
ALTER TABLE public.chutes SET SCHEMA legacy;
ALTER TABLE public.sorting_groups SET SCHEMA legacy;

DO $$
BEGIN
    IF to_regprocedure('public.fill_snapshots()') IS NOT NULL THEN
        ALTER FUNCTION public.fill_snapshots() SET SCHEMA legacy;
        ALTER FUNCTION legacy.fill_snapshots() SET search_path = legacy, public;
    END IF;
END $$;
ALTER TABLE parcel.organizations SET SCHEMA public;
ALTER TABLE parcel.conveyor_belts SET SCHEMA public;
ALTER TABLE parcel.devices SET SCHEMA public;
ALTER TABLE parcel.chutes SET SCHEMA public;
ALTER TABLE parcel.sorting_groups SET SCHEMA public;
ALTER TABLE parcel.rule_versions SET SCHEMA public;
ALTER TABLE parcel.version_chutes SET SCHEMA public;
ALTER TABLE parcel.sorting_rules SET SCHEMA public;
ALTER TABLE parcel.sorting_attempts SET SCHEMA public;
ALTER TABLE parcel.device_events SET SCHEMA public;
ALTER TABLE parcel.event_images SET SCHEMA public;
ALTER TABLE parcel.user_notifications SET SCHEMA public;
ALTER TABLE parcel.migration_issues SET SCHEMA public;
ALTER FUNCTION parcel.guard_rule_version_row() SET SCHEMA public;
ALTER FUNCTION public.guard_rule_version_row() SET search_path = public;
ALTER FUNCTION parcel.guard_rule_version_child() SET SCHEMA public;
ALTER FUNCTION public.guard_rule_version_child() SET search_path = public;
ALTER FUNCTION parcel.assert_published_version(BIGINT, BIGINT, BIGINT) SET SCHEMA public;
ALTER FUNCTION public.assert_published_version(BIGINT, BIGINT, BIGINT) SET search_path = public;
ALTER FUNCTION parcel.check_belt_desired_version() SET SCHEMA public;
ALTER FUNCTION public.check_belt_desired_version() SET search_path = public;
ALTER FUNCTION parcel.check_device_applied_version() SET SCHEMA public;
ALTER FUNCTION public.check_device_applied_version() SET search_path = public;
ALTER FUNCTION parcel.check_attempt_rule_version() SET SCHEMA public;
ALTER FUNCTION public.check_attempt_rule_version() SET search_path = public;
DROP SCHEMA parcel;
UPDATE public.device_events SET image_uri = replace(image_uri, '/api/v2/events/', '/api/events/') WHERE image_uri LIKE '/api/v2/events/%';
UPDATE public.sorting_attempts SET image_uri = replace(image_uri, '/api/v2/events/', '/api/events/') WHERE image_uri LIKE '/api/v2/events/%';
