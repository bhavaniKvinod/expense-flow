export function money(amount, currency = 'USD') {
  const n = Number(amount ?? 0)
  return n.toLocaleString(undefined, { style: 'currency', currency })
}

export function dateOnly(value) {
  if (!value) return '—'
  return value // LocalDate serialises as YYYY-MM-DD
}

export function dateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString()
}

export const STATUS_LABELS = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  REIMBURSED: 'Reimbursed',
  CANCELLED: 'Cancelled',
}

export const CATEGORIES = ['TRAVEL', 'MEALS', 'SOFTWARE', 'TRAINING', 'OTHER']

export const ROLES = ['EMPLOYEE', 'MANAGER', 'ADMIN']

export const CURRENCIES = ['USD', 'INR', 'EUR']
