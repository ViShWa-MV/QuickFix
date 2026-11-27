-- Verified service addresses.
-- Existing rows have no street or coordinates, so they start UNVERIFIED and must be
-- re-entered before they can be used for a booking.
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS street_name VARCHAR(160);
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS locality VARCHAR(160);
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS osm_place_id BIGINT;
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED';

-- Official pincode list (India Post). Loaded by ops/geo/load-pincodes.sh.
CREATE TABLE IF NOT EXISTS pincodes (
    pincode  VARCHAR(6)   PRIMARY KEY,
    office   VARCHAR(160),
    district VARCHAR(120) NOT NULL,
    state    VARCHAR(120) NOT NULL
);
