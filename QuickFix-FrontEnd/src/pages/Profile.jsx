import { useEffect, useState } from 'react'

import API_BASE_URL from '../api'


function Profile({
  user,
  setUser,
  onHome,
  onProfile,
  onLogout
}) {

  // =========================================================
  // PROFILE STATE
  // =========================================================

  const [editName, setEditName] =
    useState('')

  const [editPhone, setEditPhone] =
    useState('')

  const [savingProfile, setSavingProfile] =
    useState(false)

  const [profileMessage, setProfileMessage] =
    useState(null)


  // =========================================================
  // ADDRESS STATE
  // =========================================================

  const [addresses, setAddresses] =
    useState([])

  const [showAddressForm, setShowAddressForm] =
    useState(false)

  const [savingAddress, setSavingAddress] =
    useState(false)

  const [addressMessage, setAddressMessage] =
    useState(null)

  const [addressForm, setAddressForm] =
    useState({
      label: '',
      addressLine: '',
      city: '',
      state: '',
      pincode: ''
    })


  // =========================================================
  // INITIALIZE PROFILE
  // =========================================================

  useEffect(() => {

    if (!user) {
      return
    }

    setEditName(
      user.name || ''
    )

    setEditPhone(
      user.phone || ''
    )

    loadAddresses()

  }, [user])


  // =========================================================
  // LOAD ADDRESSES
  // =========================================================

  const loadAddresses = async () => {

    const token =
      localStorage.getItem('token')


    if (!token) {

      setAddressMessage(
        'You are not authenticated.'
      )

      return
    }


    try {

      const response =
        await fetch(
          `${API_BASE_URL}/api/users/addresses`,
          {
            method: 'GET',

            headers: {
              'Authorization':
                `Bearer ${token}`
            }
          }
        )


      if (!response.ok) {

        throw new Error(
          `Failed to load addresses: ${response.status}`
        )

      }


      const data =
        await response.json()


      console.log(
        'Saved addresses:',
        data
      )


      setAddresses(data)


    } catch (error) {

      console.error(
        'Address loading error:',
        error
      )


      setAddressMessage(
        'Unable to load addresses.'
      )

    }
  }


  // =========================================================
  // UPDATE PROFILE
  // =========================================================

  const handleUpdateProfile = async () => {

    const token =
      localStorage.getItem('token')


    if (!token) {

      setProfileMessage(
        'You are not authenticated.'
      )

      return
    }


    try {

      setSavingProfile(true)

      setProfileMessage(null)


      const response =
        await fetch(
          `${API_BASE_URL}/api/users/me`,
          {
            method: 'PUT',

            headers: {

              'Content-Type':
                'application/json',

              'Authorization':
                `Bearer ${token}`

            },

            body: JSON.stringify({

              name: editName,

              phone: editPhone

            })
          }
        )


      if (!response.ok) {

        throw new Error(
          `Profile update failed: ${response.status}`
        )

      }


      const updatedUser =
        await response.json()


      console.log(
        'Updated profile:',
        updatedUser
      )


      setUser(
        updatedUser
      )


      setProfileMessage(
        'Profile updated successfully.'
      )


    } catch (error) {

      console.error(
        'Profile update error:',
        error
      )


      setProfileMessage(
        'Unable to update profile.'
      )


    } finally {

      setSavingProfile(false)

    }

  }


  // =========================================================
  // ADDRESS FORM CHANGE
  // =========================================================

  const handleAddressChange = (event) => {

    const {
      name,
      value
    } = event.target


    setAddressForm(
      previous => ({
        ...previous,
        [name]: value
      })
    )

  }


  // =========================================================
  // ADD ADDRESS
  // =========================================================

  const handleAddAddress = async () => {

    const token =
      localStorage.getItem('token')


    if (!token) {

      setAddressMessage(
        'You are not authenticated.'
      )

      return
    }


    // Basic validation

    if (
      !addressForm.addressLine ||
      !addressForm.city ||
      !addressForm.state ||
      !addressForm.pincode
    ) {

      setAddressMessage(
        'Please fill all required address fields.'
      )

      return
    }


    try {

      setSavingAddress(true)

      setAddressMessage(null)


      const response =
        await fetch(
          `${API_BASE_URL}/api/users/addresses`,
          {
            method: 'POST',

            headers: {

              'Content-Type':
                'application/json',

              'Authorization':
                `Bearer ${token}`

            },

            body: JSON.stringify(
              addressForm
            )
          }
        )


      if (!response.ok) {

        throw new Error(
          `Address creation failed: ${response.status}`
        )

      }


      const newAddress =
        await response.json()


      console.log(
        'New address:',
        newAddress
      )


      // Add newly created address
      // to the current list

      setAddresses(
        previous => [
          ...previous,
          newAddress
        ]
      )


      // Clear form

      setAddressForm({

        label: '',

        addressLine: '',

        city: '',

        state: '',

        pincode: ''

      })


      // Hide form

      setShowAddressForm(false)


      setAddressMessage(
        'Address added successfully.'
      )


    } catch (error) {

      console.error(
        'Add address error:',
        error
      )


      setAddressMessage(
        'Unable to add address.'
      )


    } finally {

      setSavingAddress(false)

    }

  }


  // =========================================================
  // UI
  // =========================================================

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
          PROFILE CONTENT
      =================================================== */}

      <main className="dashboard-content">


        {/* =================================================
            PROFILE HEADER
        ================================================= */}

        <section className="dashboard-welcome">


          <h1>
            My Profile
          </h1>


          <p>
            Manage your QuickFix account information.
          </p>


        </section>


        {/* =================================================
            PERSONAL INFORMATION
        ================================================= */}

        <section className="user-info">


          <h2>
            Personal Information
          </h2>


          {/* NAME */}

          <div className="profile-field">

            <label>
              Name
            </label>


            <input
              type="text"
              value={editName}
              onChange={(event) =>
                setEditName(
                  event.target.value
                )
              }
            />

          </div>


          {/* EMAIL */}

          <div className="profile-field">

            <label>
              Email
            </label>


            <input
              type="email"
              value={
                user?.email || ''
              }
              disabled
            />


            <small>
              Email is managed by Google.
            </small>

          </div>


          {/* PHONE */}

          <div className="profile-field">

            <label>
              Phone
            </label>


            <input
              type="text"
              value={editPhone}
              onChange={(event) =>
                setEditPhone(
                  event.target.value
                )
              }
            />

          </div>


          {/* ROLE */}

          <div className="profile-field">

            <label>
              Role
            </label>


            <input
              type="text"
              value={
                user?.role || ''
              }
              disabled
            />

          </div>


          {/* SAVE PROFILE */}

          <button
            className="continue-button"
            onClick={
              handleUpdateProfile
            }
            disabled={
              savingProfile
            }
          >

            {savingProfile
              ? 'Saving...'
              : 'Save Changes'}

          </button>


          {/* PROFILE MESSAGE */}

          {profileMessage && (

            <p>
              {profileMessage}
            </p>

          )}

        </section>


        {/* =================================================
            ADDRESSES
        ================================================= */}

        <section className="quick-actions">


          <h2>
            My Addresses
          </h2>


          <p>
            Manage your saved service addresses.
          </p>


          {/* =================================================
              ADD ADDRESS BUTTON
          ================================================= */}

          {!showAddressForm && (

            <button
              className="action-card"
              onClick={() => {

                setShowAddressForm(true)

                setAddressMessage(null)

              }}
            >

              📍

              <span>
                Add Address
              </span>

            </button>

          )}


          {/* =================================================
              ADDRESS FORM
          ================================================= */}

          {showAddressForm && (

            <div className="address-form">


              <h3>
                Add New Address
              </h3>


              {/* LABEL */}

              <div className="profile-field">

                <label>
                  Label
                </label>


                <input
                  type="text"
                  name="label"
                  placeholder="Home"
                  value={
                    addressForm.label
                  }
                  onChange={
                    handleAddressChange
                  }
                />

              </div>


              {/* ADDRESS */}

              <div className="profile-field">

                <label>
                  Address
                </label>


                <input
                  type="text"
                  name="addressLine"
                  placeholder="Street / House / Apartment"
                  value={
                    addressForm.addressLine
                  }
                  onChange={
                    handleAddressChange
                  }
                />

              </div>


              {/* CITY */}

              <div className="profile-field">

                <label>
                  City
                </label>


                <input
                  type="text"
                  name="city"
                  placeholder="Bangalore"
                  value={
                    addressForm.city
                  }
                  onChange={
                    handleAddressChange
                  }
                />

              </div>


              {/* STATE */}

              <div className="profile-field">

                <label>
                  State
                </label>


                <input
                  type="text"
                  name="state"
                  placeholder="Karnataka"
                  value={
                    addressForm.state
                  }
                  onChange={
                    handleAddressChange
                  }
                />

              </div>


              {/* PINCODE */}

              <div className="profile-field">

                <label>
                  Pincode
                </label>


                <input
                  type="text"
                  name="pincode"
                  placeholder="560001"
                  value={
                    addressForm.pincode
                  }
                  onChange={
                    handleAddressChange
                  }
                />

              </div>


              {/* SAVE ADDRESS */}

              <button
                className="continue-button"
                onClick={
                  handleAddAddress
                }
                disabled={
                  savingAddress
                }
              >

                {savingAddress
                  ? 'Saving...'
                  : 'Save Address'}

              </button>


              {/* CANCEL */}

              <button
                className="profile-button"
                onClick={() => {

                  setShowAddressForm(false)

                  setAddressMessage(null)

                }}
              >

                Cancel

              </button>


            </div>

          )}


          {/* =================================================
              ADDRESS MESSAGE
          ================================================= */}

          {addressMessage && (

            <p>
              {addressMessage}
            </p>

          )}


          {/* =================================================
              SAVED ADDRESSES
          ================================================= */}

          {addresses.length > 0 && (

            <div>

              <h3>
                Saved Addresses
              </h3>


              {addresses.map(
                address => (

                  <div
                    key={address.id}
                    className="action-card"
                  >

                    📍


                    <div>

                      <strong>
                        {address.label ||
                          'Address'}
                      </strong>


                      <p>
                        {address.addressLine}
                      </p>


                      <p>
                        {address.city},
                        {' '}
                        {address.state}
                        {' - '}
                        {address.pincode}
                      </p>

                    </div>

                  </div>

                )
              )}

            </div>

          )}


          {/* NO ADDRESSES */}

          {addresses.length === 0 &&
            !showAddressForm && (

              <p>
                No saved addresses yet.
              </p>

            )}

        </section>


      </main>


    </div>

  )
}


export default Profile