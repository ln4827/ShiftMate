import { useState, useEffect, useMemo } from 'react'
import { shiftApi, departmentApi, employeeApi } from '../api/client'
import styles from './SchedulePage.module.css'

const parseISO = (value) => {
  if (typeof value === 'string') {
    const [y, m, d] = value.split('-').map(Number)
    return new Date(y, m - 1, d)
  }
  return value
}

const toISO = (d) => {
  const date = parseISO(d)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const toMon = (d) => {
  const date = parseISO(d)
  const c = new Date(date)
  const weekday = c.getDay() === 0 ? 7 : c.getDay()
  c.setDate(c.getDate() - (weekday - 1))
  c.setHours(0, 0, 0, 0)
  return c
}

const addDays = (d, n) => {
  const date = parseISO(d)
  const c = new Date(date)
  c.setDate(c.getDate() + n)
  return c
}

const fmtDate = (d) => d.toLocaleDateString('en-GB', { weekday: 'short', day: '2-digit', month: 'short' })
const fmtRange = (d) => `${fmtDate(d)} – ${fmtDate(addDays(d, 6))}`

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const TODAY_ISO = toISO(new Date())

const DEPT_COLORS = ['#01B574', '#4318FF', '#7551FF', '#FF6B35', '#E31A1A', '#39B8FF']
const deptColor = (deptId) => DEPT_COLORS[(deptId - 1) % DEPT_COLORS.length] || DEPT_COLORS[0]

function initials(name = '') {
  return name.split(' ').map(p => p[0] || '').join('').slice(0, 2).toUpperCase()
}

// true when [s1,e1) and [s2,e2) overlap (HH:mm strings)
function timesOverlap(s1, e1, s2, e2) {
  return s1 < e2 && s2 < e1
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

  const [shiftModal, setShiftModal]   = useState(null)
  const [assignModal, setAssignModal] = useState(null)
  const [assignForm, setAssignForm]   = useState(BLANK_ASSIGN)
  const [empRoles, setEmpRoles]       = useState([])
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

  const weekDates = useMemo(() =>
    Array.from({ length: 7 }, (_, i) => toISO(addDays(weekStart, i))),
    [weekStart]
  )

  // Shifts grouped by date, sorted by start time
  const shiftsByDay = useMemo(() => {
    const map = {}
    weekDates.forEach(d => { map[d] = [] })
    shifts.forEach(s => { if (map[s.shiftDate]) map[s.shiftDate].push(s) })
    weekDates.forEach(d => map[d].sort((a, b) => a.startTime.localeCompare(b.startTime)))
    return map
  }, [shifts, weekDates])

  // Shifts that have coverage requirements but don't meet them
  const coverageWarnings = useMemo(() =>
    shifts.filter(s => s.coverageRequirements?.length > 0 && !s.coverageMet),
    [shifts]
  )

  // Per-employee conflict/availability status for the currently open assign modal
  const employeeStatus = useMemo(() => {
    if (!assignModal) return {}
    const shift = shifts.find(s => s.id === assignModal.shiftId)
    if (!shift) return {}
    const status = {}
    employees.filter(e => e.active).forEach(emp => {
      const key = String(emp.id)
      if (shift.assignments.some(a => a.employeeId === emp.id)) {
        status[key] = 'assigned'
        return
      }
      const conflict = shifts.some(s =>
        s.id !== shift.id &&
        s.shiftDate === shift.shiftDate &&
        s.assignments.some(a => a.employeeId === emp.id) &&
        timesOverlap(s.startTime, s.endTime, shift.startTime, shift.endTime)
      )
      status[key] = conflict ? 'conflict' : 'available'
    })
    return status
  }, [assignModal, shifts, employees])

  // ── Shift CRUD ───────────────────────────────────────────────────────────────

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
    shiftApi.delete(id).then(loadShifts).catch(e => setError(e.message))
  }

  const togglePublish = (s) => {
    const call = s.published ? shiftApi.unpublish(s.id) : shiftApi.publish(s.id)
    call.then(loadShifts).catch(e => setError(e.message))
  }

  // ── Assign ───────────────────────────────────────────────────────────────────

  const openAssign = (shiftId) => {
    setAssignForm(BLANK_ASSIGN)
    setEmpRoles([])
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
      .then(() => { setAssignModal(null); setEmpRoles([]); loadShifts() })
      .catch(e => setModalError(e.message))
      .finally(() => setSubmitting(false))
  }

  const unassign = (shiftId, assignmentId) => {
    shiftApi.unassign(shiftId, assignmentId)
      .then(loadShifts)
      .catch(e => setError(e.message))
  }

  const activeEmps = employees.filter(e => e.active)
  const selectedEmpStatus = employeeStatus[assignForm.employeeId]

  // ── Render ───────────────────────────────────────────────────────────────────

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

      {/* Coverage warning banner */}
      {coverageWarnings.length > 0 && (
        <div className={styles.coverageBanner}>
          <span className={styles.coverageBannerIcon}>&#9888;</span>
          <div>
            <strong>{coverageWarnings.length} shift{coverageWarnings.length > 1 ? 's' : ''}</strong>
            {' '}{coverageWarnings.length > 1 ? 'have' : 'has'} unmet coverage requirements:{' '}
            {coverageWarnings.map((s, i) => (
              <span key={s.id}>
                {i > 0 && ', '}
                {s.shiftDate} {s.startTime}–{s.endTime}
                {s.departmentName ? ` (${s.departmentName})` : ''}
              </span>
            ))}
          </div>
        </div>
      )}

      {error && <div className={styles.errorBanner}>{error}</div>}
      {loading && <div className={styles.loading}>Loading…</div>}

      {/* Weekly grid: 7 day columns, shifts as cards within each column */}
      <div className={styles.gridWrapper}>
        <div className={styles.dayGrid}>
          {weekDates.map((date, i) => {
            const isToday = date === TODAY_ISO
            const dayShifts = shiftsByDay[date] || []
            return (
              <div key={date} className={`${styles.dayColumn} ${isToday ? styles.dayColumnToday : ''}`}>
                <div className={`${styles.dayHeader} ${isToday ? styles.dayHeaderToday : ''}`}>
                  <span className={styles.dayName}>{DAYS[i]}</span>
                  {isToday
                    ? <span className={styles.dayNumToday}>{date.slice(8)}</span>
                    : <span className={styles.dayNum}>{date.slice(8)}</span>
                  }
                </div>
                <div className={styles.dayBody}>
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
                  <button
                    className={styles.addDayBtn}
                    onClick={() => openCreate(date)}
                    title={`Add shift on ${date}`}
                  >
                    + Add
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {/* Create / Edit Shift modal */}
      {shiftModal && (
        <div className={styles.overlay}>
          <dialog open className={styles.modal} aria-labelledby="shift-modal-title">
            <div className={styles.modalHeader}>
              <h3 id="shift-modal-title">{shiftModal.mode === 'create' ? 'New Shift' : 'Edit Shift'}</h3>
              <button className={styles.modalClose} onClick={() => setShiftModal(null)}>&#x2715;</button>
            </div>
            {modalError && <div className={styles.modalError}>{modalError}</div>}
            <form onSubmit={submitShift} className={styles.form}>
              <label>
                Department
                <select required value={formData.departmentId} onChange={e => setFormData(f => ({ ...f, departmentId: e.target.value }))}>
                  <option value="">Select department…</option>
                  {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                </select>
              </label>
              <label>
                Date
                <input type="date" required value={formData.shiftDate} onChange={e => setFormData(f => ({ ...f, shiftDate: e.target.value }))} />
              </label>
              <div className={styles.timeRow}>
                <label>
                  Start time
                  <input type="time" required value={formData.startTime} onChange={e => setFormData(f => ({ ...f, startTime: e.target.value }))} />
                </label>
                <label>
                  End time
                  <input type="time" required value={formData.endTime} onChange={e => setFormData(f => ({ ...f, endTime: e.target.value }))} />
                </label>
              </div>
              <div className={styles.modalFooter}>
                <button type="button" onClick={() => setShiftModal(null)}>Cancel</button>
                <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save Shift'}</button>
              </div>
            </form>
          </dialog>
        </div>
      )}

      {/* Assign Employee modal */}
      {assignModal && (
        <div className={styles.overlay}>
          <dialog open className={styles.modal} aria-labelledby="assign-modal-title">
            <div className={styles.modalHeader}>
              <h3 id="assign-modal-title">Assign Employee</h3>
              <button className={styles.modalClose} onClick={() => setAssignModal(null)}>&#x2715;</button>
            </div>
            {modalError && <div className={styles.modalError}>{modalError}</div>}
            <form onSubmit={submitAssign} className={styles.form}>
              <label>
                Employee
                <select
                  required
                  value={assignForm.employeeId}
                  onChange={e => {
                    const empId = e.target.value
                    setAssignForm(f => ({ ...f, employeeId: empId, roleId: '' }))
                    if (empId) {
                      const employee = employees.find(emp => String(emp.id) === empId)
                      setEmpRoles(employee?.roles || [])
                    } else {
                      setEmpRoles([])
                    }
                  }}
                >
                  <option value="">Select employee…</option>
                  {activeEmps.map(e => {
                    const st = employeeStatus[String(e.id)] || 'available'
                    const suffix = st === 'conflict' ? ' — ⚠ conflict' : st === 'assigned' ? ' — already assigned' : ''
                    return (
                      <option key={e.id} value={e.id} disabled={st === 'assigned'}>
                        {e.firstName} {e.lastName}{suffix}
                      </option>
                    )
                  })}
                </select>
                {assignForm.employeeId && (
                  <div className={`${styles.availIndicator} ${
                    selectedEmpStatus === 'available' ? styles.availGreen :
                    selectedEmpStatus === 'conflict'  ? styles.availRed   :
                    styles.availGrey
                  }`}>
                    {selectedEmpStatus === 'available' && '● Available — no scheduling conflicts'}
                    {selectedEmpStatus === 'conflict'  && '● Has a conflicting shift at this time'}
                    {selectedEmpStatus === 'assigned'  && '● Already assigned to this shift'}
                  </div>
                )}
              </label>
              <label>
                Role
                <select required value={assignForm.roleId} onChange={e => setAssignForm(f => ({ ...f, roleId: e.target.value }))}>
                  <option value="">Select role…</option>
                  {empRoles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                </select>
              </label>
              <div className={styles.modalFooter}>
                <button type="button" onClick={() => { setAssignModal(null); setEmpRoles([]) }}>Cancel</button>
                <button type="submit" disabled={submitting || selectedEmpStatus === 'assigned'}>
                  {submitting ? 'Assigning…' : 'Assign'}
                </button>
              </div>
            </form>
          </dialog>
        </div>
      )}
    </div>
  )
}

// ── Shift block card ──────────────────────────────────────────────────────────

function ShiftBlock({ shift: s, onEdit, onAssign, onDelete, onTogglePublish, onUnassign }) {
  const color = deptColor(s.departmentId)
  return (
    <div className={styles.shiftBlock} style={{ background: color }}>
      <div className={styles.shiftTopRow}>
        <span className={styles.shiftTime}>{s.startTime} – {s.endTime}</span>
        <div className={styles.shiftBadges}>
          {!s.published && <span className={styles.draftPill}>Draft</span>}
          {s.coverageRequirements?.length > 0 && (
            <span className={`${styles.coveragePill} ${s.coverageMet ? styles.coverageMetPill : styles.coverageUnmetPill}`}>
              {s.coverageMet ? '✓' : '⚠'}
            </span>
          )}
        </div>
      </div>

      <span className={styles.shiftDept}>{s.departmentName}</span>

      {s.assignments.length > 0 ? (
        <div className={styles.shiftAssignees}>
          {s.assignments.map(a => (
            <div key={a.id} className={styles.assigneeRow}>
              <span className={styles.assigneeAvatar}>{initials(a.employeeName || '')}</span>
              <span className={styles.assigneeName}>{a.employeeName}</span>
              <button className={styles.removeBtn} onClick={() => onUnassign(s.id, a.id)} title="Remove">&#x2715;</button>
            </div>
          ))}
        </div>
      ) : (
        <div className={styles.unassignedHint}>No one assigned</div>
      )}

      <div className={styles.shiftActions}>
        <button className={styles.actBtn} onClick={() => onEdit(s)}>Edit</button>
        <button className={styles.actBtn} onClick={() => onAssign(s.id)}>Assign</button>
        <button
          className={`${styles.actBtn} ${s.published ? styles.unpublishBtn : styles.publishActBtn}`}
          onClick={() => onTogglePublish(s)}
        >
          {s.published ? 'Unpublish' : 'Publish'}
        </button>
        <button className={`${styles.actBtn} ${styles.delBtn}`} onClick={() => onDelete(s.id)}>Del</button>
      </div>
    </div>
  )
}
