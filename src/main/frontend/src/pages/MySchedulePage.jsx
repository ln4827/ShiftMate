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

export default function MySchedulePage() {
  const { user } = useAuth()
  const [weekStart, setWeekStart] = useState(() => toMon(new Date()))
  const [myShifts, setMyShifts]         = useState([])
  const [allPublished, setAllPublished] = useState([])
  const [loading, setLoading]           = useState(false)
  const [error, setError]               = useState('')

  // Swap modal state
  const [swapModal, setSwapModal] = useState(null)   // { myAssignmentId, myShift }
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

  const myByDay = useMemo(() => {
    const map = {}
    for (let i = 0; i < 7; i++) map[toISO(addDays(weekStart, i))] = []
    myShifts.forEach(s => { if (map[s.shiftDate]) map[s.shiftDate].push(s) })
    return map
  }, [myShifts, weekStart])

  // Other employees' assignments the current user can swap with
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
      <div className={styles.toolbar}>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, -7))}>← Prev</button>
        <span className={styles.weekLabel}>{fmtRange(weekStart)}</span>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, 7))}>Next →</button>
      </div>

      {error && <div className={styles.errorBanner}>{error}</div>}
      {loading && <div className={styles.loading}>Loading…</div>}

      <div className={styles.grid}>
        {Object.entries(myByDay).map(([date, dayShifts], i) => (
          <div key={date} className={styles.dayCol}>
            <div className={styles.dayHeader}>{DAYS[i]} {date.slice(5)}</div>
            {dayShifts.length === 0 && <div className={styles.empty}>—</div>}
            {dayShifts.map(s => {
              const myAssId = myAssignmentId(s)
              const myRole  = s.assignments.find(a => a.employeeId === user?.id)?.roleName
              return (
                <div key={s.id} className={styles.card}>
                  <div className={styles.dept}>{s.departmentName}</div>
                  <div className={styles.time}>{s.startTime} – {s.endTime}</div>
                  {myRole && <div className={styles.role}>Role: <em>{myRole}</em></div>}
                  {myAssId && (
                    <button className={styles.swapBtn} onClick={() => openSwap(myAssId, s)}>
                      Request Swap
                    </button>
                  )}
                </div>
              )
            })}
          </div>
        ))}
      </div>

      {swapModal && (
        <div className={styles.overlay} onClick={() => setSwapModal(null)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>Request Swap</h3>
            <p className={styles.swapInfo}>
              Your shift: <strong>{swapModal.myShift.shiftDate}</strong> —&nbsp;
              {swapModal.myShift.departmentName}, {swapModal.myShift.startTime}–{swapModal.myShift.endTime}
            </p>
            {swapError && <div className={styles.modalError}>{swapError}</div>}
            <form onSubmit={submitSwap} className={styles.form}>
              <label>Swap with
                <select required value={swapTarget} onChange={e => setSwapTarget(e.target.value)}>
                  <option value="">Select a colleague&apos;s shift…</option>
                  {otherAssignments.map(a => (
                    <option key={a.id} value={a.id}>
                      {a.employeeName} — {a.shiftDate} {a.shiftStart}–{a.shiftEnd} ({a.dept})
                    </option>
                  ))}
                </select>
              </label>
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
          </div>
        </div>
      )}
    </div>
  )
}
