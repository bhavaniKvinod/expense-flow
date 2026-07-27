/** Simple prev/next pager for endpoints returning the backend's PageResponse shape. */
export default function Pagination({ pageInfo, onPageChange }) {
  if (!pageInfo || pageInfo.totalPages <= 1) return null

  const { page, totalPages } = pageInfo

  return (
    <div className="pagination">
      <button className="btn" onClick={() => onPageChange(page - 1)} disabled={page <= 0}>
        ← Prev
      </button>
      <span className="pagination-status">
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn" onClick={() => onPageChange(page + 1)} disabled={page + 1 >= totalPages}>
        Next →
      </button>
    </div>
  )
}
