import { Routes, Route, Navigate } from 'react-router-dom'
import { RequireAuth, RequireRole } from './auth/guards'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import MyReports from './pages/MyReports'
import ReportEditor from './pages/ReportEditor'
import ReportDetail from './pages/ReportDetail'
import ApprovalsQueue from './pages/ApprovalsQueue'
import ReimbursementQueue from './pages/ReimbursementQueue'
import UserManagement from './pages/UserManagement'
import PolicySettings from './pages/PolicySettings'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        element={
          <RequireAuth>
            <Layout />
          </RequireAuth>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/reports" element={<MyReports />} />
        <Route path="/reports/:id" element={<ReportDetail />} />
        <Route path="/reports/:id/edit" element={<ReportEditor />} />
        <Route
          path="/approvals"
          element={
            <RequireRole roles={['MANAGER', 'ADMIN']}>
              <ApprovalsQueue />
            </RequireRole>
          }
        />
        <Route
          path="/reimbursements"
          element={
            <RequireRole roles={['ADMIN']}>
              <ReimbursementQueue />
            </RequireRole>
          }
        />
        <Route
          path="/users"
          element={
            <RequireRole roles={['ADMIN']}>
              <UserManagement />
            </RequireRole>
          }
        />
        <Route
          path="/policy"
          element={
            <RequireRole roles={['ADMIN']}>
              <PolicySettings />
            </RequireRole>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
