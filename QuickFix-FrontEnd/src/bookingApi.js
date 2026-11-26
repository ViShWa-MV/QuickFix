// =========================================================
// Booking service configuration
//
// Base URL of the QuickFix booking-service (bookings,
// lifecycle, in-job quotes).
// Override per-environment with VITE_BOOKING_API_URL;
// falls back to the local dev backend when unset.
// =========================================================

const BOOKING_BASE_URL =
  import.meta.env.VITE_BOOKING_API_URL || 'http://localhost:8083'

export default BOOKING_BASE_URL
