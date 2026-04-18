import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { employeeApi, authApi } from '../api/client'
import { useAuth } from '../App'
import styles from './EmployeesPage.module.css'

export default function EmployeesPage() {
  const { user, setUser } = useAuth()
  const navigate = useNavigate()
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    employeeApi.list()
      .then(setEmployees)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  async function handleLogout() {
    await authApi.logout().catch(() => {})
    setUser(null)
    navigate('/login')
  }

  async function handleToggle(employee) {
    try {
      if (employee.active) {
        await employeeApi.deactivate(employee.id)
      } else {
        await employeeApi.reactivate(employee.id)
      }
      const updated = await employeeApi.list()
      setEmployees(updated)
    } catch (err) {
      alert(err.message)
    }
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>ShiftMate</h1>
        <div className={styles.headerRight}>
          <span className={styles.userInfo}>
            {user.email} {user.manager && <span className={styles.badge}>Manager</span>}
          </span>
          <button onClick={handleLogout} className={styles.logoutBtn}>Sign out</button>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.toolbar}>
          <h2 className={styles.sectionTitle}>Employees</h2>
        </div>

        {loading && <p className={styles.state}>Loading…</p>}
        {error   && <p className={styles.errorState}>{error}</p>}

        {!loading && !error && (
          <div className={styles.tableWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Roles</th>
                  <th>Status</th>
                  {user.manager && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {employees.length === 0 && (
                  <tr>
                    <td colSpan={user.manager ? 5 : 4} className={styles.empty}>
                      No employees found.
                    </td>
                  </tr>
                )}
                {employees.map(emp => (
                  <tr key={emp.id}>
                    <td className={styles.name}>
                      {emp.firstName} {emp.lastName}
                      {emp.manager && <span className={styles.badge}>Manager</span>}
                    </td>
                    <td>{emp.email}</td>
                    <td>
                      {emp.roles.length > 0
                        ? emp.roles.map(r => r.name).join(', ')
                        : <span className={styles.noRoles}>—</span>}
                    </td>
                    <td>
                      <span className={emp.active ? styles.active : styles.inactive}>
                        {emp.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    {user.manager && (
                      <td>
                        <button
                          onClick={() => handleToggle(emp)}
                          className={emp.active ? styles.deactivateBtn : styles.reactivateBtn}
                        >
                          {emp.active ? 'Deactivate' : 'Reactivate'}
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  )
}
