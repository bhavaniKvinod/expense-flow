import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client, { errorMessage } from '../api/client'
import Pagination from '../components/Pagination'
import StatusBadge from '../components/StatusBadge'
import { money, dateTime } from '../util/format'

export default function ApprovalsQueue() {
  const [reports, setReports] = useState(null)
  const [pageInfo, setPageInfo] = useState(null)
  const [page, setPage] = useState(0)
  const [error, setError] = useState(null)

  useEffect(() => {
    client
      .get('/approvals', { params: { page } })
      .then((res) => {
        setReports(res.data.content)
        setPageInfo(res.data)
      })
      .catch((err) => setError(errorMessage(err)))
  }, [page])

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Approvals</h1>
          <div className="subtitle">Reports from your team awaiting a decision.</div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        {!reports ? (
          <div className="center-note">Loading…</div>
        ) : reports.length === 0 ? (
          <div className="table-empty">Nothing awaiting approval. 🎉</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Employee</th>
                <th>Title</th>
                <th>Items</th>
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
                  <td>{r.lineItemCount}</td>
                  <td className="right">{money(r.totalAmount)}</td>
                  <td className="nowrap">{dateTime(r.submittedAt)}</td>
                  <td className="right"><Link className="btn" to={`/reports/${r.id}`}>Review</Link></td>
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
