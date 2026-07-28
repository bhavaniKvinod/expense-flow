import { render, screen } from '@testing-library/react'
import StatusBadge from './StatusBadge'

describe('StatusBadge', () => {
  it('renders the human-readable label for a known status', () => {
    render(<StatusBadge status="SUBMITTED" />)
    expect(screen.getByText('Submitted')).toBeInTheDocument()
  })

  it('applies the status as a CSS class', () => {
    render(<StatusBadge status="APPROVED" />)
    expect(screen.getByText('Approved')).toHaveClass('badge', 'APPROVED')
  })

  it('falls back to the raw status when unknown', () => {
    render(<StatusBadge status="UNKNOWN" />)
    expect(screen.getByText('UNKNOWN')).toBeInTheDocument()
  })
})
