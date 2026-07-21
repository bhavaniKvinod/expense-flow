import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import client, { errorMessage } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import { money, dateTime } from '../util/format'

export default function MyReports() {
  const navigate = useNavigate()
  const [reports, setReports] = useState(null)
  const [error, setError] = useState(null)
  const [creating, setCreating] = useState(false)

  function load() {
    client
      .get('/reports')
      .then((res) => setReports(res.data))
      .catch((err) => setError(errorMessage(err)))
  }

  useEffect(load, [])

  async function createReport() {
    setCreating(true)
    setError(null)
    try {
      const { data } = await client.post('/reports', { title: 'New expense report', purpose: '' })
      navigate(`/reports/${data.id}/edit`)
    } catch (err) {
      setError(errorMessage(err))
      setCreating(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>My Reports</h1>
          <div className="subtitle">Draft, submit and track your expense reports.</div>
        </div>
        <button className="btn-primary" onClick={createReport} disabled={creating}>
          {creating ? 'Creating…' : '+ New report'}
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        {!reports ? (
          <div className="center-note">Loading…</div>
        ) : reports.length === 0 ? (
          <div className="table-empty">No reports yet. Create your first one.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Title</th>
                <th>Status</th>
                <th>Items</th>
                <th className="right">Total (USD)</th>
                <th>Submitted</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {reports.map((r) => (
                <tr key={r.id}>
                  <td><Link to={`/reports/${r.id}`}>{r.title}</Link></td>
                  <td><StatusBadge status={r.status} /></td>
                  <td>{r.lineItemCount}</td>
                  <td className="right">{money(r.totalAmount)}</td>
                  <td className="nowrap">{dateTime(r.submittedAt)}</td>
                  <td className="right">
                    {(r.status === 'DRAFT' || r.status === 'REJECTED') ? (
                      <Link to={`/reports/${r.id}/edit`}>Edit</Link>
                    ) : (
                      <Link to={`/reports/${r.id}`}>View</Link>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
