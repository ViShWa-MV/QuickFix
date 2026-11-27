-- Public name shown on technician cards. Filled in by technicians; nullable for profiles created before this version.
ALTER TABLE technician_profiles ADD COLUMN IF NOT EXISTS display_name VARCHAR(255);
