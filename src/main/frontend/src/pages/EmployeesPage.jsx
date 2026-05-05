import { useState, useEffect } from 'react'
import { employeeApi } from '../api/client'
import { useAuth } from '../App'
import styles from './EmployeesPage.module.css'

function initials(firstName = '', lastName = '') {
  return `${firstName[0] || ''}${lastName[0] || ''}`.toUpperCase()
}

const INITIAL_FORM = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  manager: false,
  roleIds: []
}

export default function EmployeesPage() {
  const { user } = useAuth()
  const [employees, setEmployees] = useState([])
  const [availableRoles, setAvailableRoles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Modal & Form State
  const [showModal, setShowModal] = useState(false)
  const [editingId, setEditingId] = useState(null) // null = add, id = edit
  const [formData, setFormData] = useState(INITIAL_FORM)

  useEffect(() => {
    refreshData()
    employeeApi.roles().then(setAvailableRoles).catch(console.error)
  }, [])

  const refreshData = () => {
    setLoading(true)
    employeeApi.list()
      .then(setEmployees)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }

  const openAddModal = () => {
    setEditingId(null)
    setFormData(INITIAL_FORM)
    setShowModal(true)
  }

  const openEditModal = (emp) => {
    setEditingId(emp.id)

    // Extract just the IDs from the employee's current roles
    const currentRoleIds = emp.roles ? emp.roles.map(r => r.id) : []

    console.log("Editing Employee. Current Role IDs:", currentRoleIds)

    setFormData({
      firstName: emp.firstName || '',
      lastName: emp.lastName || '',
      email: emp.email || '',
      password: '', // Keep blank
      manager: emp.manager || false,
      roleIds: currentRoleIds // This array drives the checkboxes
    })
    setShowModal(true)
  }

  async function handleToggleStatus(employee) {
    try {
      if (employee.active) {
        await employeeApi.deactivate(employee.id)
      } else {
        await employeeApi.reactivate(employee.id)
      }
      refreshData()
    } catch (err) {
      alert(err.message)
    }
  }

  async function handleSubmit(e) {
    e.preventDefault()
    try {
      if (editingId) {
        await employeeApi.update(editingId, formData)
      } else {
        await employeeApi.create(formData)
      }
      setShowModal(false)
      refreshData()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleRoleToggle = (roleId) => {
    setFormData(prev => ({
      ...prev,
      roleIds: prev.roleIds.includes(roleId)
        ? prev.roleIds.filter(id => id !== roleId)
        : [...prev.roleIds, roleId]
    }))
  }

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className={styles.title}>Employees</h1>
        {user.manager && (
          <button className={styles.addBtn} onClick={openAddModal}>
            + Add Employee
          </button>
        )}
      </div>

      {loading && <p className={styles.state}>Loading…</p>}
      {error && <p className={styles.errorState}>{error}</p>}

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
                      <div className={styles.actions}>
                        <button onClick={() => openEditModal(emp)} className={styles.editBtn}>
                          Edit
                        </button>
                        <button
                          onClick={() => handleToggleStatus(emp)}
                          className={emp.active ? styles.deactivateBtn : styles.reactivateBtn}
                        >
                          {emp.active ? 'Deactivate' : 'Reactivate'}
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className={styles.modalOverlay}>
          <div className={styles.modal}>
            <div className={styles.modalHeader}>
              <h2>{editingId ? 'Edit Employee' : 'Add New Employee'}</h2>
              <button onClick={() => setShowModal(false)} className={styles.closeBtn}>&times;</button>
            </div>
            <form onSubmit={handleSubmit} className={styles.form}>
              <div className={styles.formGrid}>
                <div className={styles.field}>
                  <label>First Name</label>
                  <input
                    type="text" required
                    value={formData.firstName}
                    onChange={e => setFormData({ ...formData, firstName: e.target.value })}
                  />
                </div>
                <div className={styles.field}>
                  <label>Last Name</label>
                  <input
                    type="text" required
                    value={formData.lastName}
                    onChange={e => setFormData({ ...formData, lastName: e.target.value })}
                  />
                </div>
              </div>

              <div className={styles.field}>
                <label>Email Address</label>
                <input
                  type="email" required
                  value={formData.email}
                  onChange={e => setFormData({ ...formData, email: e.target.value })}
                />
              </div>

              <div className={styles.field}>
                <label>Password {editingId && <small>(Leave blank to keep current)</small>}</label>
                <input
                  type="password" required={!editingId}
                  value={formData.password}
                  onChange={e => setFormData({ ...formData, password: e.target.value })}
                />
              </div>

              <div className={styles.checkboxField}>
                <label>
                  <input
                    type="checkbox"
                    checked={formData.manager}
                    onChange={e => setFormData({ ...formData, manager: e.target.checked })}
                  />
                  Is Manager
                </label>
              </div>

              <div className={styles.rolesSection}>
                <label className={styles.sectionLabel}>Assigned Roles</label>
                <div className={styles.rolesGrid}>
                  {availableRoles.map(role => (
                    <label key={role.id} className={styles.roleItem}>
                      <input
                        type="checkbox"
                        checked={formData.roleIds.includes(role.id)}
                        onChange={() => handleRoleToggle(role.id)}
                      />
                      {role.name}
                    </label>
                  ))}
                </div>
              </div>

              <div className={styles.formActions}>
                <button type="button" onClick={() => setShowModal(false)} className={styles.cancelBtn}>
                  Cancel
                </button>
                <button type="submit" className={styles.submitBtn}>
                  {editingId ? 'Update Employee' : 'Create Employee'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}