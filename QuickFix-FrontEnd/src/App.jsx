import { useEffect, useState } from 'react'

import Login from './pages/Login'
import Onboarding from './pages/Onboarding'
import Home from './pages/Home'
import Profile from './pages/Profile'
import FindTechnician from './pages/FindTechnician'
import Bookings from './pages/Bookings'
import TechnicianDashboard from './pages/TechnicianDashboard'

import API_BASE_URL from './api'

import './App.css'


function App() {

  // =========================================================
  // STATE
  // =========================================================

  const [user, setUser] = useState(null)

  const [loading, setLoading] = useState(false)

  const [error, setError] = useState(null)


  // =========================================================
  // CURRENT PATH
  // =========================================================

  const pathname =
    window.location.pathname


  // =========================================================
  // LOAD CURRENT USER
  // =========================================================

  useEffect(() => {

    /*
     * We only need to load the user automatically
     * on Home and Profile pages.
     */

    if (
      pathname !== '/home' &&
      pathname !== '/profile' &&
      pathname !== '/find' &&
      pathname !== '/bookings'
    ) {
      return
    }


    // =======================================================
    // CHECK JWT FROM URL
    // =======================================================

    const params =
      new URLSearchParams(
        window.location.search
      )


    const tokenFromUrl =
      params.get('token')


    console.log(
      'Token from URL:',
      tokenFromUrl
    )


    // =======================================================
    // STORE JWT
    // =======================================================

    if (tokenFromUrl) {

      localStorage.setItem(
        'token',
        tokenFromUrl
      )


      console.log(
        'JWT stored in localStorage'
      )


      /*
       * Remove the token from the URL.
       *
       * Example:
       *
       * /home?token=abc123
       *
       * becomes:
       *
       * /home
       */

      window.history.replaceState(
        {},
        document.title,
        pathname
      )
    }


    // =======================================================
    // GET JWT
    // =======================================================

    const token =
      localStorage.getItem('token')


    console.log(
      'JWT from localStorage:',
      token
    )


    // =======================================================
    // NO TOKEN
    // =======================================================

    if (!token) {

      setError(
        'You are not authenticated.'
      )

      return
    }


    // =======================================================
    // LOAD USER
    // =======================================================

    const loadUser = async () => {

      try {

        setLoading(true)

        setError(null)


        const response =
          await fetch(
            `${API_BASE_URL}/api/users/me`,
            {
              method: 'GET',

              headers: {
                'Authorization':
                  `Bearer ${token}`
              }
            }
          )


        console.log(
          'Profile API status:',
          response.status
        )


        // ===================================================
        // JWT INVALID
        // ===================================================

        if (response.status === 401) {

          console.error(
            'JWT rejected by backend'
          )


          localStorage.removeItem(
            'token'
          )

          localStorage.removeItem(
            'userId'
          )

          localStorage.removeItem(
            'email'
          )

          localStorage.removeItem(
            'role'
          )


          setError(
            'Your authentication token is invalid or expired.'
          )

          return
        }


        // ===================================================
        // OTHER ERROR
        // ===================================================

        if (!response.ok) {

          throw new Error(
            `Request failed: ${response.status}`
          )
        }


        // ===================================================
        // READ USER
        // ===================================================

        const data =
          await response.json()


        console.log(
          'Profile response:',
          data
        )


        setUser(data)

      } catch (error) {

        console.error(
          'Profile loading error:',
          error
        )


        setError(
          'Unable to load your profile.'
        )

      } finally {

        setLoading(false)
      }
    }


    loadUser()

  }, [pathname])


  // =========================================================
  // LOGOUT
  // =========================================================

  const handleLogout = () => {

    localStorage.removeItem(
      'token'
    )

    localStorage.removeItem(
      'userId'
    )

    localStorage.removeItem(
      'email'
    )

    localStorage.removeItem(
      'role'
    )


    window.location.href = '/'
  }


  // =========================================================
  // NAVIGATION
  // =========================================================

  const goToHome = () => {

    window.location.href =
      '/home'
  }


  const goToProfile = () => {

    window.location.href =
      '/profile'
  }


  const goToFind = () => {

    window.location.href =
      '/find'
  }


  const goToBookings = () => {

    window.location.href =
      '/bookings'
  }


  // =========================================================
  // LOGIN PAGE
  // =========================================================

  if (pathname === '/') {

    return (
      <Login />
    )
  }


  // =========================================================
  // ONBOARDING PAGE
  // =========================================================

  if (pathname === '/onboarding') {

    return (
      <Onboarding />
    )
  }


  // =========================================================
  // AUTHENTICATED APP PAGES — loading / error / role routing
  // =========================================================

  const authenticatedPaths = ['/home', '/profile', '/find', '/bookings']

  if (authenticatedPaths.includes(pathname)) {

    if (loading) {
      return (
        <div className="dashboard-page">
          <div className="dashboard-loading">
            <h2>Loading QuickFix...</h2>
          </div>
        </div>
      )
    }

    if (error) {
      return (
        <div className="dashboard-page">
          <div className="dashboard-error">
            <h2>Something went wrong</h2>
            <p>{error}</p>
            <button onClick={() => (window.location.href = '/')}>
              Back to Login
            </button>
          </div>
        </div>
      )
    }

    // Technicians get the technician dashboard for the app pages;
    // the profile page stays the shared account screen.
    if (user?.role === 'TECHNICIAN' && pathname !== '/profile') {
      return (
        <TechnicianDashboard
          user={user}
          onProfile={goToProfile}
          onLogout={handleLogout}
        />
      )
    }
  }


  // =========================================================
  // FIND A TECHNICIAN PAGE
  // =========================================================

  if (pathname === '/find') {

    return (

      <FindTechnician
        user={user}
        onHome={goToHome}
        onProfile={goToProfile}
        onLogout={handleLogout}
        onBookings={goToBookings}
      />

    )
  }


  // =========================================================
  // MY BOOKINGS PAGE
  // =========================================================

  if (pathname === '/bookings') {

    return (

      <Bookings
        user={user}
        onHome={goToHome}
        onProfile={goToProfile}
        onLogout={handleLogout}
        onFind={goToFind}
      />

    )
  }


  // =========================================================
  // HOME PAGE
  // =========================================================

  if (pathname === '/home') {

    return (

      <Home
        user={user}
        onHome={goToHome}
        onProfile={goToProfile}
        onLogout={handleLogout}
        onFind={goToFind}
        onBookings={goToBookings}
      />

    )
  }


  // =========================================================
  // PROFILE PAGE
  // =========================================================

  if (pathname === '/profile') {

    return (

      <Profile
        user={user}
        setUser={setUser}
        onHome={goToHome}
        onProfile={goToProfile}
        onLogout={handleLogout}
      />

    )
  }


  // =========================================================
  // UNKNOWN URL
  // =========================================================

  return (

    <Login />

  )
}


export default App