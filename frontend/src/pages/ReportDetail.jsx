import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import client, { errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import StatusBadge from '../components/StatusBadge'
import AuditTimeline from '../components/AuditTimeline'
import { money, dateTime } from '../util/format'

export default function ReportDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [report, setReport] = useState(null)
  const [audit, setAudit] = useState([])
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  function load() {
    Promise.all([client.get(`/reports/${id}`), client.get(`/reports/${id}/audit`)])
      .then(([r, a]) => {
        setReport(r.data)
        setAudit(a.data)
      })
      .catch((err) => setError(errorMessage(err)))
  }

  useEffect(load, [id])

  if (error && !report) return <div className="alert alert-error">{error}</div>
  if (!report) return <div className="center-note">Loading…</div>

  const isOwner = report.employeeId === user.id
  const canDecide =
    (user.role === 'MANAGER' || user.role === 'ADMIN') && report.status === 'SUBMITTED'
  const canReimburse = user.role === 'ADMIN' && report.status === 'APPROVED'

  async function act(fn) {
    setBusy(true)
    setError(null)
    try {
      await fn()
      load()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  function approve() {
    act(() => client.post(`/reports/${id}/approve`))
  }

  function reject() {
    const reason = window.prompt('Reason for rejection:')
    if (reason == null || reason.trim() === '') return
    act(() => client.post(`/reports/${id}/reject`, { reason }))
  }

  function reimburse() {
    const ref = window.prompt('Payment reference (optional):', '')
    if (ref == null) return
    act(() => client.post(`/reports/${id}/reimburse`, { paymentReference: ref }))
  }

  function cancel() {
    if (!window.confirm('Withdraw this report?')) return
    act(() => client.post(`/reports/${id}/cancel`))
  }

  async function remove() {
    if (!window.confirm('Delete this draft permanently?')) return
    setBusy(true)
    try {
      await client.delete(`/reports/${id}`)
      navigate('/reports')
    } catch (err) {
      setError(errorMessage(err))
      setBusy(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>{report.title}</h1>
          <div className="subtitle">
            <StatusBadge status={report.status} /> &nbsp; by {report.employeeName}
          </div>
        </div>
        <Link className="btn" to="/reports">← Back</Link>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {report.status === 'REJECTED' && report.rejectionReason && (
        <div className="alert alert-error"><strong>Rejected:</strong> {report.rejectionReason}</div>
      )}

      <div className="card">
        <div className="grid cols-2">
          <div>
            <div className="muted">Business purpose</div>
            <div>{report.purpose || '—'}</div>
          </div>
          <div>
            <div className="muted">Total (USD equivalent)</div>
            <div style={{ fontSize: 22, fontWeight: 700 }}>{money(report.totalAmount)}</div>
          </div>
          <div>
            <div className="muted">Submitted</div>
            <div>{dateTime(report.submittedAt)}</div>
          </div>
          <div>
            <div className="muted">Decision</div>
            <div>{report.decidedByName ? `${report.decidedByName} · ${dateTime(report.decidedAt)}` : '—'}</div>
          </div>
        </div>
      </div>

      <div className="card">
        <h2>Line items</h2>
        {report.lineItems.length === 0 ? (
          <div className="table-empty">No line items.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Category</th>
                <th>Merchant</th>
                <th>Notes</th>
                <th className="right">Amount</th>
                <th>Receipt</th>
              </tr>
            </thead>
            <tbody>
              {report.lineItems.map((li) => (
                <tr key={li.id}>
                  <td className="nowrap">{li.date}</td>
                  <td>{li.category}</td>
                  <td>{li.merchant}</td>
                  <td className="muted">{li.notes || '—'}</td>
                  <td className="right">{money(li.amount, li.currency)}</td>
                  <td>{li.receipt ? `📎 ${li.receipt.fileName}` : <span className="muted">—</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h2>Actions</h2>
        <div className="btn-row">
          {isOwner && (report.status === 'DRAFT' || report.status === 'REJECTED') && (
            <Link className="btn btn-primary" to={`/reports/${id}/edit`}>Edit / submit</Link>
          )}
          {isOwner && report.status === 'SUBMITTED' && (
            <button className="btn" onClick={cancel} disabled={busy}>Withdraw</button>
          )}
          {isOwner && report.status === 'DRAFT' && (
            <button className="btn-danger" onClick={remove} disabled={busy}>Delete draft</button>
          )}
          {canDecide && (
            <>
              <button className="btn-success" onClick={approve} disabled={busy}>Approve</button>
              <button className="btn-danger" onClick={reject} disabled={busy}>Reject</button>
            </>
          )}
          {canReimburse && (
            <button className="btn-primary" onClick={reimburse} disabled={busy}>Mark reimbursed</button>
          )}
          {!isOwner && !canDecide && !canReimburse && (
            <span className="muted">No actions available for this report.</span>
          )}
        </div>
      </div>

      <div className="card">
        <h2>Activity</h2>
        <AuditTimeline events={audit} />
      </div>
    </div>
  )
}
