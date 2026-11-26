// =========================================================
// Theme (day / night) management
//
// Writes data-theme on <html> and persists the choice. The CSS in index.css
// maps data-theme + prefers-color-scheme to the palette:
//   - no stored choice  -> follows the OS
//   - "light" / "dark"  -> forces that theme
// =========================================================

const KEY = 'quickfix-theme'

export function storedTheme() {
  try {
    return localStorage.getItem(KEY)
  } catch {
    return null
  }
}

export function systemTheme() {
  return window.matchMedia
    && window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light'
}

/** The theme actually in effect right now. */
export function effectiveTheme() {
  return storedTheme() || systemTheme()
}

function apply(theme) {
  const root = document.documentElement
  if (theme === 'light' || theme === 'dark') {
    root.setAttribute('data-theme', theme)
  } else {
    root.removeAttribute('data-theme')
  }
}

/** Persist and apply an explicit choice. */
export function setTheme(theme) {
  try {
    localStorage.setItem(KEY, theme)
  } catch {
    /* ignore (private mode / blocked storage) */
  }
  apply(theme)
}

/** Apply the stored choice on startup (leaves OS default if none). */
export function initTheme() {
  const stored = storedTheme()
  if (stored) {
    apply(stored)
  }
}
