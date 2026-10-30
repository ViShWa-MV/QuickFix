import { useEffect, useState } from 'react'

import CATALOG_BASE_URL from '../catalogApi'


/*
 * Find a Technician
 *
 * Calls catalog-service discovery search:
 *   GET /api/technicians/search?category&pincode&from&to
 *
 * Booking is not available yet (booking-service is a later step),
 * so the Book button is intentionally disabled.
 */
function FindTechnician({
  user,
  onHome,
  onProfile,
  onLogout
}) {

  // =========================================================
  // STATE
  // =========================================================

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


  // =========================================================
  // LOAD CATEGORIES (for the dropdown)
  // =========================================================

  useEffect(() => {

    const loadCategories = async () => {

      try {

        const response =
          await fetch(`${CATALOG_BASE_URL}/api/categories`)

        if (!response.ok) {
          return
        }

        const data = await response.json()

        setCategories(data)

        // Default the dropdown to the first category.
        if (data.length > 0) {
          setForm(f => ({
            ...f,
            category: String(data[0].id)
          }))
        }

      } catch {
        // A missing category list shouldn't break the page.
      }
    }

    loadCategories()

  }, [])


  // Map category id -> name, for readable skill chips.
  const categoryName = (id) => {
    const match = categories.find(c => c.id === id)
    return match ? match.name : `Category #${id}`
  }


  // =========================================================
  // SEARCH
  // =========================================================

  const handleChange = (field) => (event) => {
    setForm({
      ...form,
      [field]: event.target.value
    })
  }


  const handleSearch = async (event) => {

    event.preventDefault()

    setLoading(true)
    setError(null)
    setSearched(true)

    try {

      const from = `${form.date}T${form.from}:00`
      const to = `${form.date}T${form.to}:00`

      const url =
        `${CATALOG_BASE_URL}/api/technicians/search`
        + `?category=${encodeURIComponent(form.category)}`
        + `&pincode=${encodeURIComponent(form.pincode)}`
        + `&from=${encodeURIComponent(from)}`
        + `&to=${encodeURIComponent(to)}`

      const response = await fetch(url)

      if (!response.ok) {
        const body = await response.json().catch(() => null)
        throw new Error(
          body?.message || `Search failed (${response.status})`
        )
      }

      setResults(await response.json())

    } catch (err) {
      setError(err.message || 'Something went wrong')
      setResults([])
    } finally {
      setLoading(false)
    }
  }


  // =========================================================
  // RENDER
  // =========================================================

  return (

    <div className="finder-page">


      {/* HEADER (shared with the dashboard) */}

      <header className="dashboard-header">

        <div
          className="dashboard-logo"
          onClick={onHome}
          style={{ cursor: 'pointer' }}
        >
          QuickFix
        </div>

        <div className="dashboard-navigation">

          <button className="profile-button" onClick={onHome}>
            Home
          </button>

          <button className="profile-button profile-button--active">
            Find a Technician
          </button>

        </div>

        <div className="dashboard-user">

          <button className="profile-button" onClick={onProfile}>
            👤 {user?.name || user?.email || 'Guest'}
          </button>

          <button className="profile-button" onClick={onLogout}>
            Logout
          </button>

        </div>

      </header>


      <main className="finder-main">


        {/* HERO + SEARCH */}

        <section className="finder-hero">

          <h1>Find a Technician</h1>

          <p>
            Search verified technicians by service, area and time —
            ranked by rating and experience.
          </p>

        </section>


        <form className="finder-search" onSubmit={handleSearch}>

          <div className="finder-field">
            <label htmlFor="f-category">Service</label>
            <select
              id="f-category"
              value={form.category}
              onChange={handleChange('category')}
              required
            >
              {categories.length === 0 && (
                <option value="">No categories</option>
              )}
              {categories.map(c => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div className="finder-field">
            <label htmlFor="f-pincode">Pincode</label>
            <input
              id="f-pincode"
              type="text"
              inputMode="numeric"
              placeholder="e.g. 560001"
              value={form.pincode}
              onChange={handleChange('pincode')}
              required
            />
          </div>

          <div className="finder-field">
            <label htmlFor="f-date">Date</label>
            <input
              id="f-date"
              type="date"
              value={form.date}
              onChange={handleChange('date')}
              required
            />
          </div>

          <div className="finder-field finder-field--time">
            <label htmlFor="f-from">From</label>
            <input
              id="f-from"
              type="time"
              value={form.from}
              onChange={handleChange('from')}
              required
            />
          </div>

          <div className="finder-field finder-field--time">
            <label htmlFor="f-to">To</label>
            <input
              id="f-to"
              type="time"
              value={form.to}
              onChange={handleChange('to')}
              required
            />
          </div>

          <button
            type="submit"
            className="finder-submit"
            disabled={loading}
          >
            {loading ? 'Searching…' : 'Search'}
          </button>

        </form>


        {/* RESULTS */}

        <section className="finder-results">

          {loading && (
            <p className="finder-note">Searching for technicians…</p>
          )}

          {!loading && error && (
            <div className="finder-error">
              <strong>Couldn’t search.</strong> {error}
            </div>
          )}

          {!loading && !error && searched && results.length === 0 && (
            <div className="finder-empty">
              <div className="finder-empty-icon">🔍</div>
              <h3>No technicians available</h3>
              <p>
                No verified technician matches this service, area and
                time window. Try a different date or pincode.
              </p>
            </div>
          )}

          {!loading && !error && results.length > 0 && (
            <>
              <p className="finder-count">
                {results.length} technician
                {results.length > 1 ? 's' : ''} available
              </p>

              <div className="tech-grid">

                {results.map(t => (

                  <article className="tech-card" key={t.id}>

                    <div className="tech-top">

                      <div className="tech-avatar">
                        T{t.id}
                      </div>

                      <div className="tech-id">
                        <h3>Technician #{t.id}</h3>
                        <span className="tech-sub">
                          {t.yearsExperience} yrs experience
                        </span>
                      </div>

                      <div className="tech-rating">
                        {t.ratingCount > 0 ? (
                          <>
                            <span className="tech-stars">★</span>
                            {t.ratingAvg.toFixed(1)}
                            <span className="tech-rating-count">
                              ({t.ratingCount})
                            </span>
                          </>
                        ) : (
                          <span className="tech-new">New</span>
                        )}
                      </div>

                    </div>

                    <div className="tech-meta">

                      <div className="tech-meta-row">
                        <span className="tech-meta-label">Services</span>
                        <div className="chip-row">
                          {t.skills.map(s => (
                            <span className="chip" key={s}>
                              {categoryName(s)}
                            </span>
                          ))}
                        </div>
                      </div>

                      <div className="tech-meta-row">
                        <span className="tech-meta-label">Areas</span>
                        <div className="chip-row">
                          {t.serviceAreas.map(a => (
                            <span className="chip chip--ghost" key={a}>
                              {a}
                            </span>
                          ))}
                        </div>
                      </div>

                    </div>

                    <button
                      className="tech-book"
                      disabled
                      title="Booking is coming soon"
                    >
                      Book — coming soon
                    </button>

                  </article>

                ))}

              </div>
            </>
          )}

        </section>

      </main>

    </div>

  )
}


export default FindTechnician
