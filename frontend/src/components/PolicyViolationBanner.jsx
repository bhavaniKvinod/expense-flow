// Shows the result of a policy check: either a pass confirmation or the list of violations.
export default function PolicyViolationBanner({ check }) {
  if (!check) return null
  if (check.passed) {
    return <div className="alert alert-success">All policy rules pass — this report is ready to submit.</div>
  }
  return (
    <div className="alert alert-warning">
      <strong>This report violates {check.violations.length} policy rule(s):</strong>
      <ul>
        {check.violations.map((v, i) => (
          <li key={i}>{v.message}</li>
        ))}
      </ul>
    </div>
  )
}
