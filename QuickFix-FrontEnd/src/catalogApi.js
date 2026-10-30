// =========================================================
// Catalog service configuration
//
// Base URL of the QuickFix catalog-service (categories,
// technician profiles, availability, search).
// Override per-environment with VITE_CATALOG_API_URL;
// falls back to the local dev backend when unset.
// =========================================================

const CATALOG_BASE_URL =
  import.meta.env.VITE_CATALOG_API_URL || 'http://localhost:8082'

export default CATALOG_BASE_URL
