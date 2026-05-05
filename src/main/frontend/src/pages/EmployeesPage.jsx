import { useState, useEffect } from 'react'
import { employeeApi } from '../api/client'
import { useAuth } from '../App'
import styles from './EmployeesPage.module.css'

function initials(firstName = '', lastName = '') {
  return `${firstName[0] || ''}${lastName[0] || ''}`.toUpperCase()
}

export default function EmployeesPage() {
  const { user } = useAuth()
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    employeeApi.list()
      .then(setEmployees)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

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
      <div className={styles.pageHeader}>
        <h1 className={styles.title}>Employees</h1>
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
                  <td>
                    <div className={styles.nameCell}>
                      <div className={styles.avatar}>
                        {initials(emp.firstName, emp.lastName)}
                      </div>
                      <span className={styles.nameText}>
                        {emp.firstName} {emp.lastName}
                        {emp.manager && <span className={styles.badge}>Manager</span>}
                      </span>
                    </div>
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
    </div>
  )
}
