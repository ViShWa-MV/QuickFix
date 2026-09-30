function Home({
  user,
  onHome,
  onProfile,
  onLogout
}) {

  return (

    <div className="dashboard-page">


      {/* ===================================================
          HEADER
      =================================================== */}

      <header className="dashboard-header">


        {/* LOGO */}

        <div
          className="dashboard-logo"
          onClick={onHome}
          style={{
            cursor: 'pointer'
          }}
        >
          QuickFix
        </div>


        {/* NAVIGATION */}

        <div className="dashboard-navigation">


          <button
            className="profile-button"
            onClick={onHome}
          >
            Home
          </button>


          <button
            className="profile-button"
          >
            Services
          </button>


          <button
            className="profile-button"
          >
            Bookings
          </button>


        </div>


        {/* USER */}

        <div className="dashboard-user">


          <button
            className="profile-button"
            onClick={onProfile}
          >
            👤 {user?.name || user?.email}
          </button>


          <button
            className="profile-button"
            onClick={onLogout}
          >
            Logout
          </button>


        </div>


      </header>


      {/* ===================================================
          MAIN CONTENT
      =================================================== */}

      <main className="dashboard-content">


        {/* =================================================
            WELCOME
        ================================================= */}

        <section className="dashboard-welcome">


          <h1>
            Welcome to QuickFix 👋
          </h1>


          <p>
            What do you need help with today?
          </p>


        </section>


        {/* =================================================
            POPULAR SERVICES
        ================================================= */}

        <section className="service-section">


          <h2>
            Popular Services
          </h2>


          <div className="service-grid">


            {/* AC REPAIR */}

            <div className="service-card">

              <div className="service-icon">
                ❄️
              </div>

              <h3>
                AC Repair
              </h3>

              <p>
                Repair and maintenance
              </p>

            </div>


            {/* PLUMBING */}

            <div className="service-card">

              <div className="service-icon">
                🚿
              </div>

              <h3>
                Plumbing
              </h3>

              <p>
                Fix leaks and plumbing issues
              </p>

            </div>


            {/* ELECTRICAL */}

            <div className="service-card">

              <div className="service-icon">
                ⚡
              </div>

              <h3>
                Electrical
              </h3>

              <p>
                Electrical repair and installation
              </p>

            </div>


            {/* CLEANING */}

            <div className="service-card">

              <div className="service-icon">
                🧹
              </div>

              <h3>
                Cleaning
              </h3>

              <p>
                Professional home cleaning
              </p>

            </div>


          </div>


        </section>


        {/* =================================================
            QUICK ACTIONS
        ================================================= */}

        <section className="quick-actions">


          <h2>
            Quick Actions
          </h2>


          <div className="action-grid">


            <button
              className="action-card"
            >

              📅

              <span>
                My Bookings
              </span>

            </button>


            <button
              className="action-card"
              onClick={onProfile}
            >

              📍

              <span>
                My Addresses
              </span>

            </button>


            <button
              className="action-card"
              onClick={onProfile}
            >

              👤

              <span>
                My Profile
              </span>

            </button>


          </div>


        </section>


      </main>


    </div>

  )
}


export default Home