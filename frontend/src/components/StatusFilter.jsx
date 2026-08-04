import { STATUS_LABELS } from '../util/format'

// Lifecycle order, matching the ExpenseStatus enum. `ALL` is a sentinel meaning "no filter".
const OPTIONS = [
  'ALL',
  'DRAFT',
  'SUBMITTED',
  'APPROVED',
  'REJECTED',
  'REIMBURSED',
  'CANCELLED',
]

/**
 * Single-select status filter for the reports table. Renders a pill per status (plus "All"),
 * reusing the StatusBadge colour vocabulary (`badge ${status}`) for the active pill so a status
 * reads the same here as in the table. `value` is a status string or 'ALL'; `onChange` is called
 * with the newly-selected value. Disabled while a fetch is in flight to avoid overlapping requests.
 */
export default function StatusFilter({ value, onChange, disabled = false }) {
  return (
    <div
      className="status-filter"
      role="group"
      aria-label="Filter reports by status"
    >
      <span className="status-filter-label">Filter:</span>
      {OPTIONS.map((opt) => {
        const active = opt === value
        const label = opt === 'ALL' ? 'All' : STATUS_LABELS[opt]
        return (
          <button
            key={opt}
            type="button"
            className={`status-pill${active ? ` active badge ${opt}` : ''}`}
            aria-pressed={active}
            disabled={disabled}
            onClick={() => onChange(opt)}
          >
            {label}
          </button>
        )
      })}
    </div>
  )
}
