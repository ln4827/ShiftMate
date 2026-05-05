import { useState, useEffect } from 'react'
import { useAuth } from '../App'
import { swapApi } from '../api/client'
import styles from './SwapsPage.module.css'

const STATUS_LABEL = { PENDING: 'Pending', APPROVED: 'Approved', REJECTED: 'Rejected' }
const STATUS_CLASS  = { PENDING: styles.pending, APPROVED: styles.approved, REJECTED: styles.rejected }

export default function SwapsPage() {
  const { user } = useAuth()
  const isManager = user?.manager

  const [mySwaps, setMySwaps] = useState([])
  const [pending, setPending] = useState([])
  const [tab, setTab]         = useState('my')
  const [error, setError]     = useState('')

  useEffect(() => { loadMy() }, []) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { if (isManager && tab === 'pending') loadPending() }, [tab, isManager]) // eslint-disable-line react-hooks/exhaustive-deps

  const loadMy      = () => swapApi.getMy().then(data => setMySwaps(data ?? [])).catch(() => {})
  const loadPending = () => swapApi.getPending().then(data => setPending(data ?? [])).catch(e => setError(e.message))

  const approve = (id) => swapApi.approve(id).then(loadPending).catch(e => setError(e.message))
  const reject  = (id) => swapApi.reject(id).then(loadPending).catch(e => setError(e.message))

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className={styles.title}>Shift Swaps</h1>
      </div>

      {isManager && (
        <div className={styles.tabs}>
          <button
            className={tab === 'my' ? styles.tabActive : styles.tab}
            onClick={() => setTab('my')}
          >
            My Swaps
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

      {/* My swaps */}
      {tab === 'my' && (
        <div className={styles.section}>
          <h3>My Swap Requests</h3>
          <p className={styles.hint}>
            To request a swap, go to <strong>My Schedule</strong> and click &ldquo;Request Swap&rdquo; on a shift.
          </p>
          {mySwaps.length === 0
            ? <p className={styles.empty}>No swap requests yet.</p>
            : (
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Your Shift</th>
                    <th>Swap With</th>
                    <th>Their Shift</th>
                    <th>Status</th>
                    <th>Requested</th>
                  </tr>
                </thead>
                <tbody>
                  {mySwaps.map(s => {
                    const iAmRequester = s.requesterId === user?.id
                    return (
                      <tr key={s.id}>
                        <td>
                          {iAmRequester
                            ? <>{s.requesterShiftDate} {s.requesterShiftStart}–{s.requesterShiftEnd}<br /><em>{s.requesterShiftDepartment}</em></>
                            : <>{s.targetShiftDate} {s.targetShiftStart}–{s.targetShiftEnd}<br /><em>{s.targetShiftDepartment}</em></>
                          }
                        </td>
                        <td>{iAmRequester ? s.targetName : s.requesterName}</td>
                        <td>
                          {iAmRequester
                            ? <>{s.targetShiftDate} {s.targetShiftStart}–{s.targetShiftEnd}<br /><em>{s.targetShiftDepartment}</em></>
                            : <>{s.requesterShiftDate} {s.requesterShiftStart}–{s.requesterShiftEnd}<br /><em>{s.requesterShiftDepartment}</em></>
                          }
                        </td>
                        <td><span className={STATUS_CLASS[s.status]}>{STATUS_LABEL[s.status]}</span></td>
                        <td>{s.requestedAt?.slice(0, 10)}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
        </div>
      )}

      {/* Manager approval queue */}
      {tab === 'pending' && isManager && (
        <div className={styles.section}>
          <h3>Pending Swap Requests</h3>
          {pending.length === 0
            ? <p className={styles.empty}>No pending swap requests.</p>
            : (
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Requester</th>
                    <th>Their Shift</th>
                    <th>Target</th>
                    <th>Their Shift</th>
                    <th>Requested</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {pending.map(s => (
                    <tr key={s.id}>
                      <td>{s.requesterName}<br /><em>{s.requesterRoleName}</em></td>
                      <td>{s.requesterShiftDate}<br />{s.requesterShiftStart}–{s.requesterShiftEnd}<br /><em>{s.requesterShiftDepartment}</em></td>
                      <td>{s.targetName}<br /><em>{s.targetRoleName}</em></td>
                      <td>{s.targetShiftDate}<br />{s.targetShiftStart}–{s.targetShiftEnd}<br /><em>{s.targetShiftDepartment}</em></td>
                      <td>{s.requestedAt?.slice(0, 10)}</td>
                      <td>
                        <button className={styles.approveBtn} onClick={() => approve(s.id)}>Approve</button>
                        <button className={styles.rejectBtn}  onClick={() => reject(s.id)}>Reject</button>
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
