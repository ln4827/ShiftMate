import { Routes, Route, Navigate } from 'react-router-dom'
import { useState, useEffect, createContext, useContext, useMemo } from 'react'
import PropTypes from 'prop-types'
import { authApi } from './api/client'
import NavBar from './components/NavBar'
import { ConfirmProvider } from './components/ConfirmDialog'
import LoginPage from './pages/LoginPage'
import EmployeesPage from './pages/EmployeesPage'
import SchedulePage from './pages/SchedulePage'
import MySchedulePage from './pages/MySchedulePage'
import TimeOffPage from './pages/TimeOffPage'
import SwapsPage from './pages/SwapsPage'
import ReportsPage from './pages/ReportsPage'
import AvailabilityPage from './pages/AvailabilityPage'
import DepartmentsPage from './pages/DepartmentsPage'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

function RequireAuth({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/login" replace />
}
RequireAuth.propTypes = { children: PropTypes.node.isRequired }

function RequireManager({ children }) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  return user.manager ? children : <Navigate to="/my-schedule" replace />
}
RequireManager.propTypes = { children: PropTypes.node.isRequired }

function AppLayout({ children }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <NavBar />
      <main style={{ flex: 1, background: '#F4F7FE' }}>{children}</main>
    </div>
  )
}
AppLayout.propTypes = { children: PropTypes.node.isRequired }

export default function App() {
  const [user, setUser] = useState(undefined)
  const [notifTick, setNotifTick] = useState(0)

  useEffect(() => {
    authApi.me()
      .then(setUser)
      .catch(() => setUser(null))
  }, [])

  const bumpNotifTick = () => setNotifTick(t => t + 1)
  const contextValue = useMemo(
    () => ({ user, setUser, notifTick, bumpNotifTick }),
    [user, setUser, notifTick]
  )

  if (user === undefined) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading…</div>
  }

  // Default route after login: managers → /schedule, employees → /my-schedule
  const defaultRoute = user?.manager ? '/schedule' : '/my-schedule'

  return (
    <AuthContext.Provider value={contextValue}>
      <ConfirmProvider>
      <Routes>
        <Route path="/login" element={user ? <Navigate to={defaultRoute} replace /> : <LoginPage />} />

        <Route path="/schedule" element={
          <RequireManager>
            <AppLayout><SchedulePage /></AppLayout>
          </RequireManager>
        } />

        <Route path="/my-schedule" element={
          <RequireAuth>
            <AppLayout><MySchedulePage /></AppLayout>
          </RequireAuth>
        } />

        <Route path="/employees" element={
          <RequireManager>
            <AppLayout><EmployeesPage /></AppLayout>
          </RequireManager>
        } />

        <Route path="/time-off" element={
          <RequireAuth>
            <AppLayout><TimeOffPage /></AppLayout>
          </RequireAuth>
        } />

        <Route path="/swaps" element={
          <RequireAuth>
            <AppLayout><SwapsPage /></AppLayout>
          </RequireAuth>
        } />

        <Route path="/departments" element={
          <RequireManager>
            <AppLayout><DepartmentsPage /></AppLayout>
          </RequireManager>
        } />

        <Route path="/reports" element={
          <RequireManager>
            <AppLayout><ReportsPage /></AppLayout>
          </RequireManager>
        } />

        <Route path="/availability" element={
          <RequireAuth>
            <AppLayout><AvailabilityPage /></AppLayout>
          </RequireAuth>
        } />

        <Route path="*" element={<Navigate to={user ? defaultRoute : '/login'} replace />} />
      </Routes>
      </ConfirmProvider>
    </AuthContext.Provider>
  )
}
