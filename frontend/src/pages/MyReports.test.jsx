import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import MyReports from './MyReports'
import client from '../api/client'

// Mock the axios client module. The component imports the default `client`
// (with .get/.post) and the named `errorMessage`; provide both.
jest.mock('../api/client', () => ({
  __esModule: true,
  default: { get: jest.fn(), post: jest.fn() },
  errorMessage: (err) =>
    err?.response?.data?.message || err?.message || 'Request failed',
}))

// react-router's useNavigate needs to resolve; spy on it to assert redirects.
const mockNavigate = jest.fn()
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}))

function renderPage() {
  return render(
    <MemoryRouter
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <MyReports />
    </MemoryRouter>,
  )
}

beforeEach(() => {
  jest.clearAllMocks()
})

describe('MyReports', () => {
  it('renders a row per report returned by the API', async () => {
    client.get.mockResolvedValue({
      data: {
        content: [
          {
            id: 1,
            title: 'Client dinner',
            status: 'SUBMITTED',
            lineItemCount: 3,
            totalAmount: 120.5,
            submittedAt: '2026-07-20T10:00:00Z',
          },
          {
            id: 2,
            title: 'Conference travel',
            status: 'DRAFT',
            lineItemCount: 1,
            totalAmount: 800,
            submittedAt: null,
          },
        ],
        page: 0,
        totalPages: 1,
      },
    })

    renderPage()

    expect(await screen.findByText('Client dinner')).toBeInTheDocument()
    expect(screen.getByText('Conference travel')).toBeInTheDocument()
    // Fetched from the reports endpoint with the initial page param.
    expect(client.get).toHaveBeenCalledWith('/reports', { params: { page: 0 } })
  })

  it('shows the empty state when there are no reports', async () => {
    client.get.mockResolvedValue({
      data: { content: [], page: 0, totalPages: 0 },
    })

    renderPage()

    expect(await screen.findByText(/No reports yet/i)).toBeInTheDocument()
  })

  it('refetches with the status param when a filter pill is clicked', async () => {
    client.get.mockResolvedValue({
      data: { content: [], page: 0, totalPages: 0 },
    })

    renderPage()
    await screen.findByText(/No reports yet/i)

    // Initial load carries no status filter.
    expect(client.get).toHaveBeenCalledWith('/reports', { params: { page: 0 } })

    await userEvent.click(screen.getByRole('button', { name: 'Rejected' }))

    await waitFor(() => {
      expect(client.get).toHaveBeenCalledWith('/reports', {
        params: { page: 0, status: 'REJECTED' },
      })
    })
  })

  it('shows a filter-specific empty state with a reset action', async () => {
    client.get.mockResolvedValue({
      data: { content: [], page: 0, totalPages: 0 },
    })

    renderPage()
    await screen.findByText(/No reports yet/i)

    await userEvent.click(screen.getByRole('button', { name: 'Cancelled' }))

    // Distinct message for "filter matched nothing" vs. "no reports at all".
    expect(await screen.findByText(/No/)).toBeInTheDocument()
    const reset = await screen.findByRole('button', {
      name: /Show all reports/i,
    })
    await userEvent.click(reset)

    await waitFor(() => {
      expect(client.get).toHaveBeenLastCalledWith('/reports', {
        params: { page: 0 },
      })
    })
  })

  it('surfaces an error message when the fetch fails', async () => {
    client.get.mockRejectedValue({
      response: { data: { message: 'Server exploded' } },
    })

    renderPage()

    expect(await screen.findByText('Server exploded')).toBeInTheDocument()
  })

  it('creates a report and navigates to its editor', async () => {
    client.get.mockResolvedValue({
      data: { content: [], page: 0, totalPages: 0 },
    })
    client.post.mockResolvedValue({ data: { id: 42 } })

    renderPage()
    await screen.findByText(/No reports yet/i)

    await userEvent.click(screen.getByRole('button', { name: /New report/i }))

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith('/reports', {
        title: 'New expense report',
        purpose: '',
      })
      expect(mockNavigate).toHaveBeenCalledWith('/reports/42/edit')
    })
  })
})
