import { Fragment, useEffect, useRef } from 'react'

import API_BASE_URL from '../api'


function Login() {

  // Reveal-on-scroll — content stays fully visible without JS / reduced motion.
  const rootRef = useRef(null)
  useEffect(() => {
    const root = rootRef.current
    if (!root) return
    const items = root.querySelectorAll('.reveal')
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduce || !('IntersectionObserver' in window)) {
      items.forEach(el => el.classList.add('in'))
      return
    }
    root.classList.add('js-reveal')
    const io = new IntersectionObserver((entries) => {
      entries.forEach(e => {
        if (e.isIntersecting) { e.target.classList.add('in'); io.unobserve(e.target) }
      })
    }, { threshold: 0.12 })
    items.forEach(el => io.observe(el))
    return () => io.disconnect()
  }, [])

  // Cursor-driven 3D tilt for the booking flow diagram.
  const flowRef = useRef(null)
  useEffect(() => {
    const el = flowRef.current
    if (!el) return
    const scene = el.querySelector('.flow3d-scene')
    if (!scene) return
    const reset = () => {
      scene.style.setProperty('--rx', '9deg')
      scene.style.setProperty('--ry', '-7deg')
    }
    reset()
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const onMove = (e) => {
      const r = el.getBoundingClientRect()
      const px = (e.clientX - r.left) / r.width - 0.5
      const py = (e.clientY - r.top) / r.height - 0.5
      scene.style.setProperty('--rx', `${(-py * 10 + 6).toFixed(2)}deg`)
      scene.style.setProperty('--ry', `${(px * 14).toFixed(2)}deg`)
    }
    el.addEventListener('mousemove', onMove)
    el.addEventListener('mouseleave', reset)
    return () => {
      el.removeEventListener('mousemove', onMove)
      el.removeEventListener('mouseleave', reset)
    }
  }, [])

  const handleGoogleLogin = () => {
    window.location.href = `${API_BASE_URL}/oauth2/authorization/google`
  }

  const GoogleButton = ({ label = 'Continue with Google' }) => (
    <button className="google-btn" onClick={handleGoogleLogin}>
      <svg className="google-g" viewBox="0 0 48 48" aria-hidden="true">
        <path fill="#EA4335" d="M24 9.5c3.5 0 6.6 1.2 9 3.6l6.7-6.7C35.6 2.6 30.1 0 24 0 14.6 0 6.4 5.4 2.6 13.2l7.8 6.1C12.2 13.3 17.6 9.5 24 9.5z"/>
        <path fill="#4285F4" d="M46.1 24.5c0-1.6-.1-3.1-.4-4.5H24v9h12.4c-.5 2.9-2.1 5.3-4.6 7l7.1 5.5c4.2-3.9 6.6-9.6 6.6-16.5z"/>
        <path fill="#FBBC05" d="M10.4 28.3a14.6 14.6 0 0 1 0-8.6l-7.8-6.1a24 24 0 0 0 0 20.8l7.8-6.1z"/>
        <path fill="#34A853" d="M24 48c6.5 0 11.9-2.1 15.9-5.8l-7.1-5.5c-2 1.3-4.6 2.1-8.8 2.1-6.4 0-11.8-3.8-13.6-9.3l-7.8 6.1C6.4 42.6 14.6 48 24 48z"/>
      </svg>
      {label}
    </button>
  )

  const categories = [
    { icon: '❄️', name: 'Air Conditioning', color: '#06b6d4' },
    { icon: '🎨', name: 'Painting', color: '#f59e0b' },
    { icon: '💡', name: 'Electrical', color: '#f97316' },
    { icon: '🚰', name: 'Plumbing', color: '#3b82f6' },
    { icon: '🪚', name: 'Carpentry', color: '#b45309' },
    { icon: '🧹', name: 'Cleaning', color: '#10b981' },
  ]

  const flow = [
    { icon: '🔎', title: 'Search', sub: 'service · area · time', color: '#7c3aed' },
    { icon: '📅', title: 'Book', sub: 'pick an open slot', color: '#2563eb' },
    { icon: '👍', title: 'Accept', sub: 'technician confirms', color: '#0891b2' },
    { icon: '📍', title: 'Arrive', sub: 'share arrival OTP', color: '#f59e0b' },
    { icon: '🔧', title: 'In progress', sub: 'work gets done', color: '#db2777' },
    { icon: '✅', title: 'Done', sub: 'confirm with OTP', color: '#10b981' },
  ]

  return (
    <div className="landing" ref={rootRef}>

      <nav className="landing-nav">
        <div className="landing-logo">QuickFix</div>
        <button className="landing-signin" onClick={handleGoogleLogin}>Sign in</button>
      </nav>

      <header className="landing-hero">
        <div className="hero-aurora" aria-hidden="true" />

        <div className="hero-copy reveal">
          <span className="hero-eyebrow">Trusted home services</span>
          <h1>Home repairs, handled by <span className="accent">verified pros.</span></h1>
          <p className="hero-sub">
            Book background-checked technicians for AC, plumbing, electrical,
            painting and more — at a time that works for you, with every visit
            secured by a one-time code.
          </p>

          <div className="hero-cta-group">
            <GoogleButton />
            <span className="hero-cta-note">Free to join · No app download</span>
          </div>

          <ul className="trust-row">
            <li><strong>★ 4.8</strong> average rating</li>
            <li><strong>🔒</strong> OTP-verified visits</li>
            <li><strong>🛡</strong> Background-checked pros</li>
          </ul>
        </div>

        <div className="hero-visual reveal">
          <div className="preview-card">
            <div className="preview-head">
              <div className="preview-avatar">RK</div>
              <div className="preview-id">
                <strong>Ravi Kumar</strong>
                <span>AC Repair &amp; Service</span>
              </div>
              <div className="preview-rating">★ 4.9</div>
            </div>
            <div className="preview-status">
              <span className="preview-dot" /> On the way · ETA 12 min
            </div>
            <div className="preview-otp">
              <span className="preview-otp-label">Arrival code — share with technician</span>
              <span className="preview-otp-code">4&nbsp;8&nbsp;2&nbsp;1</span>
            </div>
          </div>
          <span className="float-chip float-chip--1">❄️ AC fixed in 45 min</span>
          <span className="float-chip float-chip--2">🧹 Deep clean booked</span>
        </div>
      </header>

      {/* scrolling services marquee */}
      <div className="marquee" aria-hidden="true">
        <div className="marquee-track">
          {[...categories, ...categories].map((c, i) => (
            <span className="marquee-item" key={i} style={{ '--c': c.color }}>
              <span className="marquee-dot" />{c.icon} {c.name}
            </span>
          ))}
        </div>
      </div>

      <section className="landing-section reveal">
        <h2>How a booking flows</h2>
        <p className="flow-caption">
          From search to sign-off — every step tracked, every visit verified.
        </p>
        <div className="flow3d" ref={flowRef}>
          <div className="flow3d-scene">
            {flow.map((n, i) => (
              <Fragment key={n.title}>
                <div className="flow-node" style={{ '--c': n.color }}>
                  <span className="flow-node-step">{i + 1}</span>
                  <span className="flow-node-icon">{n.icon}</span>
                  <span className="flow-node-title">{n.title}</span>
                  <span className="flow-node-sub">{n.sub}</span>
                </div>
                {i < flow.length - 1 && (
                  <span className="flow-arrow" aria-hidden="true">→</span>
                )}
              </Fragment>
            ))}
            <span className="flow-pulse" aria-hidden="true" />
          </div>
        </div>
      </section>

      <section className="landing-section reveal">
        <h2>Services you can book</h2>
        <div className="cat-grid">
          {categories.map(c => (
            <div className="cat-card" key={c.name} style={{ '--c': c.color }}>
              <span className="cat-icon">{c.icon}</span>
              <span className="cat-name">{c.name}</span>
            </div>
          ))}
        </div>
      </section>

      <section className="landing-final reveal">
        <div className="final-panel">
          <h2>Ready to get something fixed?</h2>
          <p>Sign in with Google and book your first technician in minutes.</p>
          <GoogleButton label="Get started with Google" />
        </div>
      </section>

      <footer className="landing-footer">
        QuickFix · Home services, verified end to end
      </footer>
    </div>
  )
}


export default Login
