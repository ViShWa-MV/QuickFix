import { useEffect, useState } from 'react'

import CATALOG_BASE_URL from '../catalogApi'
import BOOKING_BASE_URL from '../bookingApi'


/*
 * Technician Dashboard
 *
 * Shown to a logged-in user whose role is TECHNICIAN. Resolves their provider
 * profile (catalog) by userId, lets them set it up + publish availability if
 * missing, and shows their booking queue with accept/reject/advance actions.
 *
 * INTERIM: a newly created profile is auto-approved here so the technician is
 * immediately searchable. This is temporary — the admin-approval step (next)
 * will replace the auto-approve, and profiles will stay PENDING until approved.
 */
function TechnicianDashboard({ user, onProfile, onLogout }) {

  const [categories, setCategories] = useState([])
  const [profile, setProfile] = useState(undefined)  // undefined=loading, null=none
  const [jobs, setJobs] = useState([])
  const [quotes, setQuotes] = useState({})
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  // setup form
  const [setup, setSetup] = useState({ skills: [], pincode: '560001', yearsExperience: 3 })
  // availability form
  const [avail, setAvail] = useState({
    date: '2026-11-03', from: '09:00', to: '12:00', minutes: 60
  })
  // quote form (per job)
  const [qForm, setQForm] = useState(null)
  // OTP the technician types in, keyed by booking id
  const [otpInput, setOtpInput] = useState({})

  // ---- initial load ----
  useEffect(() => {
    const init = async () => {
      try {
        const cr = await fetch(`${CATALOG_BASE_URL}/api/categories`)
        if (cr.ok) setCategories(await cr.json())
      } catch { /* non-fatal */ }
      await resolveProfile()
    }
    init()
  }, [user?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  const resolveProfile = async () => {
    if (!user?.id) { setProfile(null); return }
    try {
      const r = await fetch(`${CATALOG_BASE_URL}/api/technicians/by-user/${user.id}`)
      if (r.status === 404) { setProfile(null); return }
      const p = await r.json()
      setProfile(p)
      loadJobs(p.id)
    } catch {
      setError('Could not load your technician profile.')
      setProfile(null)
    }
  }

  const loadJobs = async (profileId) => {
    try {
      const r = await fetch(`${BOOKING_BASE_URL}/api/bookings/technician/${profileId}`)
      const list = r.ok ? await r.json() : []
      setJobs(list)
      const entries = await Promise.all(list.map(async b => {
        try {
          const qr = await fetch(`${BOOKING_BASE_URL}/api/bookings/${b.id}/quote`)
          return [b.id, qr.ok ? await qr.json() : null]
        } catch { return [b.id, null] }
      }))
      setQuotes(Object.fromEntries(entries))
    } catch { /* non-fatal */ }
  }

  // ---- profile setup (interim: register then auto-approve) ----
  const createProfile = async () => {
    if (setup.skills.length === 0) { setError('Pick at least one service.'); return }
    setBusy(true); setError(null)
    try {
      const reg = await fetch(`${CATALOG_BASE_URL}/api/technicians`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: user.id,
          skills: setup.skills,
          serviceAreas: [setup.pincode],
          yearsExperience: Number(setup.yearsExperience)
        })
      })
      const created = await reg.json().catch(() => null)
      if (!reg.ok) throw new Error(created?.message || `Setup failed (${reg.status})`)
      // INTERIM auto-approve so the profile is searchable now.
      await fetch(`${CATALOG_BASE_URL}/api/technicians/${created.id}/verification`, {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'APPROVED' })
      })
      await resolveProfile()
    } catch (err) {
      setError(err.message)
    } finally { setBusy(false) }
  }

  const toggleSkill = (id) => {
    setSetup(s => ({
      ...s,
      skills: s.skills.includes(id) ? s.skills.filter(x => x !== id) : [...s.skills, id]
    }))
  }

  // ---- availability ----
  const publishSlots = async () => {
    setBusy(true); setError(null)
    try {
      const r = await fetch(`${CATALOG_BASE_URL}/api/technicians/${profile.id}/slots/generate`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fromDate: avail.date, toDate: avail.date,
          dailyStart: `${avail.from}:00`, dailyEnd: `${avail.to}:00`,
          slotMinutes: Number(avail.minutes)
        })
      })
      if (!r.ok) { const b = await r.json().catch(() => null); throw new Error(b?.message || 'Could not publish slots') }
    } catch (err) { setError(err.message) }
    finally { setBusy(false) }
  }

  // ---- job actions ----
  const act = async (id, path) => {
    setBusy(true)
    try {
      const r = await fetch(`${BOOKING_BASE_URL}/api/bookings/${id}/${path}`, { method: 'POST' })
      if (!r.ok) { const b = await r.json().catch(() => null); setError(b?.message || `Action failed (${r.status})`) }
      else setError(null)
    } finally { setBusy(false); if (profile) loadJobs(profile.id) }
  }

  // POST an action that carries the typed OTP (start / complete).
  const actOtp = async (id, path) => {
    setBusy(true)
    try {
      const r = await fetch(`${BOOKING_BASE_URL}/api/bookings/${id}/${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ otp: otpInput[id] || '' })
      })
      if (!r.ok) {
        const b = await r.json().catch(() => null)
        setError(b?.message || `Failed (${r.status})`)
      } else {
        setError(null)
        setOtpInput({ ...otpInput, [id]: '' })
      }
    } finally { setBusy(false); if (profile) loadJobs(profile.id) }
  }

  const submitQuote = async (id) => {
    setBusy(true)
    try {
      const r = await fetch(`${BOOKING_BASE_URL}/api/bookings/${id}/quote`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ lineItems: [{ description: qForm.description, amount: Number(qForm.amount) }] })
      })
      if (!r.ok) { const b = await r.json().catch(() => null); setError(b?.message || 'Quote failed') }
      else { setError(null); setQForm(null) }
    } finally { setBusy(false); if (profile) loadJobs(profile.id) }
  }

  const header = (
    <header className="dashboard-header">
      <div className="dashboard-logo">QuickFix <span className="role-tag">Technician</span></div>
      <div className="dashboard-user">
        <button className="profile-button" onClick={onProfile}>👤 {user?.name || user?.email}</button>
        <button className="profile-button" onClick={onLogout}>Logout</button>
      </div>
    </header>
  )

  // ---- loading / setup states ----
  if (profile === undefined) {
    return <div className="finder-page">{header}<main className="finder-main"><p className="finder-note">Loading…</p></main></div>
  }

  if (profile === null) {
    return (
      <div className="finder-page">{header}
        <main className="finder-main">
          <section className="finder-hero">
            <h1>Set up your technician profile</h1>
            <p>Tell customers what you do and where. (For now this is approved automatically; admin approval is coming.)</p>
          </section>
          {error && <div className="finder-error" style={{ marginBottom: 16 }}>{error}</div>}
          <div className="booking-card">
            <div className="tech-meta-row">
              <span className="tech-meta-label">Services you offer</span>
              <div className="chip-row">
                {categories.map(c => (
                  <button key={c.id} type="button"
                          className={`chip-toggle ${setup.skills.includes(c.id) ? 'chip-toggle--on' : ''}`}
                          onClick={() => toggleSkill(c.id)}>
                    {c.name}
                  </button>
                ))}
              </div>
            </div>
            <div className="finder-field">
              <label>Service area (pincode)</label>
              <input type="text" value={setup.pincode}
                     onChange={e => setSetup({ ...setup, pincode: e.target.value })} />
            </div>
            <div className="finder-field">
              <label>Years of experience</label>
              <input type="number" min="0" value={setup.yearsExperience}
                     onChange={e => setSetup({ ...setup, yearsExperience: e.target.value })} />
            </div>
            <button className="finder-submit" disabled={busy} onClick={createProfile}>
              {busy ? 'Saving…' : 'Create profile'}
            </button>
          </div>
        </main>
      </div>
    )
  }

  // ---- active profile: queue ----
  const incoming = jobs.filter(j => j.status === 'REQUESTED')
  const active = jobs.filter(j => ['CONFIRMED', 'EN_ROUTE', 'IN_PROGRESS'].includes(j.status))
  const history = jobs.filter(j => ['COMPLETED', 'CLOSED', 'DECLINED', 'EXPIRED', 'CANCELLED'].includes(j.status))

  const actionsFor = (b) => {
    switch (b.status) {
      case 'CONFIRMED': return [['On the way', 'en-route']]
      default:          return []
    }
  }

  const JobCard = (b) => {
    const quote = quotes[b.id]
    return (
      <article className="booking-card" key={b.id}>
        <div className="booking-top">
          <div>
            <h3>Booking #{b.id}</h3>
            <span className="tech-sub">Customer #{b.customerId} · Service #{b.serviceId} · Slot #{b.slotId}</span>
          </div>
          <span className="pill">{b.status}</span>
        </div>
        <div className="booking-amounts"><span>Base ₹{b.baseAmount ?? 0}</span>
          {quote && <span>Quote {quote.status} ₹{quote.amount}</span>}
        </div>
        <div className="booking-actions">
          {b.status === 'REQUESTED' && (
            <>
              <button className="btn-mini" disabled={busy} onClick={() => act(b.id, 'accept')}>Accept</button>
              <button className="btn-mini btn-mini--ghost" disabled={busy} onClick={() => act(b.id, 'decline')}>Reject</button>
            </>
          )}
          {actionsFor(b).map(([label, path]) => (
            <button key={path} className="btn-mini" disabled={busy} onClick={() => act(b.id, path)}>{label}</button>
          ))}
          {b.status === 'IN_PROGRESS' && !quote && qForm?.bookingId !== b.id && (
            <button className="btn-mini btn-mini--ghost" disabled={busy}
                    onClick={() => setQForm({ bookingId: b.id, description: '', amount: '' })}>Propose quote</button>
          )}
        </div>

        {b.status === 'EN_ROUTE' && (
          <div className="otp-row">
            <button className="btn-mini btn-mini--ghost" disabled={busy}
                    onClick={() => act(b.id, 'arrive')}>I've arrived — send code</button>
            <input className="otp-input" inputMode="numeric" maxLength="6" placeholder="Start code"
                   value={otpInput[b.id] || ''}
                   onChange={e => setOtpInput({ ...otpInput, [b.id]: e.target.value })} />
            <button className="btn-mini" disabled={busy || (otpInput[b.id] || '').length !== 6}
                    onClick={() => actOtp(b.id, 'start')}>Start work</button>
          </div>
        )}

        {b.status === 'IN_PROGRESS' && (
          <div className="otp-row">
            <button className="btn-mini btn-mini--ghost" disabled={busy}
                    onClick={() => act(b.id, 'request-completion')}>Work done — send code</button>
            <input className="otp-input" inputMode="numeric" maxLength="6" placeholder="Completion code"
                   value={otpInput[b.id] || ''}
                   onChange={e => setOtpInput({ ...otpInput, [b.id]: e.target.value })} />
            <button className="btn-mini" disabled={busy || (otpInput[b.id] || '').length !== 6}
                    onClick={() => actOtp(b.id, 'complete')}>Complete</button>
          </div>
        )}
        {qForm?.bookingId === b.id && (
          <div className="quote-form">
            <input type="text" placeholder="Description" value={qForm.description}
                   onChange={e => setQForm({ ...qForm, description: e.target.value })} />
            <input type="number" placeholder="₹ amount" min="1" value={qForm.amount}
                   onChange={e => setQForm({ ...qForm, amount: e.target.value })} />
            <button className="btn-mini" disabled={busy || !qForm.description || !qForm.amount}
                    onClick={() => submitQuote(b.id)}>Send</button>
            <button className="btn-mini btn-mini--ghost" onClick={() => setQForm(null)}>Cancel</button>
          </div>
        )}
      </article>
    )
  }

  return (
    <div className="finder-page">{header}
      <main className="finder-main">
        <section className="finder-hero">
          <h1>Technician Dashboard</h1>
          <p>Profile #{profile.id} · {profile.verificationStatus} · {profile.serviceAreas.join(', ')}</p>
        </section>

        {error && <div className="finder-error" style={{ marginBottom: 16 }}>{error}</div>}

        {/* availability */}
        <div className="booking-card">
          <div className="booking-top"><h3>Publish availability</h3></div>
          <div className="avail-form">
            <div className="finder-field"><label>Date</label>
              <input type="date" value={avail.date} onChange={e => setAvail({ ...avail, date: e.target.value })} /></div>
            <div className="finder-field finder-field--time"><label>From</label>
              <input type="time" value={avail.from} onChange={e => setAvail({ ...avail, from: e.target.value })} /></div>
            <div className="finder-field finder-field--time"><label>To</label>
              <input type="time" value={avail.to} onChange={e => setAvail({ ...avail, to: e.target.value })} /></div>
            <div className="finder-field finder-field--time"><label>Slot (min)</label>
              <input type="number" min="15" step="15" value={avail.minutes}
                     onChange={e => setAvail({ ...avail, minutes: e.target.value })} /></div>
            <button className="finder-submit" disabled={busy} onClick={publishSlots}>Publish</button>
          </div>
        </div>

        <h2 className="queue-heading">Incoming requests {incoming.length > 0 && <span className="count-badge">{incoming.length}</span>}</h2>
        {incoming.length === 0 ? <p className="finder-note">No pending requests.</p>
          : <div className="booking-list">{incoming.map(JobCard)}</div>}

        <h2 className="queue-heading">Active jobs</h2>
        {active.length === 0 ? <p className="finder-note">No active jobs.</p>
          : <div className="booking-list">{active.map(JobCard)}</div>}

        {history.length > 0 && (
          <>
            <h2 className="queue-heading">History</h2>
            <div className="booking-list">{history.map(JobCard)}</div>
          </>
        )}
      </main>
    </div>
  )
}

export default TechnicianDashboard
