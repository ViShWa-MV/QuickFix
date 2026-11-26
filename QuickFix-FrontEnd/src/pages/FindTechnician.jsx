import { useEffect, useState } from 'react'

import CATALOG_BASE_URL from '../catalogApi'
import BOOKING_BASE_URL from '../bookingApi'


/*
 * Find a Technician
 *
 * Search catalog-service (GET /api/technicians/search) and book a technician
 * via booking-service (POST /api/bookings), picking a service in the category
 * and one of the technician's open slots.
 */
function FindTechnician({
  user,
  onHome,
  onProfile,
  onLogout,
  onBookings
}) {

  const [categories, setCategories] = useState([])

  const [form, setForm] = useState({
    category: '',
    pincode: '560001',
    date: '2026-11-03',
    from: '10:00',
    to: '11:00'
  })

  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [searched, setSearched] = useState(false)

  // Booking modal state
  const [bookTech, setBookTech] = useState(null)   // technician being booked
  const [bookServices, setBookServices] = useState([])
  const [bookSlots, setBookSlots] = useState([])
  const [bookSel, setBookSel] = useState({ serviceId: '', slotId: '' })
  const [bookBusy, setBookBusy] = useState(false)
  const [bookResult, setBookResult] = useState(null)   // {ok, message}


  // ---- load categories for the dropdown ----
  useEffect(() => {
    const load = async () => {
      try {
        const r = await fetch(`${CATALOG_BASE_URL}/api/categories`)
        if (!r.ok) return
        const data = await r.json()
        setCategories(data)
        if (data.length > 0) {
          setForm(f => ({ ...f, category: String(data[0].id) }))
        }
      } catch { /* non-fatal */ }
    }
    load()
  }, [])

  const categoryName = (id) => {
    const m = categories.find(c => c.id === id)
    return m ? m.name : `Category #${id}`
  }

  const change = (field) => (e) =>
    setForm({ ...form, [field]: e.target.value })


  // ---- search ----
  const handleSearch = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setSearched(true)
    try {
      const fromTs = `${form.date}T${form.from}:00`
      const toTs = `${form.date}T${form.to}:00`
      const url =
        `${CATALOG_BASE_URL}/api/technicians/search`
        + `?category=${encodeURIComponent(form.category)}`
        + `&pincode=${encodeURIComponent(form.pincode)}`
        + `&from=${encodeURIComponent(fromTs)}`
        + `&to=${encodeURIComponent(toTs)}`
      const r = await fetch(url)
      if (!r.ok) {
        const b = await r.json().catch(() => null)
        throw new Error(b?.message || `Search failed (${r.status})`)
      }
      setResults(await r.json())
    } catch (err) {
      setError(err.message || 'Something went wrong')
      setResults([])
    } finally {
      setLoading(false)
    }
  }


  // ---- open booking modal for a technician ----
  const openBooking = async (tech) => {
    setBookTech(tech)
    setBookResult(null)
    setBookSel({ serviceId: '', slotId: '' })
    setBookServices([])
    setBookSlots([])

    try {
      const [svcRes, slotRes] = await Promise.all([
        fetch(`${CATALOG_BASE_URL}/api/services/category/${form.category}`),
        fetch(`${CATALOG_BASE_URL}/api/technicians/${tech.id}/slots`)
      ])
      const services = svcRes.ok ? await svcRes.json() : []
      const slots = slotRes.ok ? await slotRes.json() : []
      const open = slots.filter(s => s.status === 'OPEN')
      setBookServices(services)
      setBookSlots(open)
      setBookSel({
        serviceId: services[0] ? String(services[0].id) : '',
        slotId: open[0] ? String(open[0].id) : ''
      })
    } catch {
      setBookResult({ ok: false, message: 'Could not load services or slots.' })
    }
  }

  const closeBooking = () => setBookTech(null)

  const confirmBooking = async () => {
    if (!user?.id) {
      setBookResult({ ok: false, message: 'Please log in again to book.' })
      return
    }
    setBookBusy(true)
    setBookResult(null)
    try {
      const r = await fetch(`${BOOKING_BASE_URL}/api/bookings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          customerId: user.id,
          technicianProfileId: bookTech.id,
          serviceId: Number(bookSel.serviceId),
          slotId: Number(bookSel.slotId)
        })
      })
      const body = await r.json().catch(() => null)
      if (!r.ok) {
        throw new Error(body?.message || `Booking failed (${r.status})`)
      }
      setBookResult({
        ok: true,
        message: `Requested! Booking #${body.id} is ${body.status}.`
      })
    } catch (err) {
      setBookResult({ ok: false, message: err.message })
    } finally {
      setBookBusy(false)
    }
  }

  const fmtSlot = (s) => {
    const d = s.startAt.slice(0, 10)
    const a = s.startAt.slice(11, 16)
    const b = s.endAt.slice(11, 16)
    return `${d}  ${a}–${b}`
  }


  return (
    <div className="finder-page">

      <header className="dashboard-header">
        <div className="dashboard-logo" onClick={onHome} style={{ cursor: 'pointer' }}>
          QuickFix
        </div>
        <div className="dashboard-navigation">
          <button className="profile-button" onClick={onHome}>Home</button>
          <button className="profile-button profile-button--active">Find a Technician</button>
          <button className="profile-button" onClick={onBookings}>My Bookings</button>
        </div>
        <div className="dashboard-user">
          <button className="profile-button" onClick={onProfile}>
            👤 {user?.name || user?.email || 'Guest'}
          </button>
          <button className="profile-button" onClick={onLogout}>Logout</button>
        </div>
      </header>

      <main className="finder-main">

        <section className="finder-hero">
          <h1>Find a Technician</h1>
          <p>Search verified technicians by service, area and time — ranked by rating and experience.</p>
        </section>

        <form className="finder-search" onSubmit={handleSearch}>
          <div className="finder-field">
            <label htmlFor="f-category">Service</label>
            <select id="f-category" value={form.category} onChange={change('category')} required>
              {categories.length === 0 && <option value="">No categories</option>}
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="finder-field">
            <label htmlFor="f-pincode">Pincode</label>
            <input id="f-pincode" type="text" inputMode="numeric" placeholder="e.g. 560001"
                   value={form.pincode} onChange={change('pincode')} required />
          </div>
          <div className="finder-field">
            <label htmlFor="f-date">Date</label>
            <input id="f-date" type="date" value={form.date} onChange={change('date')} required />
          </div>
          <div className="finder-field finder-field--time">
            <label htmlFor="f-from">From</label>
            <input id="f-from" type="time" value={form.from} onChange={change('from')} required />
          </div>
          <div className="finder-field finder-field--time">
            <label htmlFor="f-to">To</label>
            <input id="f-to" type="time" value={form.to} onChange={change('to')} required />
          </div>
          <button type="submit" className="finder-submit" disabled={loading}>
            {loading ? 'Searching…' : 'Search'}
          </button>
        </form>

        <section className="finder-results">

          {loading && <p className="finder-note">Searching for technicians…</p>}

          {!loading && error && (
            <div className="finder-error"><strong>Couldn’t search.</strong> {error}</div>
          )}

          {!loading && !error && searched && results.length === 0 && (
            <div className="finder-empty">
              <div className="finder-empty-icon">🔍</div>
              <h3>No technicians available</h3>
              <p>No verified technician matches this service, area and time window. Try a different date or pincode.</p>
            </div>
          )}

          {!loading && !error && results.length > 0 && (
            <>
              <p className="finder-count">
                {results.length} technician{results.length > 1 ? 's' : ''} available
              </p>
              <div className="tech-grid">
                {results.map(t => (
                  <article className="tech-card" key={t.id}>
                    <div className="tech-top">
                      <div className="tech-avatar">T{t.id}</div>
                      <div className="tech-id">
                        <h3>Technician #{t.id}</h3>
                        <span className="tech-sub">{t.yearsExperience} yrs experience</span>
                      </div>
                      <div className="tech-rating">
                        {t.ratingCount > 0
                          ? <><span className="tech-stars">★</span>{t.ratingAvg.toFixed(1)}
                              <span className="tech-rating-count">({t.ratingCount})</span></>
                          : <span className="tech-new">New</span>}
                      </div>
                    </div>
                    <div className="tech-meta">
                      <div className="tech-meta-row">
                        <span className="tech-meta-label">Services</span>
                        <div className="chip-row">
                          {t.skills.map(s => <span className="chip" key={s}>{categoryName(s)}</span>)}
                        </div>
                      </div>
                      <div className="tech-meta-row">
                        <span className="tech-meta-label">Areas</span>
                        <div className="chip-row">
                          {t.serviceAreas.map(a => <span className="chip chip--ghost" key={a}>{a}</span>)}
                        </div>
                      </div>
                    </div>
                    <button className="tech-book tech-book--active" onClick={() => openBooking(t)}>
                      Book
                    </button>
                  </article>
                ))}
              </div>
            </>
          )}

        </section>

      </main>


      {/* BOOKING MODAL */}
      {bookTech && (
        <div className="modal-overlay" onClick={closeBooking}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-head">
              <h2>Book Technician #{bookTech.id}</h2>
              <button className="modal-close" onClick={closeBooking}>✕</button>
            </div>

            {bookResult?.ok ? (
              <div className="modal-body">
                <div className="finder-note" style={{ color: 'var(--success)' }}>
                  ✓ {bookResult.message}
                </div>
                <div className="modal-actions">
                  <button className="finder-submit" onClick={onBookings}>Go to My Bookings</button>
                  <button className="btn-ghost" onClick={closeBooking}>Keep searching</button>
                </div>
              </div>
            ) : (
              <div className="modal-body">
                <div className="finder-field">
                  <label>Service</label>
                  <select value={bookSel.serviceId}
                          onChange={e => setBookSel({ ...bookSel, serviceId: e.target.value })}>
                    {bookServices.length === 0 && <option value="">No services in this category</option>}
                    {bookServices.map(s => (
                      <option key={s.id} value={s.id}>{s.name} — ₹{s.price}</option>
                    ))}
                  </select>
                </div>

                <div className="finder-field">
                  <label>Available slot</label>
                  <select value={bookSel.slotId}
                          onChange={e => setBookSel({ ...bookSel, slotId: e.target.value })}>
                    {bookSlots.length === 0 && <option value="">No open slots</option>}
                    {bookSlots.map(s => (
                      <option key={s.id} value={s.id}>{fmtSlot(s)}</option>
                    ))}
                  </select>
                </div>

                {bookResult && !bookResult.ok && (
                  <div className="finder-error">{bookResult.message}</div>
                )}

                <div className="modal-actions">
                  <button className="finder-submit"
                          disabled={bookBusy || !bookSel.serviceId || !bookSel.slotId}
                          onClick={confirmBooking}>
                    {bookBusy ? 'Booking…' : 'Confirm booking'}
                  </button>
                  <button className="btn-ghost" onClick={closeBooking}>Cancel</button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

    </div>
  )
}

export default FindTechnician
