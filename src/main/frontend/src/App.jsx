import { Routes, Route, Navigate } from 'react-router-dom'
import { useState, useEffect, createContext, useContext } from 'react'
import { authApi } from './api/client'
import LoginPage from './pages/LoginPage'
import EmployeesPage from './pages/EmployeesPage'

const AuthContext = createContext(null)
export const useAuth = () => useContext(AuthContext)

function RequireAuth({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/login" replace />
}

export default function App() {
  const [user, setUser] = useState(undefined)

  useEffect(() => {
    authApi.me()
      .then(setUser)
      .catch(() => setUser(null))
  }, [])

  if (user === undefined) {
    return <div style={{ padding: '2rem', textAlign: 'center' }}>Loading…</div>
  }

  return (
    <AuthContext.Provider value={{ user, setUser }}>
      <Routes>
        <Route path="/login" element={user ? <Navigate to="/employees" replace /> : <LoginPage />} />
        <Route path="/employees" element={<RequireAuth><EmployeesPage /></RequireAuth>} />
        <Route path="*" element={<Navigate to={user ? '/employees' : '/login'} replace />} />
      </Routes>
    </AuthContext.Provider>
  )
}
