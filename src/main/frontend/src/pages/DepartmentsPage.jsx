import { useState, useEffect } from 'react'
import { departmentApi, employeeApi } from '../api/client'
import styles from './DepartmentsPage.module.css'

export default function DepartmentsPage() {
  const [departments, setDepartments] = useState([])
  const [availableRoles, setAvailableRoles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [editingDept, setEditingDept] = useState(null) // { id, name, allowedRoleIds }
  const [selectedRoleIds, setSelectedRoleIds] = useState([])
  const [saving, setSaving] = useState(false)
  const [modalError, setModalError] = useState('')

  useEffect(() => {
    Promise.all([
      departmentApi.list(),
      employeeApi.roles(),
    ])
      .then(([depts, roles]) => {
        setDepartments(depts)
        setAvailableRoles(roles)
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  const openEdit = (dept) => {
    setEditingDept(dept)
    setSelectedRoleIds(dept.allowedRoleIds || [])
    setModalError('')
  }

  const toggleRole = (roleId) => {
    setSelectedRoleIds(ids =>
      ids.includes(roleId) ? ids.filter(id => id !== roleId) : [...ids, roleId]
    )
  }

  const save = async (e) => {
    e.preventDefault()
    setSaving(true)
    setModalError('')
    try {
      const updated = await departmentApi.setAllowedRoles(editingDept.id, selectedRoleIds)
      setDepartments(ds => ds.map(d => d.id === updated.id ? updated : d))
      setEditingDept(null)
    } catch (err) {
      setModalError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const roleNameById = (id) => availableRoles.find(r => r.id === id)?.name || ''

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h2 className={styles.title}>Departments</h2>
      </div>

      {error && <div className={styles.errorState}>{error}</div>}
      {loading && <div className={styles.state}>Loading…</div>}

      {!loading && !error && (
        <div className={styles.tableWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Department</th>
                <th>Allowed Roles</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {departments.map(dept => (
                <tr key={dept.id}>
                  <td className={styles.deptName}>{dept.name}</td>
                  <td>
                    {dept.allowedRoleIds && dept.allowedRoleIds.length > 0 ? (
                      <div className={styles.roleBadges}>
                        {dept.allowedRoleIds.map(id => (
                          <span key={id} className={styles.roleBadge}>{roleNameById(id)}</span>
                        ))}
                      </div>
                    ) : (
                      <span className={styles.noRoles}>No restriction — all roles allowed</span>
                    )}
                  </td>
                  <td>
                    <button className={styles.editBtn} onClick={() => openEdit(dept)}>
                      Edit Roles
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {editingDept && (
        <div className={styles.modalOverlay}>
          <dialog open className={styles.modal} aria-labelledby="dept-modal-title">
            <div className={styles.modalHeader}>
              <h2 id="dept-modal-title">Allowed Roles — {editingDept.name}</h2>
              <button className={styles.closeBtn} onClick={() => setEditingDept(null)}>&#x2715;</button>
            </div>
            <p className={styles.hint}>
              Only employees with at least one selected role will appear when assigning shifts in this department.
              Deselect all to allow any role.
            </p>
            {modalError && <div className={styles.errorState}>{modalError}</div>}
            <form onSubmit={save}>
              <div className={styles.rolesGrid}>
                {availableRoles.map(role => (
                  <label key={role.id} className={styles.roleItem}>
                    <input
                      type="checkbox"
                      checked={selectedRoleIds.includes(role.id)}
                      onChange={() => toggleRole(role.id)}
                    />
                    {role.name}
                  </label>
                ))}
              </div>
              <div className={styles.formActions}>
                <button type="button" className={styles.cancelBtn} onClick={() => setEditingDept(null)}>
                  Cancel
                </button>
                <button type="submit" className={styles.submitBtn} disabled={saving}>
                  {saving ? 'Saving…' : 'Save'}
                </button>
              </div>
            </form>
          </dialog>
        </div>
      )}
    </div>
  )
}
