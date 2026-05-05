import { useState, useEffect } from 'react'
import { useAuth } from '../App'
import { timeOffApi } from '../api/client'
import styles from './TimeOffPage.module.css'

const STATUS_LABEL = { PENDING: 'Pending', APPROVED: 'Approved', REJECTED: 'Rejected' }
const STATUS_CLASS  = { PENDING: styles.pending, APPROVED: styles.approved, REJECTED: styles.rejected }

const BLANK = { startDate: '', endDate: '', reason: '' }

export default function TimeOffPage() {
  const { user } = useAuth()
  const isManager = user?.manager

  const [myRequests, setMyRequests]   = useState([])
  const [pending, setPending]         = useState([])
  const [tab, setTab]                 = useState(isManager ? 'pending' : 'my')
  const [form, setForm]               = useState(BLANK)
  const [submitting, setSubmitting]   = useState(false)
  const [formError, setFormError]     = useState('')
  const [formSuccess, setFormSuccess] = useState('')
  const [error, setError]             = useState('')

  useEffect(() => { loadMy() }, []) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { if (isManager) loadPending() }, [isManager]) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { if (isManager && tab === 'pending') loadPending() }, [tab, isManager]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const es = new EventSource('/api/notifications/stream')
    es.addEventListener('notification', (e) => {
      try {
        const { type } = JSON.parse(e.data)
        if (type === 'TIMEOFF_REQUESTED' || type === 'TIMEOFF_APPROVED' || type === 'TIMEOFF_REJECTED') {
          loadMy()
          if (isManager) loadPending()
        }
      } catch {}
    })
    return () => es.close()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const loadMy      = () => timeOffApi.getMy().then(setMyRequests).catch(() => {})
  const loadPending = () => timeOffApi.getPending().then(setPending).catch(e => setError(e.message))

  const submit = (e) => {
    e.preventDefault()
    setFormError('')
    setFormSuccess('')
    setSubmitting(true)
    timeOffApi.request(form)
      .then(() => {
        setForm(BLANK)
        setFormSuccess('Request submitted successfully.')
        loadMy()
        if (isManager) loadPending()
      })
      .catch(e => setFormError(e.message))
      .finally(() => setSubmitting(false))
  }

  const approve = (id) => {
    timeOffApi.approve(id).then(() => loadPending()).catch(e => setError(e.message))
  }

  const reject = (id) => {
    timeOffApi.reject(id).then(() => loadPending()).catch(e => setError(e.message))
  }

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className={styles.title}>Time Off</h1>
      </div>

      {isManager && (
        <div className={styles.tabs}>
          <button
            className={tab === 'my' ? styles.tabActive : styles.tab}
            onClick={() => setTab('my')}
          >
            My Requests
          </button>
          <button
            className={tab === 'pending' ? styles.tabActive : styles.tab}
            onClick={() => setTab('pending')}
          >
            Approval Queue
            {pending.length > 0 && <span className={styles.count}>{pending.length}</span>}
          </button>
        </div>
      )}

      {error && <div className={styles.errorBanner}>{error}</div>}

      {/* My requests tab */}
      {tab === 'my' && (
        <div className={styles.section}>
          <h3>Submit Request</h3>
          {formError   && <div className={styles.formError}>{formError}</div>}
          {formSuccess && <div className={styles.formSuccess}>{formSuccess}</div>}
          <form onSubmit={submit} className={styles.form}>
            <label htmlFor="to-start">Start date</label>
            <input
              id="to-start"
              type="date"
              required
              value={form.startDate}
              onChange={e => setForm(f => ({ ...f, startDate: e.target.value }))}
            />
            <label htmlFor="to-end">End date</label>
            <input
              id="to-end"
              type="date"
              required
              value={form.endDate}
              onChange={e => setForm(f => ({ ...f, endDate: e.target.value }))}
            />
            <label htmlFor="to-reason">Reason</label>
            <textarea
              id="to-reason"
              required
              rows={3}
              maxLength={500}
              value={form.reason}
              onChange={e => setForm(f => ({ ...f, reason: e.target.value }))}
            />
            <button type="submit" className={styles.submitBtn} disabled={submitting}>
              {submitting ? 'Submitting…' : 'Submit'}
            </button>
          </form>

          <h3 className={styles.listTitle}>My Requests</h3>
          {myRequests.length === 0
            ? <p className={styles.empty}>No requests yet.</p>
            : (
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Dates</th>
                    <th>Reason</th>
                    <th>Status</th>
                    <th>Resolved by</th>
                  </tr>
                </thead>
                <tbody>
                  {myRequests.map(r => (
                    <tr key={r.id}>
                      <td>{r.startDate} – {r.endDate}</td>
                      <td>{r.reason}</td>
                      <td><span className={STATUS_CLASS[r.status]}>{STATUS_LABEL[r.status]}</span></td>
                      <td>{r.resolvedByName || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
        </div>
      )}

      {/* Manager approval tab */}
      {tab === 'pending' && isManager && (
        <div className={styles.section}>
          <h3>Pending Requests</h3>
          {pending.length === 0
            ? <p className={styles.empty}>No pending requests.</p>
            : (
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Dates</th>
                    <th>Reason</th>
                    <th>Requested</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {pending.map(r => (
                    <tr key={r.id}>
                      <td>{r.employeeName}</td>
                      <td>{r.startDate} – {r.endDate}</td>
                      <td>{r.reason}</td>
                      <td>{r.requestedAt?.slice(0, 10)}</td>
                      <td>
                        <button className={styles.approveBtn} onClick={() => approve(r.id)}>Approve</button>
                        <button className={styles.rejectBtn}  onClick={() => reject(r.id)}>Reject</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
        </div>
      )}
    </div>
  )
}
