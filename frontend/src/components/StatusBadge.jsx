import { STATUS_LABELS } from '../util/format'

export default function StatusBadge({ status }) {
  return <span className={`badge ${status}`}>{STATUS_LABELS[status] || status}</span>
}
