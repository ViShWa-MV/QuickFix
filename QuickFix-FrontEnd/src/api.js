// =========================================================
// API configuration
//
// Base URL of the QuickFix user-service backend.
// Override per-environment with VITE_API_URL in a .env file;
// falls back to the local dev backend when unset.
// =========================================================

const API_BASE_URL =
  import.meta.env.VITE_API_URL || 'http://localhost:8081'

export default API_BASE_URL
