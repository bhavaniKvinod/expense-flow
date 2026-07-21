import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const isManager = user.role === 'MANAGER' || user.role === 'ADMIN'
  const isAdmin = user.role === 'ADMIN'

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">ExpenseFlow</div>
        <nav>
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/reports">My Reports</NavLink>
          {isManager && <NavLink to="/approvals">Approvals</NavLink>}
          {isAdmin && <NavLink to="/reimbursements">Reimbursements</NavLink>}
          {isAdmin && <NavLink to="/users">Users</NavLink>}
          {isAdmin && <NavLink to="/policy">Policy</NavLink>}
        </nav>
        <div className="spacer" />
        <div className="user-box">
          <div>{user.name}</div>
          <div className="role">{user.role.toLowerCase()}</div>
          <button className="btn-ghost" style={{ marginTop: 10, color: '#cbd5e1' }} onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </aside>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
