import { dateTime } from '../util/format'

export default function AuditTimeline({ events }) {
  if (!events?.length) return <p className="muted">No activity yet.</p>
  return (
    <ul className="timeline">
      {events.map((e) => (
        <li key={e.id}>
          <div>
            <strong>{e.action}</strong> by {e.actorName}
          </div>
          {e.note && <div className="muted">{e.note}</div>}
          <div className="when">{dateTime(e.createdAt)}</div>
        </li>
      ))}
    </ul>
  )
}
