import { useState, useEffect } from 'react'
import { reportApi } from '../api/client'
import styles from './ReportsPage.module.css'

const toMon   = (d) => { const c = new Date(d); c.setDate(d.getDate() - (d.getDay() === 0 ? 6 : d.getDay() - 1)); c.setHours(0,0,0,0); return c }
const addDays = (d, n) => { const c = new Date(d); c.setDate(d.getDate() + n); return c }
const toISO   = (d) => d.toISOString().split('T')[0]
const fmtDate = (d) => d.toLocaleDateString('en-GB', { weekday: 'short', day: '2-digit', month: 'short' })
const fmtRange = (d) => `${fmtDate(d)} – ${fmtDate(addDays(d, 6))}`

export default function ReportsPage() {
  const [weekStart, setWeekStart] = useState(() => toMon(new Date()))
  const [rows, setRows]           = useState([])
  const [loading, setLoading]     = useState(false)
  const [error, setError]         = useState('')

  useEffect(() => { load() }, [weekStart]) // eslint-disable-line react-hooks/exhaustive-deps

  const load = () => {
    setLoading(true)
    setError('')
    reportApi.hours(weekStart)
      .then(setRows)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }

  const totalHours = rows.reduce((s, r) => s + r.totalHours, 0)

  return (
    <div className={styles.page}>
      <h2 className={styles.title}>Hours Report</h2>

      <div className={styles.toolbar}>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, -7))}>← Prev</button>
        <span className={styles.weekLabel}>{fmtRange(weekStart)}</span>
        <button className={styles.weekBtn} onClick={() => setWeekStart(d => addDays(d, 7))}>Next →</button>
      </div>

      {error && <div className={styles.errorBanner}>{error}</div>}

      <div className={styles.card}>
        {loading
          ? <p className={styles.loading}>Loading…</p>
          : rows.length === 0
            ? <p className={styles.empty}>No shifts worked this week.</p>
            : (
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Employee</th>
                    <th>Hours Worked</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r, i) => (
                    <tr key={r.employeeId}>
                      <td className={styles.num}>{i + 1}</td>
                      <td>{r.employeeName}</td>
                      <td className={styles.hours}>{r.totalHours.toFixed(1)} h</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={2} className={styles.totalLabel}>Total</td>
                    <td className={styles.totalValue}>{totalHours.toFixed(1)} h</td>
                  </tr>
                </tfoot>
              </table>
            )}
      </div>
    </div>
  )
}
