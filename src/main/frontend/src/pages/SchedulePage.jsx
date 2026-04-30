import { useState, useEffect, useMemo } from 'react'
import { shiftApi, departmentApi, employeeApi } from '../api/client'
import styles from './SchedulePage.module.css'

const toISO   = (d) => d.toISOString().split('T')[0]
const toMon   = (d) => { const c = new Date(d); c.setDate(d.getDate() - (d.getDay() === 0 ? 6 : d.getDay() - 1)); c.setHours(0,0,0,0); return c }
const addDays = (d, n) => { const c = new Date(d); c.setDate(d.getDate() + n); return c }
const fmtDate = (d) => d.toLocaleDateString('en-GB', { weekday: 'short', day: '2-digit', month: 'short' })
const fmtRange = (d) => `${fmtDate(d)} – ${fmtDate(addDays(d, 6))}`

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const TODAY_ISO = toISO(new Date())

// Per-department color palette (cycles through)
const DEPT_COLORS = ['#01B574', '#4318FF', '#7551FF', '#FF6B35', '#E31A1A', '#39B8FF']
const deptColor = (deptId) => DEPT_COLORS[(deptId - 1) % DEPT_COLORS.length] || DEPT_COLORS[0]

function initials(name = '') {
  return name.split(' ').map(p => p[0] || '').join('').slice(0, 2).toUpperCase()
}

