import { useState, useEffect, useMemo } from 'react'
import { shiftApi, departmentApi, employeeApi } from '../api/client'
import styles from './SchedulePage.module.css'

const toISO  = (d) => d.toISOString().split('T')[0]
const toMon  = (d) => { const c = new Date(d); c.setDate(d.getDate() - (d.getDay() === 0 ? 6 : d.getDay() - 1)); c.setHours(0,0,0,0); return c }
const addDays = (d, n) => { const c = new Date(d); c.setDate(d.getDate() + n); return c }
const fmtDate = (d) => d.toLocaleDateString('en-GB', { weekday: 'short', day: '2-digit', month: 'short' })
const fmtRange = (d) => `${fmtDate(d)} – ${fmtDate(addDays(d, 6))}`

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']

const BLANK_SHIFT = { departmentId: '', shiftDate: '', startTime: '', endTime: '' }
const BLANK_ASSIGN = { employeeId: '', roleId: '' }

export default function SchedulePage() {
  const [weekStart, setWeekStart] = useState(() => toMon(new Date()))
  const [shifts, setShifts]       = useState([])
  const [departments, setDepts]   = useState([])
  const [employees, setEmps]      = useState([])
  const [roles, setRoles]         = useState([])
  const [loading, setLoading]     = useState(false)
  const [error, setError]         = useState('')

  // Modals
  const [shiftModal, setShiftModal]   = useState(null)   // null | { mode:'create'|'edit', data, shiftId? }
  const [assignModal, setAssignModal] = useState(null)   // null | { shiftId }
  const [assignForm, setAssignForm]   = useState(BLANK_ASSIGN)
  const [formData, setFormData]       = useState(BLANK_SHIFT)
  const [submitting, setSubmitting]   = useState(false)
  const [modalError, setModalError]   = useState('')

  useEffect(() => {
    departmentApi.list().then(setDepts).catch(() => {})
    employeeApi.list().then(setEmps).catch(() => {})
    employeeApi.roles().then(setRoles).catch(() => {})
  }, [])

  useEffect(() => { loadShifts() }, [weekStart]) // eslint-disable-line react-hooks/exhaustive-deps

  const loadShifts = () => {
    setLoading(true)
    setError('')
    shiftApi.list(weekStart)
      .then(setShifts)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }

  // Group shifts by ISO date
  const byDay = useMemo(() => {
    const map = {}
    for (let i = 0; i < 7; i++) {
      map[toISO(addDays(weekStart, i))] = []
    }
    shifts.forEach(s => { if (map[s.shiftDate]) map[s.shiftDate].push(s) })
    return map
  }, [shifts, weekStart])

  // ── Shift CRUD ────────────────────────────────────────────────────────────

  const openCreate = () => {
    setFormData({ ...BLANK_SHIFT, shiftDate: toISO(weekStart) })
    setModalError('')
    setShiftModal({ mode: 'create' })
  }

  const openEdit = (s) => {
    setFormData({
      departmentId: String(s.departmentId),
      shiftDate:    s.shiftDate,
      startTime:    s.startTime,
      endTime:      s.endTime,
    })
    setModalError('')
    setShiftModal({ mode: 'edit', shiftId: s.id })
  }

  const submitShift = (e) => {
    e.preventDefault()
    setSubmitting(true)
    setModalError('')
    const payload = {
      departmentId: Number(formData.departmentId),
      shiftDate:    formData.shiftDate,
      startTime:    formData.startTime,
      endTime:      formData.endTime,
    }
    const call = shiftModal.mode === 'create'
      ? shiftApi.create(payload)
      : shiftApi.update(shiftModal.shiftId, payload)

    call
      .then(() => { setShiftModal(null); loadShifts() })
      .catch(e => setModalError(e.message))
      .finally(() => setSubmitting(false))
  }

  const deleteShift = (id) => {
    if (!window.confirm('Delete this shift?')) return
    shiftApi.delete(id)
      .then(loadShifts)
      .catch(e => setError(e.message))
  }

  const togglePublish = (s) => {
    const call = s.published ? shiftApi.unpublish(s.id) : shiftApi.publish(s.id)
    call.then(loadShifts).catch(e => setError(e.message))
  }

  // ── Assign ────────────────────────────────────────────────────────────────

  const openAssign = (shiftId) => {
    setAssignForm(BLANK_ASSIGN)
    setModalError('')
    setAssignModal({ shiftId })
  }

  const submitAssign = (e) => {
    e.preventDefault()
    setSubmitting(true)
    setModalError('')
    shiftApi.assign(assignModal.shiftId, {
      employeeId: Number(assignForm.employeeId),
      roleId:     Number(assignForm.roleId),
    })
      .then(() => { setAssignModal(null); loadShifts() })
      .catch(e => setModalError(e.message))
      .finally(() => setSubmitting(false))
  }

  const unassign = (shiftId, assignmentId) => {
    shiftApi.unassign(shiftId, assignmentId)
      .then(loadShifts)
      .catch(e => setError(e.message))
  }

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, -7))}>← Prev</button>
        <span className={styles.weekLabel}>{fmtRange(weekStart)}</span>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, 7))}>Next →</button>
        <button className={styles.newBtn} onClick={openCreate}>+ New Shift</button>
      </div>

      {error && <div className={styles.errorBanner}>{error}</div>}
      {loading && <div className={styles.loading}>Loading…</div>}

      <div className={styles.grid}>
        {Object.entries(byDay).map(([date, dayShifts], i) => (
          <div key={date} className={styles.dayCol}>
            <div className={styles.dayHeader}>{DAYS[i]} {date.slice(5)}</div>
            {dayShifts.length === 0 && <div className={styles.empty}>—</div>}
            {dayShifts.map(s => (
              <div key={s.id} className={`${styles.card} ${s.published ? styles.published : styles.draft}`}>
                <div className={styles.cardTop}>
                  <span className={styles.dept}>{s.departmentName}</span>
                  <span className={`${styles.badge} ${s.published ? styles.badgePub : styles.badgeDraft}`}>
                    {s.published ? 'Published' : 'Draft'}
                  </span>
                </div>
                <div className={styles.time}>{s.startTime} – {s.endTime}</div>

                {s.coverageRequirements.length > 0 && (
                  <div className={`${styles.coverage} ${s.coverageMet ? styles.covMet : styles.covUnmet}`}>
                    {s.coverageMet ? '✓ Coverage met' : '⚠ Coverage unmet'}
                  </div>
                )}

                {s.assignments.length > 0 && (
                  <ul className={styles.assignList}>
                    {s.assignments.map(a => (
                      <li key={a.id} className={styles.assignItem}>
                        <span>{a.employeeName} <em>({a.roleName})</em></span>
                        <button
                          className={styles.removeBtn}
                          onClick={() => unassign(s.id, a.id)}
                          title="Remove assignment"
                        >✕</button>
                      </li>
                    ))}
                  </ul>
                )}

                <div className={styles.actions}>
                  <button className={styles.actBtn} onClick={() => openEdit(s)}>Edit</button>
                  <button className={styles.actBtn} onClick={() => openAssign(s.id)}>Assign</button>
                  <button className={styles.actBtn} onClick={() => togglePublish(s)}>
                    {s.published ? 'Unpublish' : 'Publish'}
                  </button>
                  <button className={`${styles.actBtn} ${styles.delBtn}`} onClick={() => deleteShift(s.id)}>Delete</button>
                </div>
              </div>
            ))}
          </div>
        ))}
      </div>

      {/* Shift modal */}
      {shiftModal && (
        <div className={styles.overlay} onClick={() => setShiftModal(null)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>{shiftModal.mode === 'create' ? 'New Shift' : 'Edit Shift'}</h3>
            {modalError && <div className={styles.modalError}>{modalError}</div>}
            <form onSubmit={submitShift} className={styles.form}>
              <label>Department
                <select required value={formData.departmentId} onChange={e => setFormData(f => ({ ...f, departmentId: e.target.value }))}>
                  <option value="">Select…</option>
                  {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                </select>
              </label>
              <label>Date
                <input type="date" required value={formData.shiftDate} onChange={e => setFormData(f => ({ ...f, shiftDate: e.target.value }))} />
              </label>
              <label>Start
                <input type="time" required value={formData.startTime} onChange={e => setFormData(f => ({ ...f, startTime: e.target.value }))} />
              </label>
              <label>End
                <input type="time" required value={formData.endTime} onChange={e => setFormData(f => ({ ...f, endTime: e.target.value }))} />
              </label>
              <div className={styles.modalFooter}>
                <button type="button" onClick={() => setShiftModal(null)}>Cancel</button>
                <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Assign modal */}
      {assignModal && (
        <div className={styles.overlay} onClick={() => setAssignModal(null)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>Assign Employee</h3>
            {modalError && <div className={styles.modalError}>{modalError}</div>}
            <form onSubmit={submitAssign} className={styles.form}>
              <label>Employee
                <select required value={assignForm.employeeId} onChange={e => setAssignForm(f => ({ ...f, employeeId: e.target.value }))}>
                  <option value="">Select…</option>
                  {employees.filter(e => e.active).map(e => (
                    <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>
                  ))}
                </select>
              </label>
              <label>Role
                <select required value={assignForm.roleId} onChange={e => setAssignForm(f => ({ ...f, roleId: e.target.value }))}>
                  <option value="">Select…</option>
                  {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                </select>
              </label>
              <div className={styles.modalFooter}>
                <button type="button" onClick={() => setAssignModal(null)}>Cancel</button>
                <button type="submit" disabled={submitting}>{submitting ? 'Assigning…' : 'Assign'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
