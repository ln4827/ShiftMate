import { useState, useEffect, useMemo } from 'react'
import { useAuth } from '../App'
import { shiftApi, swapApi } from '../api/client'
import styles from './MySchedulePage.module.css'

const toISO   = (d) => d.toISOString().split('T')[0]
const toMon   = (d) => { const c = new Date(d); c.setDate(d.getDate() - (d.getDay() === 0 ? 6 : d.getDay() - 1)); c.setHours(0,0,0,0); return c }
const addDays = (d, n) => { const c = new Date(d); c.setDate(d.getDate() + n); return c }
const fmtDate = (d) => d.toLocaleDateString('en-GB', { weekday: 'short', day: '2-digit', month: 'short' })
const fmtRange = (d) => `${fmtDate(d)} – ${fmtDate(addDays(d, 6))}`
const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const TODAY_ISO = toISO(new Date())

const DEPT_COLORS = ['#01B574', '#4318FF', '#7551FF', '#FF6B35', '#E31A1A', '#39B8FF']
const deptColor = (deptId) => DEPT_COLORS[(deptId - 1) % DEPT_COLORS.length] || DEPT_COLORS[0]

export default function MySchedulePage() {
  const { user } = useAuth()
  const [weekStart, setWeekStart] = useState(() => toMon(new Date()))
  const [myShifts, setMyShifts]         = useState([])
  const [allPublished, setAllPublished] = useState([])
  const [loading, setLoading]           = useState(false)
  const [error, setError]               = useState('')

  // Swap modal state
  const [swapModal, setSwapModal] = useState(null)
  const [swapTarget, setSwapTarget] = useState('')
  const [swapError, setSwapError]   = useState('')
  const [swapping, setSwapping]     = useState(false)

  useEffect(() => { load() }, [weekStart]) // eslint-disable-line react-hooks/exhaustive-deps

  const load = () => {
    setLoading(true)
    setError('')
    Promise.all([
      shiftApi.listMine(weekStart),
      shiftApi.listPublished(weekStart),
    ])
      .then(([mine, all]) => { setMyShifts(mine); setAllPublished(all) })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }

  const weekDates = useMemo(() =>
    Array.from({ length: 7 }, (_, i) => toISO(addDays(weekStart, i))),
    [weekStart]
  )

  const myByDay = useMemo(() => {
    const map = {}
    weekDates.forEach(d => { map[d] = [] })
    myShifts.forEach(s => { if (map[s.shiftDate]) map[s.shiftDate].push(s) })
    return map
  }, [myShifts, weekDates])

  const otherAssignments = useMemo(() => {
    const myId = user?.id
    return allPublished.flatMap(s =>
      s.assignments
        .filter(a => a.employeeId !== myId)
        .map(a => ({ ...a, shiftDate: s.shiftDate, shiftStart: s.startTime, shiftEnd: s.endTime, dept: s.departmentName }))
    )
  }, [allPublished, user])

  const openSwap = (assignmentId, shift) => {
    setSwapTarget('')
    setSwapError('')
    setSwapModal({ myAssignmentId: assignmentId, myShift: shift })
  }

  const submitSwap = (e) => {
    e.preventDefault()
    if (!swapTarget) return
    setSwapping(true)
    setSwapError('')
    swapApi.request({ requesterAssignmentId: swapModal.myAssignmentId, targetAssignmentId: Number(swapTarget) })
      .then(() => { setSwapModal(null); alert('Swap request submitted — awaiting manager approval.') })
      .catch(e => setSwapError(e.message))
      .finally(() => setSwapping(false))
  }

  const myAssignmentId = (shift) =>
    shift.assignments.find(a => a.employeeId === user?.id)?.id

  return (
    <div className={styles.page}>
      {/* Toolbar */}
      <div className={styles.toolbar}>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, -7))}>&#8592; Prev</button>
        <span className={styles.weekLabel}>{fmtRange(weekStart)}</span>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, 7))}>Next &#8594;</button>
      </div>

      {error && <div className={styles.errorBanner}>{error}</div>}
      {loading && <div className={styles.loading}>Loading…</div>}

      <div className={styles.gridWrapper}>
        <div className={styles.grid}>
          {weekDates.map((date, i) => {
            const dayShifts = myByDay[date] || []
            const isToday = date === TODAY_ISO
            return (
              <div key={date} className={styles.dayCol}>
                <div className={styles.dayHeader}>
                  <span className={styles.dayHeaderName}>{DAYS[i]}</span>
                  {isToday
                    ? <span className={styles.dayHeaderDateToday}>{date.slice(8)}</span>
                    : <span className={styles.dayHeaderDate}>{date.slice(8)}</span>
                  }
                </div>

                {dayShifts.length === 0 && (
                  <div className={styles.empty}>No shifts</div>
                )}

                {dayShifts.map(s => {
                  const myAssId = myAssignmentId(s)
                  const myRole  = s.assignments.find(a => a.employeeId === user?.id)?.roleName
                  const color   = deptColor(s.departmentId)
                  return (
                    <div key={s.id} className={styles.card}>
                      <div className={styles.shiftColorBar} style={{ background: color }} />
                      <div className={styles.dept}>{s.departmentName}</div>
                      <div className={styles.time}>{s.startTime} – {s.endTime}</div>
                      {myRole && (
                        <div className={styles.role}>Role: <em>{myRole}</em></div>
                      )}
                      {myAssId && (
                        <button className={styles.swapBtn} onClick={() => openSwap(myAssId, s)}>
                          Request Swap
                        </button>
                      )}
                    </div>
                  )
                })}
              </div>
            )
          })}
        </div>
      </div>

      {/* Swap modal */}
      {swapModal && (
        <div className={styles.overlay}>
          <dialog open className={styles.modal} aria-labelledby="swap-modal-title">
            <div className={styles.modalHeader}>
              <h3 id="swap-modal-title">Request Swap</h3>
              <button className={styles.modalClose} onClick={() => setSwapModal(null)}>&#x2715;</button>
            </div>
            <p className={styles.swapInfo}>
              Your shift: <strong>{swapModal.myShift.shiftDate}</strong> —&nbsp;
              {swapModal.myShift.departmentName}, {swapModal.myShift.startTime}–{swapModal.myShift.endTime}
            </p>
            {swapError && <div className={styles.modalError}>{swapError}</div>}
            <form onSubmit={submitSwap} className={styles.form}>
              <label htmlFor="swap-target-select">Swap with</label>
              <select
                id="swap-target-select"
                required
                value={swapTarget}
                onChange={e => setSwapTarget(e.target.value)}
              >
                <option value="">Select a colleague&apos;s shift…</option>
                {otherAssignments.map(a => (
                  <option key={a.id} value={a.id}>
                    {a.employeeName} — {a.shiftDate} {a.shiftStart}–{a.shiftEnd} ({a.dept})
                  </option>
                ))}
              </select>
              {otherAssignments.length === 0 && (
                <p className={styles.noTargets}>No other published assignments this week.</p>
              )}
              <div className={styles.modalFooter}>
                <button type="button" onClick={() => setSwapModal(null)}>Cancel</button>
                <button type="submit" disabled={swapping || !swapTarget}>
                  {swapping ? 'Submitting…' : 'Submit Request'}
                </button>
              </div>
            </form>
          </dialog>
        </div>
      )}
    </div>
  )
}
