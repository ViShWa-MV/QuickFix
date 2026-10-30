import API_BASE_URL from '../api'


function Login() {

  // =========================================================
  // GOOGLE LOGIN
  // =========================================================

  const handleGoogleLogin = () => {

    window.location.href =
      `${API_BASE_URL}/oauth2/authorization/google`
  }


  // =========================================================
  // UI
  // =========================================================

  return (

    <div className="login-page">

      <div className="login-card">

        <h1>
          QuickFix
        </h1>

        <p>
          Home services at your doorstep
        </p>


        <button
          onClick={handleGoogleLogin}
        >
          Continue with Google
        </button>

      </div>

    </div>

  )
}


export default Login