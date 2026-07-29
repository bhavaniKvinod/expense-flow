import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client, { errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'

function Stat({ value, label }) {
  return (
    <div className="stat">
      <div className="value">{value}</div>
      <div className="label">{label}</div>
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    client
      .get('/dashboard')
      .then((res) => setData(res.data))
      .catch((err) => setError(errorMessage(err)))
  }, [])

  const isManager = user.role === 'MANAGER' || user.role === 'ADMIN'
  const isAdmin = user.role === 'ADMIN'

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Welcome, {user.name.split(' ')[0]}</h1>
          <div className="subtitle">
            Here's where your expenses stand today.
          </div>
        </div>
        <Link className="btn btn-primary" to="/reports">
          New / view reports
        </Link>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {!data ? (
        <div className="center-note">Loading…</div>
      ) : (
        <>
          {isManager && (
            <div className="card">
              <h2>Needs your attention</h2>
              <div className="grid cols-2">
                <Stat
                  value={data.awaitingMyApproval}
                  label="Awaiting my approval"
                />
                {isAdmin && (
                  <Stat
                    value={data.awaitingReimbursement}
                    label="Awaiting reimbursement"
                  />
                )}
              </div>
              <div className="btn-row" style={{ marginTop: 16 }}>
                <Link className="btn" to="/approvals">
                  Go to approvals
                </Link>
                {isAdmin && (
                  <Link className="btn" to="/reimbursements">
                    Go to reimbursements
                  </Link>
                )}
              </div>
            </div>
          )}

          <div className="card">
            <h2>My reports</h2>
            <div className="grid cols-3">
              <Stat value={data.myDrafts} label="Drafts" />
              <Stat value={data.mySubmitted} label="Submitted" />
              <Stat value={data.myApproved} label="Approved" />
              <Stat value={data.myRejected} label="Rejected" />
              <Stat value={data.myReimbursed} label="Reimbursed" />
            </div>
          </div>
        </>
      )}
    </div>
  )
}
