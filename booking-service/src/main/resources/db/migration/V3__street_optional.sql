-- Streets are no longer required: an address is verified by its pincode, city, and state.
ALTER TABLE booking_addresses ALTER COLUMN street_name DROP NOT NULL;
