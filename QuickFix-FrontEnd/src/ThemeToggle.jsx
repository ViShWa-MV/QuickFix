import { useEffect, useState } from 'react'

import { effectiveTheme, setTheme } from './theme'


/** Floating day/night switch, mounted once app-wide. */
function ThemeToggle() {

  const [theme, setThemeState] = useState(() => effectiveTheme())

  // While the user hasn't made an explicit choice, follow OS changes live.
  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => {
      try {
        if (!localStorage.getItem('quickfix-theme')) {
          setThemeState(mq.matches ? 'dark' : 'light')
        }
      } catch { /* ignore */ }
    }
    mq.addEventListener?.('change', onChange)
    return () => mq.removeEventListener?.('change', onChange)
  }, [])

  const isDark = theme === 'dark'

  const toggle = () => {
    const next = isDark ? 'light' : 'dark'
    setTheme(next)
    setThemeState(next)
  }

  return (
    <button
      className="theme-toggle"
      onClick={toggle}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Light mode' : 'Dark mode'}
    >
      <span aria-hidden="true">{isDark ? '☀️' : '🌙'}</span>
    </button>
  )
}


export default ThemeToggle
