import { useState, useEffect } from 'react'
import { useAuth } from '../App'
import { availabilityApi } from '../api/client'
import styles from './AvailabilityPage.module.css'

const DAYS = [
  { value: 1, label: 'Monday' },
  { value: 2, label: 'Tuesday' },
  { value: 3, label: 'Wednesday' },
  { value: 4, label: 'Thursday' },
  { value: 5, label: 'Friday' },
  { value: 6, label: 'Saturday' },
  { value: 7, label: 'Sunday' },
]

function emptySchedule() {
  return Object.fromEntries(
    DAYS.map(d => [d.value, { available: false, startTime: '09:00', endTime: '17:00' }])
  )
}

export default function AvailabilityPage() {
  const { user } = useAuth()
  const [schedule, setSchedule] = useState(emptySchedule())
  const [loading, setLoading]   = useState(true)
  const [saving, setSaving]     = useState(false)
  const [error, setError]       = useState('')
  const [success, setSuccess]   = useState(false)

  useEffect(() => {
    availabilityApi.get(user.employeeId)
      .then(windows => {
        const next = emptySchedule()
        windows.forEach(w => {
          next[w.dayOfWeek] = {
            available: true,
            startTime: w.startTime.slice(0, 5),
            endTime:   w.endTime.slice(0, 5),
          }
        })
        setSchedule(next)
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [user.employeeId])

  function toggle(day) {
    setSchedule(s => ({ ...s, [day]: { ...s[day], available: !s[day].available } }))
  }

  function setTime(day, field, value) {
    setSchedule(s => ({ ...s, [day]: { ...s[day], [field]: value } }))
  }

  async function handleSave(e) {
    e.preventDefault()
    setError('')
    setSuccess(false)
    setSaving(true)
    const windows = DAYS
      .filter(d => schedule[d.value].available)
      .map(d => ({
        dayOfWeek: d.value,
        startTime: schedule[d.value].startTime,
        endTime:   schedule[d.value].endTime,
      }))
    try {
      await availabilityApi.set(user.employeeId, windows)
      setSuccess(true)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className={styles.page}><p className={styles.state}>Loading…</p></div>

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className={styles.title}>My Availability</h1>
        <p className={styles.subtitle}>Set the days and times you are available to work each week.</p>
      </div>

      {error   && <div className={styles.errorBanner}>{error}</div>}
      {success && <div className={styles.successBanner}>Availability saved.</div>}

      <form onSubmit={handleSave}>
        <div className={styles.card}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Day</th>
                <th>Available</th>
                <th>From</th>
                <th>To</th>
              </tr>
            </thead>
            <tbody>
              {DAYS.map(d => {
                const row = schedule[d.value]
                return (
                  <tr key={d.value} className={row.available ? styles.rowActive : styles.rowInactive}>
                    <td className={styles.dayName}>{d.label}</td>
                    <td>
                      <label className={styles.toggle}>
                        <input
                          type="checkbox"
                          checked={row.available}
                          onChange={() => toggle(d.value)}
                        />
                        <span className={styles.toggleSlider} />
                      </label>
                    </td>
                    <td>
                      <input
                        type="time"
                        className={styles.timeInput}
                        value={row.startTime}
                        disabled={!row.available}
                        onChange={e => setTime(d.value, 'startTime', e.target.value)}
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        className={styles.timeInput}
                        value={row.endTime}
                        disabled={!row.available}
                        onChange={e => setTime(d.value, 'endTime', e.target.value)}
                      />
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        <div className={styles.footer}>
          <button type="submit" className={styles.saveBtn} disabled={saving}>
            {saving ? 'Saving…' : 'Save Availability'}
          </button>
        </div>
      </form>
    </div>
  )
}
