import { useState } from 'react'


function Onboarding() {

  // =========================================================
  // STATE
  // =========================================================

  const [role, setRole] =
    useState(null)


  // =========================================================
  // ROLE SELECTION
  // =========================================================

  const handleRoleSelection = (
    selectedRole
  ) => {

    setRole(selectedRole)
  }


  // =========================================================
  // SUBMIT
  // =========================================================

  const handleContinue = async () => {

    if (!role) {

      alert(
        'Please select a role'
      )

      return
    }


    try {

      const response =
        await fetch(
          'http://localhost:8081/api/users/onboarding',
          {
            method: 'POST',

            headers: {
              'Content-Type':
                'application/json'
            },

            /*
             * Spring Security OAuth
             * session cookie.
             */

            credentials: 'include',

            body: JSON.stringify({
              role: role
            })
          }
        )


      if (!response.ok) {

        throw new Error(
          `Onboarding failed: ${response.status}`
        )
      }


      // =====================================================
      // RESPONSE
      // =====================================================

      const data =
        await response.json()


      console.log(
        'Onboarding successful:',
        data
      )


      console.log(
        'JWT:',
        data.token
      )


      console.log(
        'User ID:',
        data.userId
      )


      console.log(
        'Email:',
        data.email
      )


      console.log(
        'Role:',
        data.role
      )


      // =====================================================
      // STORE AUTH DATA
      // =====================================================

      localStorage.setItem(
        'token',
        data.token
      )

      localStorage.setItem(
        'userId',
        data.userId
      )

      localStorage.setItem(
        'email',
        data.email
      )

      localStorage.setItem(
        'role',
        data.role
      )


      // =====================================================
      // GO TO HOME
      // =====================================================

      window.location.href =
        '/home'

    } catch (error) {

      console.error(
        'Onboarding error:',
        error
      )


      alert(
        'Something went wrong. Check the browser console.'
      )
    }
  }


  // =========================================================
  // UI
  // =========================================================

  return (

    <div className="onboarding-page">

      <div className="onboarding-card">


        <h1>
          Welcome to QuickFix 👋
        </h1>


        <p className="onboarding-subtitle">
          How do you want to use QuickFix?
        </p>


        {/* =================================================
            ROLE OPTIONS
        ================================================= */}


        <div className="role-options">


          {/* CUSTOMER */}

          <button

            className={
              role === 'CUSTOMER'
                ? 'role-card selected'
                : 'role-card'
            }

            onClick={() =>
              handleRoleSelection(
                'CUSTOMER'
              )
            }

          >

            <div className="role-icon">
              🏠
            </div>


            <div>

              <h2>
                I'm a Customer
              </h2>

              <p>
                Book reliable home services
              </p>

            </div>

          </button>


          {/* TECHNICIAN */}

          <button

            className={
              role === 'TECHNICIAN'
                ? 'role-card selected'
                : 'role-card'
            }

            onClick={() =>
              handleRoleSelection(
                'TECHNICIAN'
              )
            }

          >

            <div className="role-icon">
              🔧
            </div>


            <div>

              <h2>
                I'm a Technician
              </h2>

              <p>
                Provide services to customers
              </p>

            </div>

          </button>


        </div>


        {/* =================================================
            CONTINUE
        ================================================= */}

        <button
          className="continue-button"
          onClick={handleContinue}
        >
          Continue
        </button>


      </div>

    </div>

  )
}


export default Onboarding