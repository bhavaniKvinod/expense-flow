import { useEffect, useState } from 'react'
import client, { errorMessage } from '../api/client'

export default function PolicySettings() {
  const [policy, setPolicy] = useState(null)
  const [error, setError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    client
      .get('/policy')
      .then((res) => setPolicy(res.data))
      .catch((err) => setError(errorMessage(err)))
  }, [])

  async function save(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setNotice(null)
    try {
      const { data } = await client.put('/policy', {
        receiptThreshold: policy.receiptThreshold,
        totalReportCap: policy.totalReportCap,
        maxExpenseAgeDays: Number(policy.maxExpenseAgeDays),
        usdToInrRate: policy.usdToInrRate,
        usdToEurRate: policy.usdToEurRate,
      })
      setPolicy(data)
      setNotice('Policy updated. New submissions are validated against these values.')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Policy Settings</h1>
          <div className="subtitle">Thresholds enforced automatically when employees submit.</div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {notice && <div className="alert alert-success">{notice}</div>}
      <p className="hint" style={{ marginTop: -8, marginBottom: 16 }}>
        Line items may be entered in USD, INR, or EUR. Non-USD amounts are converted using the exchange rates
        below before being checked against the receipt and total-cap thresholds (which are in USD).
      </p>

      {!policy ? (
        <div className="center-note">Loading…</div>
      ) : (
        <div className="card" style={{ maxWidth: 480 }}>
          <form onSubmit={save}>
            <label className="field">
              <span className="lbl">Receipt required above (USD)</span>
              <input
                type="number" step="0.01" min="0" required
                value={policy.receiptThreshold}
                onChange={(e) => setPolicy({ ...policy, receiptThreshold: e.target.value })}
              />
            </label>
            <label className="field">
              <span className="lbl">Total report cap (USD)</span>
              <input
                type="number" step="0.01" min="0" required
                value={policy.totalReportCap}
                onChange={(e) => setPolicy({ ...policy, totalReportCap: e.target.value })}
              />
            </label>
            <label className="field">
              <span className="lbl">Max expense age (days)</span>
              <input
                type="number" min="1" required
                value={policy.maxExpenseAgeDays}
                onChange={(e) => setPolicy({ ...policy, maxExpenseAgeDays: e.target.value })}
              />
            </label>
            <label className="field">
              <span className="lbl">Exchange rate (1 USD = ? INR)</span>
              <input
                type="number" step="0.0001" min="0" required
                value={policy.usdToInrRate}
                onChange={(e) => setPolicy({ ...policy, usdToInrRate: e.target.value })}
              />
            </label>
            <label className="field">
              <span className="lbl">Exchange rate (1 USD = ? EUR)</span>
              <input
                type="number" step="0.0001" min="0" required
                value={policy.usdToEurRate}
                onChange={(e) => setPolicy({ ...policy, usdToEurRate: e.target.value })}
              />
            </label>
            <button className="btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save policy'}</button>
          </form>
        </div>
      )}
    </div>
  )
}
