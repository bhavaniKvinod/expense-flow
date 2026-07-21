import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import client, { errorMessage } from '../api/client'
import StatusBadge from '../components/StatusBadge'
import PolicyViolationBanner from '../components/PolicyViolationBanner'
import { money, CATEGORIES, CURRENCIES } from '../util/format'

const emptyItem = { date: '', category: 'MEALS', amount: '', currency: 'USD', merchant: '', notes: '' }

export default function ReportEditor() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [report, setReport] = useState(null)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [check, setCheck] = useState(null)
  const [newItem, setNewItem] = useState(emptyItem)
  const [savingMeta, setSavingMeta] = useState(false)
  const fileInputs = useRef({})

  function load() {
    client
      .get(`/reports/${id}`)
      .then((res) => setReport(res.data))
      .catch((err) => setError(errorMessage(err)))
  }

  useEffect(load, [id])

  if (error && !report) return <div className="alert alert-error">{error}</div>
  if (!report) return <div className="center-note">Loading…</div>

  const editable = report.status === 'DRAFT' || report.status === 'REJECTED'

  async function saveMeta() {
    setSavingMeta(true)
    setError(null)
    try {
      await client.put(`/reports/${id}`, { title: report.title, purpose: report.purpose })
      setNotice('Report details saved.')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSavingMeta(false)
    }
  }

  async function addLineItem(e) {
    e.preventDefault()
    setError(null)
    setCheck(null)
    try {
      const { data } = await client.post(`/reports/${id}/line-items`, {
        ...newItem,
        amount: newItem.amount,
      })
      setReport(data)
      setNewItem(emptyItem)
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function deleteLineItem(lineItemId) {
    setError(null)
    setCheck(null)
    try {
      await client.delete(`/line-items/${lineItemId}`)
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function uploadReceipt(lineItemId, file) {
    if (!file) return
    setError(null)
    const form = new FormData()
    form.append('file', file)
    try {
      await client.post(`/line-items/${lineItemId}/receipt`, form)
      setNotice('Receipt attached.')
      load()
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function runCheck() {
    setError(null)
    setNotice(null)
    try {
      const { data } = await client.post(`/reports/${id}/check`)
      setCheck(data)
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  async function submit() {
    setError(null)
    setNotice(null)
    try {
      await client.post(`/reports/${id}/submit`)
      navigate(`/reports/${id}`)
    } catch (err) {
      // Policy violations come back as 422 with a violations array.
      if (err.response?.status === 422 && err.response.data?.violations) {
        setCheck({ passed: false, violations: err.response.data.violations })
      } else {
        setError(errorMessage(err))
      }
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Edit Report</h1>
          <div className="subtitle">
            <StatusBadge status={report.status} /> &nbsp; Total (USD equivalent): <strong>{money(report.totalAmount)}</strong>
          </div>
        </div>
        <Link className="btn" to={`/reports/${id}`}>View / done</Link>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {notice && <div className="alert alert-success">{notice}</div>}
      {report.status === 'REJECTED' && report.rejectionReason && (
        <div className="alert alert-error">
          <strong>Rejected:</strong> {report.rejectionReason} — fix the issues and resubmit.
        </div>
      )}
      {!editable && (
        <div className="alert alert-warning">
          This report is {report.status.toLowerCase()} and can no longer be edited.
        </div>
      )}

      <div className="card">
        <h2>Report details</h2>
        <label className="field">
          <span className="lbl">Title</span>
          <input
            value={report.title}
            disabled={!editable}
            onChange={(e) => setReport({ ...report, title: e.target.value })}
          />
        </label>
        <label className="field">
          <span className="lbl">Business purpose</span>
          <textarea
            rows={2}
            value={report.purpose || ''}
            disabled={!editable}
            onChange={(e) => setReport({ ...report, purpose: e.target.value })}
          />
        </label>
        {editable && (
          <button className="btn" onClick={saveMeta} disabled={savingMeta}>
            {savingMeta ? 'Saving…' : 'Save details'}
          </button>
        )}
      </div>

      <div className="card">
        <h2>Line items</h2>
        {report.lineItems.length === 0 ? (
          <div className="table-empty">No line items yet.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Category</th>
                <th>Merchant</th>
                <th className="right">Amount</th>
                <th>Receipt</th>
                {editable && <th></th>}
              </tr>
            </thead>
            <tbody>
              {report.lineItems.map((li) => (
                <tr key={li.id}>
                  <td className="nowrap">{li.date}</td>
                  <td>{li.category}</td>
                  <td>{li.merchant}</td>
                  <td className="right">{money(li.amount, li.currency)}</td>
                  <td>
                    {li.receipt ? (
                      <span className="muted">📎 {li.receipt.fileName}</span>
                    ) : editable ? (
                      <>
                        <button
                          className="btn-ghost"
                          onClick={() => fileInputs.current[li.id]?.click()}
                        >
                          Attach…
                        </button>
                        <input
                          type="file"
                          hidden
                          ref={(el) => (fileInputs.current[li.id] = el)}
                          onChange={(e) => uploadReceipt(li.id, e.target.files[0])}
                        />
                      </>
                    ) : (
                      <span className="muted">—</span>
                    )}
                  </td>
                  {editable && (
                    <td className="right">
                      <button className="btn-ghost" onClick={() => deleteLineItem(li.id)}>Remove</button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {editable && (
          <form onSubmit={addLineItem} style={{ marginTop: 16 }}>
            <div className="form-row">
              <label className="field">
                <span className="lbl">Date</span>
                <input
                  type="date"
                  required
                  value={newItem.date}
                  onChange={(e) => setNewItem({ ...newItem, date: e.target.value })}
                />
              </label>
              <label className="field">
                <span className="lbl">Category</span>
                <select
                  value={newItem.category}
                  onChange={(e) => setNewItem({ ...newItem, category: e.target.value })}
                >
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span className="lbl">Merchant</span>
                <input
                  required
                  value={newItem.merchant}
                  onChange={(e) => setNewItem({ ...newItem, merchant: e.target.value })}
                />
              </label>
              <label className="field">
                <span className="lbl">Amount</span>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  value={newItem.amount}
                  onChange={(e) => setNewItem({ ...newItem, amount: e.target.value })}
                />
              </label>
              <label className="field">
                <span className="lbl">Currency</span>
                <select
                  value={newItem.currency}
                  onChange={(e) => setNewItem({ ...newItem, currency: e.target.value })}
                >
                  {CURRENCIES.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </label>
            </div>
            <label className="field">
              <span className="lbl">Notes (optional)</span>
              <input
                value={newItem.notes}
                onChange={(e) => setNewItem({ ...newItem, notes: e.target.value })}
              />
            </label>
            <button className="btn">+ Add line item</button>
          </form>
        )}
      </div>

      {editable && (
        <div className="card">
          <h2>Policy &amp; submission</h2>
          <PolicyViolationBanner check={check} />
          <div className="btn-row">
            <button className="btn" onClick={runCheck}>Check policy</button>
            <button className="btn-primary" onClick={submit}>
              {report.status === 'REJECTED' ? 'Resubmit for approval' : 'Submit for approval'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
