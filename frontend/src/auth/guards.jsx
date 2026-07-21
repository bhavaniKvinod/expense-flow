import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

export function RequireAuth({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="center-note">Loading…</div>
  if (!user) return <Navigate to="/login" replace />
  return children
}

export function RequireRole({ roles, children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="center-note">Loading…</div>
  if (!user) return <Navigate to="/login" replace />
  if (!roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}
