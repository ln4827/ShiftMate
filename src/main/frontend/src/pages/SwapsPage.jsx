import { useState, useEffect } from 'react'
import { useAuth } from '../App'
import { swapApi, shiftApi } from '../api/client'
import styles from './SwapsPage.module.css'

const STATUS_LABEL = { PENDING: 'Pending', APPROVED: 'Approved', REJECTED: 'Rejected' }
const STATUS_CLASS = { PENDING: styles.pending, APPROVED: styles.approved, REJECTED: styles.rejected }

export default function SwapsPage() {
  const { user } = useAuth()
  const isManager = user?.manager

  const [mySwaps, setMySwaps] = useState([])
  const [pending, setPending] = useState([])
  const [tab, setTab] = useState('my')
  const [error, setError] = useState('')

  // Create Request State
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [myShifts, setMyShifts] = useState([])
  const [otherShifts, setOtherShifts] = useState([])
  const [createData, setCreateData] = useState({ requesterAssignmentId: '', targetAssignmentId: '' })

  useEffect(() => { loadMy() }, [])
  useEffect(() => { if (isManager && tab === 'pending') loadPending() }, [tab, isManager])

  const loadMy = () => swapApi.getMy().then(data => setMySwaps(data ?? [])).catch(() => { })
  const loadPending = () => swapApi.getPending().then(data => setPending(data ?? [])).catch(e => setError(e.message))

  const openCreateModal = async () => {
    setError('')
    try {
      const today = new Date();
      const my = await shiftApi.listMine(today);
      const all = await shiftApi.listPublished(today);

      // DEBUG: Check what the user object and API response actually look like
      console.log("Current User:", user);
      console.log("My Shifts from API:", my);

      // Extract user ID safely (handle both user.id and user.employeeId if applicable)
      const currentUserId = Number(user?.id || user?.employeeId);

      // 1. Map My Shifts
      // We use == instead of === to handle string vs number, or cast both to Number
      const mappedMyShifts = my.flatMap(s =>
        (s.assignments || [])
          .filter(a => Number(a.employeeId) === currentUserId)
          .map(a => ({
            ...a,
            display: `${s.shiftDate} (${s.startTime.slice(0, 5)}) - ${s.departmentName}`,
            shift: s
          }))
      );

      // 2. Map Other Shifts
      const mappedOtherShifts = all.flatMap(s =>
        (s.assignments || [])
          .filter(a => Number(a.employeeId) !== currentUserId)
          .map(a => ({
            ...a,
            display: `${a.employeeName}: ${s.shiftDate} (${s.startTime.slice(0, 5)})`,
            shift: s
          }))
      );

      setMyShifts(mappedMyShifts);
      setOtherShifts(mappedOtherShifts);

      if (mappedMyShifts.length === 0) {
        console.warn("No assignments found for User ID:", currentUserId);
      }

      setIsCreateOpen(true);
    } catch (err) {
      console.error("Swap Modal Error:", err);
      setError("Failed to load available shifts: " + err.message);
    }
  }

  const handleCreateSubmit = async (e) => {
    e.preventDefault()
    try {
      await swapApi.request(createData)
      setIsCreateOpen(false)
      setCreateData({ requesterAssignmentId: '', targetAssignmentId: '' })
      loadMy()
    } catch (err) {
      alert(err.message)
    }
  }

  const approve = (id) => swapApi.approve(id).then(loadPending).catch(e => setError(e.message))
  const reject = (id) => swapApi.reject(id).then(loadPending).catch(e => setError(e.message))

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <h1 className={styles.title}>Shift Swaps</h1>
        <button className={styles.addBtn} onClick={openCreateModal}>
          + New Swap Request
        </button>
      </div>

      {isManager && (
        <div className={styles.tabs}>
          <button className={tab === 'my' ? styles.tabActive : styles.tab} onClick={() => setTab('my')}>
            My Swaps
          </button>
          <button className={tab === 'pending' ? styles.tabActive : styles.tab} onClick={() => setTab('pending')}>
            Approval Queue
            {pending.length > 0 && <span className={styles.count}>{pending.length}</span>}
          </button>
        </div>
      )}

      {error && <div className={styles.errorBanner}>{error}</div>}

      {tab === 'my' && (
        <div className={styles.section}>
          <h3>My Swap Requests</h3>
          {mySwaps.length === 0 ? <p className={styles.empty}>No swap requests yet.</p> : (
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
                          ? <>{s.requesterShiftDate} {s.requesterShiftStart}<br /><em>{s.requesterShiftDepartment}</em></>
                          : <>{s.targetShiftDate} {s.targetShiftStart}<br /><em>{s.targetShiftDepartment}</em></>
                        }
                      </td>
                      <td>{iAmRequester ? s.targetName : s.requesterName}</td>
                      <td>
                        {iAmRequester
                          ? <>{s.targetShiftDate} {s.targetShiftStart}<br /><em>{s.targetShiftDepartment}</em></>
                          : <>{s.requesterShiftDate} {s.requesterShiftStart}<br /><em>{s.requesterShiftDepartment}</em></>
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

      {/* Approval Queue for Managers */}
      {tab === 'pending' && isManager && (
        <div className={styles.section}>
          <h3>Pending Swap Requests</h3>
          {pending.length === 0 ? <p className={styles.empty}>No pending swap requests.</p> : (
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Requester</th>
                  <th>Their Shift</th>
                  <th>Target</th>
                  <th>Their Shift</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pending.map(s => (
                  <tr key={s.id}>
                    <td>{s.requesterName}<br /><em>{s.requesterRoleName}</em></td>
                    <td>{s.requesterShiftDate}<br />{s.requesterShiftStart}</td>
                    <td>{s.targetName}<br /><em>{s.targetRoleName}</em></td>
                    <td>{s.targetShiftDate}<br />{s.targetShiftStart}</td>
                    <td>
                      <button className={styles.approveBtn} onClick={() => approve(s.id)}>Approve</button>
                      <button className={styles.rejectBtn} onClick={() => reject(s.id)}>Reject</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* NEW REQUEST MODAL */}
      {isCreateOpen && (
        <div className={styles.modalOverlay}>
          <div className={styles.modal}>
            <div className={styles.modalHeader}>
              <h2>Request a Shift Swap</h2>
              <button onClick={() => setIsCreateOpen(false)} className={styles.closeBtn}>&times;</button>
            </div>
            <form onSubmit={handleCreateSubmit} className={styles.form}>
              <div className={styles.field}>
                <label>Select Your Shift</label>
                <select
                  required
                  value={createData.requesterAssignmentId}
                  onChange={e => setCreateData({ ...createData, requesterAssignmentId: e.target.value })}
                >
                  <option value="">-- Choose one of your shifts --</option>
                  {myShifts.map(a => (
                    <option key={a.id} value={a.id}>
                      {a.display}
                    </option>
                  ))}
                </select>
              </div>

              <div className={styles.field}>
                <label>Select Coworker's Shift to Swap With</label>
                <select
                  required
                  value={createData.targetAssignmentId}
                  onChange={e => setCreateData({ ...createData, targetAssignmentId: e.target.value })}
                >
                  <option value="">-- Choose a coworker's shift --</option>
                  {otherShifts.map(a => (
                    <option key={a.id} value={a.id}>
                      {a.display}
                    </option>
                  ))}
                </select>
              </div>

              <div className={styles.formActions}>
                <button type="button" onClick={() => setIsCreateOpen(false)} className={styles.cancelBtn}>Cancel</button>
                <button type="submit" className={styles.submitBtn}>Submit Request</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}