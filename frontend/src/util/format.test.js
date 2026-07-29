import { money, dateOnly, STATUS_LABELS } from './format'

describe('money', () => {
  it('formats USD amounts', () => {
    expect(money(1234.5, 'USD')).toBe('$1,234.50')
  })

  it('defaults to USD and treats null as zero', () => {
    expect(money(null)).toBe('$0.00')
  })

  it('formats EUR amounts', () => {
    expect(money(1234.5, 'EUR')).toBe('€1,234.50')
  })
})

describe('dateOnly', () => {
  it('returns an em dash for empty values', () => {
    expect(dateOnly('')).toBe('—')
  })

  it('passes through a YYYY-MM-DD string unchanged', () => {
    expect(dateOnly('2026-07-28')).toBe('2026-07-28')
  })
})

describe('STATUS_LABELS', () => {
  it('maps each status to a human label', () => {
    expect(STATUS_LABELS.SUBMITTED).toBe('Submitted')
    expect(STATUS_LABELS.REIMBURSED).toBe('Reimbursed')
  })
})
