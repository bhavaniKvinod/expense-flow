import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client, { errorMessage } from '../api/client'
import { money, dateTime } from '../util/format'

export default function ReimbursementQueue() {
  const [reports, setReports] = useState(null)
  const [error, setError] = useState(null)
  const [busyId, setBusyId] = useState(null)

  function load() {
    client
      .get('/reimbursements')
      .then((res) => setReports(res.data))
      .catch((err) => setError(errorMessage(err)))
  }

  useEffect(load, [])

  async function reimburse(id) {
    const ref = window.prompt('Payment reference (optional):', '')
    if (ref == null) return
    setBusyId(id)
    setError(null)
    try {
      await client.post(`/reports/${id}/reimburse`, { paymentReference: ref })
      load()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Reimbursements</h1>
          <div className="subtitle">Approved reports awaiting payment (mock).</div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        {!reports ? (
          <div className="center-note">Loading…</div>
        ) : reports.length === 0 ? (
          <div className="table-empty">Nothing awaiting reimbursement.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Employee</th>
                <th>Title</th>
                <th className="right">Total (USD)</th>
                <th>Submitted</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {reports.map((r) => (
                <tr key={r.id}>
                  <td>{r.employeeName}</td>
                  <td><Link to={`/reports/${r.id}`}>{r.title}</Link></td>
                  <td className="right">{money(r.totalAmount)}</td>
                  <td className="nowrap">{dateTime(r.submittedAt)}</td>
                  <td className="right">
                    <button className="btn-primary" onClick={() => reimburse(r.id)} disabled={busyId === r.id}>
                      {busyId === r.id ? 'Processing…' : 'Mark reimbursed'}
                    </button>
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
