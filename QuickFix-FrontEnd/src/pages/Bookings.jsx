import { useEffect, useState } from 'react'

import BOOKING_BASE_URL from '../bookingApi'


/*
 * My Bookings (customer view)
 *
 * The customer tracks their bookings, cancels while still cancellable, and
 * approves or rejects a technician's quote. Accepting/advancing a job is the
 * technician's side (Technician Dashboard), not here.
 */
function Bookings({ user, onHome, onProfile, onLogout, onFind }) {

  const [bookings, setBookings] = useState([])
  const [quotes, setQuotes] = useState({})
  const [codes, setCodes] = useState({})   // bookingId -> active OTP to display
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const load = async () => {
    if (!user?.id) { setLoading(false); return }
    setLoading(true)
    try {
      const r = await fetch(`${BOOKING_BASE_URL}/api/bookings/customer/${user.id}`)
      const list = r.ok ? await r.json() : []
      setBookings(list)
      const entries = await Promise.all(list.map(async b => {
        try {
          const qr = await fetch(`${BOOKING_BASE_URL}/api/bookings/${b.id}/quote`)
          return [b.id, qr.ok ? await qr.json() : null]
        } catch { return [b.id, null] }
      }))
      setQuotes(Object.fromEntries(entries))

      // Active code to show the customer while a milestone is pending.
      const codeEntries = await Promise.all(list.map(async b => {
        const purpose = b.status === 'EN_ROUTE' ? 'START'
          : b.status === 'IN_PROGRESS' ? 'COMPLETION' : null
        if (!purpose) return [b.id, null]
        try {
          const cr = await fetch(
            `${BOOKING_BASE_URL}/api/bookings/${b.id}/otp?purpose=${purpose}`)
          return [b.id, cr.ok ? await cr.json() : null]
        } catch { return [b.id, null] }
      }))
      setCodes(Object.fromEntries(codeEntries))
      setError(null)
    } catch {
      setError('Could not load your bookings.')
    } finally {
      setLoading(false)
    }
  }

  // Poll so a code the technician just triggered appears without a manual reload.
  useEffect(() => {
    load()
    const timer = setInterval(load, 5000)
    return () => clearInterval(timer)
  }, [user?.id])  // eslint-disable-line react-hooks/exhaustive-deps

  const act = async (id, path) => {
    setBusy(true)
    try {
      const r = await fetch(`${BOOKING_BASE_URL}/api/bookings/${id}/${path}`, { method: 'POST' })
      if (!r.ok) {
        const b = await r.json().catch(() => null)
        setError(b?.message || `Action failed (${r.status})`)
      } else {
        setError(null)
      }
    } catch {
      setError('Action failed — is the service running?')
    } finally {
      setBusy(false)
      load()
    }
  }

  const canCancel = (s) => ['REQUESTED', 'CONFIRMED', 'EN_ROUTE'].includes(s)

  const statusClass = (s) => {
    if (['CONFIRMED', 'EN_ROUTE', 'IN_PROGRESS'].includes(s)) return 'pill pill--active'
    if (['COMPLETED', 'CLOSED'].includes(s)) return 'pill pill--done'
    if (['DECLINED', 'EXPIRED', 'CANCELLED'].includes(s)) return 'pill pill--dead'
    return 'pill'
  }

  const statusHint = (s) => {
    switch (s) {
      case 'REQUESTED': return 'Waiting for the technician to accept.'
      case 'CONFIRMED': return 'Accepted — the technician will be on the way.'
      case 'EN_ROUTE':  return 'The technician is on the way.'
      case 'IN_PROGRESS': return 'Work in progress.'
      case 'DECLINED':  return 'The technician declined this request.'
      case 'EXPIRED':   return 'The request expired before it was accepted.'
      default: return ''
    }
  }

  return (
    <div className="finder-page">

      <header className="dashboard-header">
        <div className="dashboard-logo" onClick={onHome} style={{ cursor: 'pointer' }}>QuickFix</div>
        <div className="dashboard-navigation">
          <button className="profile-button" onClick={onHome}>Home</button>
          <button className="profile-button" onClick={onFind}>Find a Technician</button>
          <button className="profile-button profile-button--active">My Bookings</button>
        </div>
        <div className="dashboard-user">
          <button className="profile-button" onClick={onProfile}>👤 {user?.name || user?.email || 'Guest'}</button>
          <button className="profile-button" onClick={onLogout}>Logout</button>
        </div>
      </header>

      <main className="finder-main">

        <section className="finder-hero">
          <h1>My Bookings</h1>
          <p>Track your bookings and approve any extra charges your technician proposes.</p>
        </section>

        {error && <div className="finder-error" style={{ marginBottom: 16 }}>{error}</div>}

        {loading && <p className="finder-note">Loading…</p>}

        {!loading && bookings.length === 0 && (
          <div className="finder-empty">
            <div className="finder-empty-icon">🗓️</div>
            <h3>No bookings yet</h3>
            <p>Find a technician and book a slot to see it here.</p>
            <button className="finder-submit" style={{ marginTop: 14 }} onClick={onFind}>Find a Technician</button>
          </div>
        )}

        {!loading && bookings.length > 0 && (
          <div className="booking-list">
            {bookings.map(b => {
              const quote = quotes[b.id]
              return (
                <article className="booking-card" key={b.id}>
                  <div className="booking-top">
                    <div>
                      <h3>Booking #{b.id}</h3>
                      <span className="tech-sub">
                        Technician #{b.technicianProfileId} · Service #{b.serviceId} · Slot #{b.slotId}
                      </span>
                    </div>
                    <span className={statusClass(b.status)}>{b.status}</span>
                  </div>

                  {statusHint(b.status) && <p className="booking-hint">{statusHint(b.status)}</p>}

                  {codes[b.id] && (
                    <div className="otp-callout">
                      <span className="otp-callout-label">
                        {codes[b.id].purpose === 'START' ? 'Arrival code' : 'Completion code'}
                        {' '}— share with your technician
                      </span>
                      <span className="otp-code">{codes[b.id].code}</span>
                    </div>
                  )}

                  <div className="booking-amounts">
                    <span>Base ₹{b.baseAmount ?? 0}</span>
                    {quote && quote.status === 'APPROVED' && (
                      <>
                        <span>+ Quote ₹{quote.amount}</span>
                        <strong>Total ₹{(b.baseAmount ?? 0) + quote.amount}</strong>
                      </>
                    )}
                  </div>

                  {quote && (
                    <div className={`quote-box quote-box--${quote.status.toLowerCase()}`}>
                      <div className="quote-head">
                        <span>Your technician proposed a quote — {quote.status} (₹{quote.amount})</span>
                        {quote.status === 'PROPOSED' && (
                          <span className="quote-actions">
                            <button className="btn-mini" disabled={busy}
                                    onClick={() => act(b.id, 'quote/approve')}>Approve</button>
                            <button className="btn-mini btn-mini--ghost" disabled={busy}
                                    onClick={() => act(b.id, 'quote/reject')}>Reject</button>
                          </span>
                        )}
                      </div>
                      <ul className="quote-lines">
                        {quote.lineItems.map((li, i) => <li key={i}>{li.description} — ₹{li.amount}</li>)}
                      </ul>
                    </div>
                  )}

                  {canCancel(b.status) && (
                    <div className="booking-actions">
                      <button className="btn-mini btn-mini--ghost" disabled={busy}
                              onClick={() => act(b.id, 'cancel')}>Cancel booking</button>
                    </div>
                  )}
                </article>
              )
            })}
          </div>
        )}

      </main>
    </div>
  )
}

export default Bookings
