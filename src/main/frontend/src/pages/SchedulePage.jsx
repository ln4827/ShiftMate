import { useState, useEffect, useMemo } from 'react'
import { shiftApi, departmentApi, employeeApi, availabilityApi } from '../api/client'
import { useAuth } from '../App'
import { useConfirm } from '../components/ConfirmDialog'
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

const BLANK_SHIFT = { departmentId: '', shiftDate: '', startTime: '', endTime: '' }

export default function SchedulePage() {
  const { bumpNotifTick } = useAuth()
  const confirm = useConfirm()

  const [weekStart, setWeekStart] = useState(() => toMon(new Date()))
  const [shifts, setShifts]       = useState([])
  const [departments, setDepts]   = useState([])
  const [employees, setEmps]      = useState([])
  const [roles, setRoles]         = useState([])
  const [loading, setLoading]     = useState(false)
  const [error, setError]         = useState('')

  const [shiftModal, setShiftModal] = useState(null)
  const [formData, setFormData]     = useState(BLANK_SHIFT)
  const [submitting, setSubmitting] = useState(false)
  const [modalError, setModalError] = useState('')

  // Inline assignment state inside the shift modal
  const [inlineEmpId, setInlineEmpId]       = useState('')
  const [inlineRoleId, setInlineRoleId]     = useState('')
  const [inlineEmpRoles, setInlineEmpRoles] = useState([])
  const [inlineAvail, setInlineAvail]       = useState([])
  const [availLoading, setAvailLoading]     = useState(false)
  const [pendingAdditions, setPendingAdditions] = useState([]) // { tempId, employeeId, employeeName, roleId, roleName }
  const [pendingRemovals, setPendingRemovals]   = useState([]) // assignment IDs to remove on save

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

  const shiftsByDay = useMemo(() => {
    const map = {}
    weekDates.forEach(d => { map[d] = [] })
    shifts.forEach(s => { if (map[s.shiftDate]) map[s.shiftDate].push(s) })
    weekDates.forEach(d => map[d].sort((a, b) => a.startTime.localeCompare(b.startTime)))
    return map
  }, [shifts, weekDates])

  const coverageWarnings = useMemo(() =>
    shifts.filter(s => s.coverageRequirements?.length > 0 && !s.coverageMet),
    [shifts]
  )

  // Allowed role IDs for whichever department is selected in the form
  const formDeptAllowedRoleIds = useMemo(() => {
    if (!formData.departmentId) return []
    const dept = departments.find(d => d.id === Number(formData.departmentId))
    return dept?.allowedRoleIds || []
  }, [formData.departmentId, departments])

  // Active employees filtered by allowed roles and excluding already-assigned ones
  const eligibleEmps = useMemo(() => {
    const existingIds = new Set([
      ...(shiftModal?.mode === 'edit'
        ? (shifts.find(s => s.id === shiftModal.shiftId)?.assignments || [])
            .filter(a => !pendingRemovals.includes(a.id))
            .map(a => a.employeeId)
        : []),
      ...pendingAdditions.map(a => a.employeeId),
    ])
    const active = employees.filter(e => e.active && !existingIds.has(e.id))
    if (!formDeptAllowedRoleIds.length) return active
    return active.filter(emp => emp.roles?.some(r => formDeptAllowedRoleIds.includes(r.id)))
  }, [employees, formDeptAllowedRoleIds, shiftModal, shifts, pendingRemovals, pendingAdditions])

  // Selected employee's roles filtered by department allowed roles
  const filteredInlineRoles = useMemo(() => {
    if (!formDeptAllowedRoleIds.length) return inlineEmpRoles
    return inlineEmpRoles.filter(r => formDeptAllowedRoleIds.includes(r.id))
  }, [inlineEmpRoles, formDeptAllowedRoleIds])

  // Assignments to show in the modal: existing (minus removals) + pending additions
  const displayedAssignments = useMemo(() => {
    const existing = shiftModal?.mode === 'edit'
      ? (shifts.find(s => s.id === shiftModal.shiftId)?.assignments || [])
          .filter(a => !pendingRemovals.includes(a.id))
          .map(a => ({
            id: a.id,
            employeeName: a.employeeName,
            roleName: a.roleName || roles.find(r => r.id === a.roleId)?.name || '',
            isNew: false,
          }))
      : []
    const added = pendingAdditions.map(a => ({
      tempId: a.tempId,
      employeeName: a.employeeName,
      roleName: a.roleName,
      isNew: true,
    }))
    return [...existing, ...added]
  }, [shiftModal, shifts, pendingRemovals, pendingAdditions, roles])

  // Availability status for the currently selected inline employee
  const inlineAvailStatus = useMemo(() => {
    if (!inlineEmpId || !formData.shiftDate || !formData.startTime || !formData.endTime) return null
    if (availLoading) return 'loading'
    const dow = (() => {
      const d = new Date(formData.shiftDate + 'T00:00:00')
      const js = d.getDay()
      return js === 0 ? 7 : js // ISO: 1=Mon … 7=Sun
    })()
    const toHHMM = (t) => (t || '').slice(0, 5)
    const windows = inlineAvail.filter(w => w.dayOfWeek === dow)
    if (!windows.length) return 'unavailable'
    const s = formData.startTime, e = formData.endTime
    if (windows.some(w => toHHMM(w.startTime) <= s && toHHMM(w.endTime) >= e)) return 'available'
    if (windows.some(w => toHHMM(w.startTime) < e && toHHMM(w.endTime) > s)) return 'partial'
    return 'unavailable'
  }, [inlineEmpId, formData.shiftDate, formData.startTime, formData.endTime, inlineAvail, availLoading])

  // ── Shift CRUD ───────────────────────────────────────────────────────────────

  const resetInlineAssign = () => {
    setInlineEmpId('')
    setInlineRoleId('')
    setInlineEmpRoles([])
    setInlineAvail([])
    setAvailLoading(false)
    setPendingAdditions([])
    setPendingRemovals([])
  }

  const openCreate = (prefillDate) => {
    setFormData({ ...BLANK_SHIFT, shiftDate: prefillDate || toISO(weekStart) })
    setModalError('')
    resetInlineAssign()
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
    resetInlineAssign()
    setShiftModal({ mode: 'edit', shiftId: s.id })
  }

  const submitShift = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setModalError('')
    const payload = {
      departmentId: Number(formData.departmentId),
      shiftDate:    formData.shiftDate,
      startTime:    formData.startTime,
      endTime:      formData.endTime,
    }
    try {
      let shiftId
      if (shiftModal.mode === 'create') {
        const created = await shiftApi.create(payload)
        shiftId = created.id
      } else {
        await shiftApi.update(shiftModal.shiftId, payload)
        shiftId = shiftModal.shiftId
      }
      for (const aId of pendingRemovals) {
        await shiftApi.unassign(shiftId, aId)
      }
      for (const a of pendingAdditions) {
        await shiftApi.assign(shiftId, { employeeId: a.employeeId, roleId: a.roleId })
      }
      setShiftModal(null)
      loadShifts()
    } catch (err) {
      setModalError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const deleteShift = async (id) => {
    const ok = await confirm('Delete shift', 'This shift and all its assignments will be permanently removed.', {
      confirmText: 'Delete', variant: 'danger',
    })
    if (!ok) return
    shiftApi.delete(id).then(loadShifts).catch(e => setError(e.message))
  }

  const togglePublish = async (s) => {
    if (s.published) {
      shiftApi.unpublish(s.id).then(loadShifts).catch(e => setError(e.message))
      return
    }
    try {
      await shiftApi.publish(s.id)
      loadShifts()
    } catch (e) {
      if (e.message && e.message.toLowerCase().includes('coverage')) {
        const ok = await confirm('Coverage warning', e.message, {
          confirmText: 'Publish anyway', variant: 'warning',
        })
        if (ok) shiftApi.publish(s.id, true).then(loadShifts).catch(e2 => setError(e2.message))
      } else {
        setError(e.message)
      }
    }
  }

  const unassign = (shiftId, assignmentId) => {
    shiftApi.unassign(shiftId, assignmentId)
      .then(loadShifts)
      .catch(e => setError(e.message))
  }

  // ── Inline assignment handlers ────────────────────────────────────────────────

  const addInlineAssignment = () => {
    if (!inlineEmpId || !inlineRoleId) return
    const alreadyAdded = pendingAdditions.some(a => String(a.employeeId) === inlineEmpId)
    const alreadyExists = shiftModal?.mode === 'edit' &&
      (shifts.find(s => s.id === shiftModal.shiftId)?.assignments || [])
        .filter(a => !pendingRemovals.includes(a.id))
        .some(a => String(a.employeeId) === inlineEmpId)
    if (alreadyAdded || alreadyExists) {
      setModalError('This employee is already assigned to this shift.')
      return
    }
    const emp  = employees.find(e => String(e.id) === inlineEmpId)
    const role = roles.find(r => String(r.id) === inlineRoleId)
    setPendingAdditions(prev => [...prev, {
      tempId:       Date.now(),
      employeeId:   Number(inlineEmpId),
      employeeName: emp ? `${emp.firstName} ${emp.lastName}` : '',
      roleId:       Number(inlineRoleId),
      roleName:     role?.name || '',
    }])
    setInlineEmpId('')
    setInlineRoleId('')
    setInlineEmpRoles([])
    setModalError('')
  }

  const removeDisplayedAssignment = (a) => {
    if (a.isNew) {
      setPendingAdditions(prev => prev.filter(x => x.tempId !== a.tempId))
    } else {
      setPendingRemovals(prev => [...prev, a.id])
    }
  }

  const draftCount = shifts.filter(s => !s.published).length

  const publishWeek = async () => {
    try {
      await shiftApi.publishWeek(weekStart)
      loadShifts()
      bumpNotifTick()
    } catch (e) {
      if (e.message && e.message.toLowerCase().includes('coverage')) {
        const ok = await confirm('Coverage warning', e.message, {
          confirmText: 'Publish all anyway', variant: 'warning',
        })
        if (ok) shiftApi.publishWeek(weekStart, true)
          .then(() => { loadShifts(); bumpNotifTick() })
          .catch(e2 => setError(e2.message))
      } else {
        setError(e.message)
      }
    }
  }

  // ── Render ───────────────────────────────────────────────────────────────────

  return (
    <div className={styles.page}>
      {/* Toolbar */}
      <div className={styles.toolbar}>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, -7))}>&#8592; Prev</button>
        <span className={styles.weekLabel}>{fmtRange(weekStart)}</span>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, 7))}>Next &#8594;</button>
        <div className={styles.toolbarSpacer} />
        {draftCount > 0 && (
          <button className={styles.publishWeekBtn} onClick={publishWeek}>
            Publish Week ({draftCount} draft{draftCount !== 1 ? 's' : ''})
          </button>
        )}
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

      {/* Weekly grid */}
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
                <select
                  required
                  value={formData.departmentId}
                  onChange={e => {
                    setFormData(f => ({ ...f, departmentId: e.target.value }))
                    setInlineEmpId('')
                    setInlineRoleId('')
                    setInlineEmpRoles([])
                  }}
                >
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

              {/* ── Assignments ── */}
              <div className={styles.assignSection}>
                <span className={styles.assignSectionLabel}>Assignments</span>

                {displayedAssignments.length > 0 && (
                  <div className={styles.assignList}>
                    {displayedAssignments.map(a => (
                      <div key={a.isNew ? a.tempId : a.id} className={styles.assignListRow}>
                        <span className={styles.assignListAvatar}>{initials(a.employeeName)}</span>
                        <span className={styles.assignListName}>{a.employeeName}</span>
                        {a.roleName && <span className={styles.assignListRole}>{a.roleName}</span>}
                        <button
                          type="button"
                          className={styles.assignListRemove}
                          onClick={() => removeDisplayedAssignment(a)}
                          title="Remove"
                        >&#x2715;</button>
                      </div>
                    ))}
                  </div>
                )}

                <div className={styles.assignAddRow}>
                  <select
                    value={inlineEmpId}
                    disabled={!formData.departmentId}
                    onChange={e => {
                      const id = e.target.value
                      setInlineEmpId(id)
                      setInlineRoleId('')
                      const emp = employees.find(em => String(em.id) === id)
                      setInlineEmpRoles(emp?.roles || [])
                      if (id) {
                        setAvailLoading(true)
                        availabilityApi.get(id)
                          .then(setInlineAvail)
                          .catch(() => setInlineAvail([]))
                          .finally(() => setAvailLoading(false))
                      } else {
                        setInlineAvail([])
                      }
                    }}
                  >
                    <option value="">Select employee…</option>
                    {eligibleEmps.map(e => (
                      <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>
                    ))}
                  </select>
                  <select
                    value={inlineRoleId}
                    disabled={!inlineEmpId}
                    onChange={e => setInlineRoleId(e.target.value)}
                  >
                    <option value="">Role…</option>
                    {filteredInlineRoles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                  </select>
                  <button
                    type="button"
                    className={styles.assignAddBtn}
                    disabled={!inlineEmpId || !inlineRoleId}
                    onClick={addInlineAssignment}
                  >+ Add</button>
                </div>

                {inlineAvailStatus && (
                  <div className={`${styles.availIndicator} ${
                    inlineAvailStatus === 'loading'     ? styles.availGrey   :
                    inlineAvailStatus === 'available'   ? styles.availGreen  :
                    inlineAvailStatus === 'partial'     ? styles.availYellow :
                    styles.availRed
                  }`}>
                    {inlineAvailStatus === 'loading'   && 'Checking availability…'}
                    {inlineAvailStatus === 'available' && '✓ Available for this shift'}
                    {inlineAvailStatus === 'partial'   && '~ Partially available for this shift'}
                    {inlineAvailStatus === 'unavailable' && '✕ Not available for this shift'}
                  </div>
                )}
              </div>

              <div className={styles.modalFooter}>
                <button type="button" onClick={() => setShiftModal(null)}>Cancel</button>
                <button type="submit" disabled={submitting}>{submitting ? 'Saving…' : 'Save Shift'}</button>
              </div>
            </form>
          </dialog>
        </div>
      )}
    </div>
  )
}

// ── Shift block card ──────────────────────────────────────────────────────────

function ShiftBlock({ shift: s, onEdit, onDelete, onTogglePublish, onUnassign }) {
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
        {s.published && (
          <button className={`${styles.actBtn} ${styles.unpublishBtn}`} onClick={() => onTogglePublish(s)}>
            Unpublish
          </button>
        )}
        <button className={`${styles.actBtn} ${styles.delBtn}`} onClick={() => onDelete(s.id)}>Del</button>
      </div>
    </div>
  )
}