const BLANK_SHIFT  = { departmentId: '', shiftDate: '', startTime: '', endTime: '' }
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
  const [shiftModal, setShiftModal]   = useState(null)
  const [assignModal, setAssignModal] = useState(null)
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

  // Weekday ISO dates
  const weekDates = useMemo(() =>
    Array.from({ length: 7 }, (_, i) => toISO(addDays(weekStart, i))),
    [weekStart]
  )

  // Active employees who appear in any shift this week, plus all active employees
  const activeEmps = useMemo(() =>
    employees.filter(e => e.active),
    [employees]
  )

  // Build map: employeeId → { [date]: [shift, ...] }
  // Also collect unassigned shifts (no assignments at all)
  const { empShiftMap, unassignedByDay } = useMemo(() => {
    const empMap = {}
    activeEmps.forEach(e => {
      empMap[e.id] = {}
      weekDates.forEach(d => { empMap[e.id][d] = [] })
    })
    const unassigned = {}
    weekDates.forEach(d => { unassigned[d] = [] })

    shifts.forEach(s => {
      if (s.assignments.length === 0) {
        if (unassigned[s.shiftDate]) unassigned[s.shiftDate].push(s)
        return
      }
      s.assignments.forEach(a => {
        if (empMap[a.employeeId] && empMap[a.employeeId][s.shiftDate] !== undefined) {
          empMap[a.employeeId][s.shiftDate].push(s)
        }
      })
    })
    return { empShiftMap: empMap, unassignedByDay: unassigned }
  }, [shifts, weekDates, activeEmps])

  // ── Shift CRUD ──────────────────────────────────────────────────────────────

  const openCreate = (prefillDate) => {
    setFormData({ ...BLANK_SHIFT, shiftDate: prefillDate || toISO(weekStart) })
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

  // ── Assign ──────────────────────────────────────────────────────────────────

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

  // ── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className={styles.page}>
      {/* Toolbar */}
      <div className={styles.toolbar}>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, -7))}>&#8592; Prev</button>
        <span className={styles.weekLabel}>{fmtRange(weekStart)}</span>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, 7))}>Next &#8594;</button>
        <div className={styles.toolbarSpacer} />
        <button className={styles.newBtn} onClick={() => openCreate()}>+ Add Shift</button>
      </div>

      {error && <div className={styles.errorBanner}>{error}</div>}
      {loading && <div className={styles.loading}>Loading…</div>}

      {/* Grid */}
      <div className={styles.gridWrapper}>
        <div className={styles.grid}>

          {/* Header row */}
          <div className={styles.headerNameCell} />
          {weekDates.map((date, i) => {
            const isToday = date === TODAY_ISO
            return (
              <div key={date} className={styles.headerCell}>
                <span className={styles.dayName}>{DAYS[i]}</span>
                {isToday
                  ? <span className={styles.dayNumToday}>{date.slice(8)}</span>
                  : <span className={styles.dayNum}>{date.slice(8)}</span>
                }
              </div>
            )
          })}

          {/* Employee rows */}
          {activeEmps.map(emp => (
            <>
              {/* Employee name cell */}
              <div key={`emp-${emp.id}`} className={styles.empCell}>
                <div className={styles.empAvatar}>{initials(`${emp.firstName} ${emp.lastName}`)}</div>
                <div className={styles.empInfo}>
                  <div className={styles.empName}>{emp.firstName} {emp.lastName}</div>
                  {emp.roles?.[0] && <span className={styles.empRole}>{emp.roles[0].name}</span>}
                </div>
              </div>

              {/* Day cells for this employee */}
              {weekDates.map(date => {
                const dayShifts = empShiftMap[emp.id]?.[date] || []
                // Deduplicate shifts (employee may have multiple assignments in same shift)
                const uniqueShifts = [...new Map(dayShifts.map(s => [s.id, s])).values()]
                return (
                  <div key={`${emp.id}-${date}`} className={styles.dayCell}>
                    {uniqueShifts.length === 0 && (
                      <button
                        className={styles.addBtn}
                        onClick={() => openCreate(date)}
                        title="Add shift"
                      >+</button>
                    )}
                    {uniqueShifts.map(s => (
                      <ShiftBlock
                        key={s.id}
                        shift={s}
                        onEdit={openEdit}
                        onAssign={openAssign}
                        onDelete={deleteShift}
                        onTogglePublish={togglePublish}
                        onUnassign={unassign}
                      />
                    ))}
                  </div>
                )
              })}
            </>
          ))}

          {/* Unassigned shifts row */}
          {shifts.some(s => s.assignments.length === 0) && (
            <>
              <div className={styles.empCell}>
                <div className={styles.empInfo}>
                  <div className={styles.empName} style={{ color: '#A3AED0', fontStyle: 'italic' }}>Unassigned</div>
                </div>
              </div>
              {weekDates.map(date => {
                const dayShifts = unassignedByDay[date] || []
                return (
                  <div key={`unassigned-${date}`} className={styles.dayCell}>
                    {dayShifts.length === 0 && (
                      <button
                        className={styles.addBtn}
                        onClick={() => openCreate(date)}
                        title="Add shift"
                      >+</button>
                    )}
                    {dayShifts.map(s => (
                      <ShiftBlock
                        key={s.id}
                        shift={s}
                        onEdit={openEdit}
                        onAssign={openAssign}
                        onDelete={deleteShift}
                        onTogglePublish={togglePublish}
                        onUnassign={unassign}
                      />
                    ))}
                  </div>
                )
              })}
            </>
          )}

        </div>
      </div>

      {/* Shift modal */}
      {shiftModal && (
        <div className={styles.overlay}>
          <dialog open className={styles.modal} aria-labelledby="shift-modal-title">
            <div className={styles.modalHeader}>
              <h3 id="shift-modal-title">{shiftModal.mode === 'create' ? 'New Shift' : 'Edit Shift'}</h3>
              <button className={styles.modalClose} onClick={() => setShiftModal(null)}>&#x2715;</button>
            </div>
            {modalError && <div className={styles.modalError}>{modalError}</div>}
            <form onSubmit={submitShift} className={styles.form}>
              <label htmlFor="shift-dept">Department</label>
              <select id="shift-dept" required value={formData.departmentId} onChange={e => setFormData(f => ({ ...f, departmentId: e.target.value }))}>
                <option value="">Select…</option>
                {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
              <label htmlFor="shift-date">Date</label>
              <input id="shift-date" type="date" required value={formData.shiftDate} onChange={e => setFormData(f => ({ ...f, shiftDate: e.target.value }))} />
              <label htmlFor="shift-start">Start time</label>
              <input id="shift-start" type="time" required value={formData.startTime} onChange={e => setFormData(f => ({ ...f, startTime: e.target.value }))} />
              <label htmlFor="shift-end">End time</label>
              <input id="shift-end" type="time" required value={formData.endTime} onChange={e => setFormData(f => ({ ...f, endTime: e.target.value }))} />
              <div className={styles.modalFooter}>
                <button type="button" onClick={() => setShiftModal(null)}>Cancel</button>
                <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save'}</button>
              </div>
            </form>
          </dialog>
        </div>
      )}

      {/* Assign modal */}
      {assignModal && (
        <div className={styles.overlay}>
          <dialog open className={styles.modal} aria-labelledby="assign-modal-title">
            <div className={styles.modalHeader}>
              <h3 id="assign-modal-title">Assign Employee</h3>
              <button className={styles.modalClose} onClick={() => setAssignModal(null)}>&#x2715;</button>
            </div>
            {modalError && <div className={styles.modalError}>{modalError}</div>}
            <form onSubmit={submitAssign} className={styles.form}>
              <label htmlFor="assign-emp">Employee</label>
              <select id="assign-emp" required value={assignForm.employeeId} onChange={e => setAssignForm(f => ({ ...f, employeeId: e.target.value }))}>
                <option value="">Select…</option>
                {employees.filter(e => e.active).map(e => (
                  <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>
                ))}
              </select>
              <label htmlFor="assign-role">Role</label>
              <select id="assign-role" required value={assignForm.roleId} onChange={e => setAssignForm(f => ({ ...f, roleId: e.target.value }))}>
                <option value="">Select…</option>
                {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
              </select>
              <div className={styles.modalFooter}>
                <button type="button" onClick={() => setAssignModal(null)}>Cancel</button>
                <button type="submit" disabled={submitting}>{submitting ? 'Assigning…' : 'Assign'}</button>
              </div>
            </form>
          </dialog>
        </div>
      )}
    </div>
  )
}

