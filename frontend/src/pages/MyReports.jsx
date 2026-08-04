import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import client, { errorMessage } from '../api/client'
import Pagination from '../components/Pagination'
import StatusBadge from '../components/StatusBadge'
import StatusFilter from '../components/StatusFilter'
import { money, dateTime, STATUS_LABELS } from '../util/format'

const VALID_STATUSES = Object.keys(STATUS_LABELS)

export default function MyReports() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [reports, setReports] = useState(null)
  const [pageInfo, setPageInfo] = useState(null)
  const [page, setPage] = useState(0)
  const [error, setError] = useState(null)
  const [creating, setCreating] = useState(false)
  const [loading, setLoading] = useState(false)

  // URL query string is the source of truth for the active filter, so the filter is
  // shareable/bookmarkable and survives Back/forward. An unknown value falls back to "All".
  const rawStatus = searchParams.get('status')
  const status =
    rawStatus && VALID_STATUSES.includes(rawStatus) ? rawStatus : 'ALL'

  function load() {
    setLoading(true)
    const params = { page }
    if (status !== 'ALL') params.status = status
    client
      .get('/reports', { params })
      .then((res) => {
        setReports(res.data.content)
        setPageInfo(res.data)
      })
      .catch((err) => setError(errorMessage(err)))
      .finally(() => setLoading(false))
  }

  useEffect(load, [page, status])

  function changeStatus(next) {
    setError(null)
    setPage(0) // a smaller filtered set may not have the current page
    setReports(null)
    const params = {}
    if (next !== 'ALL') params.status = next
    setSearchParams(params)
  }

  async function createReport() {
    setCreating(true)
    setError(null)
    try {
      const { data } = await client.post('/reports', {
        title: 'New expense report',
        purpose: '',
      })
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
          <div className="subtitle">
            Draft, submit and track your expense reports.
          </div>
        </div>
        <button
          className="btn-primary"
          onClick={createReport}
          disabled={creating}
        >
          {creating ? 'Creating…' : '+ New report'}
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <StatusFilter
          value={status}
          onChange={changeStatus}
          disabled={loading}
        />

        {!reports ? (
          <div className="center-note">Loading…</div>
        ) : reports.length === 0 ? (
          status === 'ALL' ? (
            <div className="table-empty">
              No reports yet. Create your first one.
            </div>
          ) : (
            <div className="table-empty">
              No <strong>{STATUS_LABELS[status]}</strong> reports.{' '}
              <button
                className="link-button"
                onClick={() => changeStatus('ALL')}
              >
                Show all reports
              </button>
            </div>
          )
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
                  <td>
                    <Link to={`/reports/${r.id}`}>{r.title}</Link>
                  </td>
                  <td>
                    <StatusBadge status={r.status} />
                  </td>
                  <td>{r.lineItemCount}</td>
                  <td className="right">{money(r.totalAmount)}</td>
                  <td className="nowrap">{dateTime(r.submittedAt)}</td>
                  <td className="right">
                    {r.status === 'DRAFT' || r.status === 'REJECTED' ? (
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
        <Pagination pageInfo={pageInfo} onPageChange={setPage} />
      </div>
    </div>
  )
}
