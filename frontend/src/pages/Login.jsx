import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../api/client'

export default function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('employee@expenseflow.test')
  const [password, setPassword] = useState('password123')
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  if (user) {
    navigate('/', { replace: true })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <div className="card login-card">
        <div className="brand">ExpenseFlow</div>
        <p className="subtitle" style={{ textAlign: 'center', marginBottom: 20 }}>
          Sign in to submit and track expenses
        </p>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <label className="field">
            <span className="lbl">Email</span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label className="field">
            <span className="lbl">Password</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>
          <button className="btn-primary" style={{ width: '100%' }} disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <div className="hint" style={{ marginTop: 16 }}>
          Demo accounts (password <code>password123</code>):
          <ul style={{ margin: '6px 0 0', paddingLeft: 18 }}>
            <li><code>employee@expenseflow.test</code> — Employee</li>
            <li><code>manager@expenseflow.test</code> — Manager</li>
            <li><code>admin@expenseflow.test</code> — Admin / Finance</li>
          </ul>
        </div>
      </div>
    </div>
  )
}