// ── Shift block component ────────────────────────────────────────────────────

function ShiftBlock({ shift: s, onEdit, onAssign, onDelete, onTogglePublish, onUnassign }) {
  const color = deptColor(s.departmentId)
  return (
    <div
      className={styles.shiftBlock}
      style={{ background: color }}
    >
      <span className={styles.shiftTime}>{s.startTime} – {s.endTime}</span>
      <span className={styles.shiftDept}>{s.departmentName}</span>

      {!s.published && <span className={styles.draftPill}>Draft</span>}

      {s.coverageRequirements?.length > 0 && (
        <span className={styles.coveragePill}>
          {s.coverageMet ? '✓ Coverage' : '⚠ Unmet'}
        </span>
      )}

      {s.assignments.length > 0 && (
        <div className={styles.shiftAssignees}>
          {s.assignments.map(a => (
            <div key={a.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span>{a.employeeName}</span>
              <button className={styles.removeBtn} onClick={() => onUnassign(s.id, a.id)} title="Remove">&#x2715;</button>
            </div>
          ))}
        </div>
      )}

      <div className={styles.shiftActions}>
        <button className={styles.actBtn} onClick={() => onEdit(s)}>Edit</button>
        <button className={styles.actBtn} onClick={() => onAssign(s.id)}>Assign</button>
        <button className={styles.actBtn} onClick={() => onTogglePublish(s)}>
          {s.published ? 'Unpublish' : 'Publish'}
        </button>
        <button className={`${styles.actBtn} ${styles.delBtn}`} onClick={() => onDelete(s.id)}>Del</button>
      </div>
    </div>
  )
}
