import { useState, useEffect, useRef } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import PropTypes from 'prop-types'
import { useAuth } from '../App'
import { authApi, notificationApi } from '../api/client'
import styles from './NavBar.module.css'

export default function NavBar() {
  const { user, setUser } = useAuth()
  const navigate = useNavigate()
  const isManager = user?.manager

  const [unread, setUnread]         = useState(0)
  const [notifications, setNotifs]  = useState([])
  const [bellOpen, setBellOpen]     = useState(false)
  const bellRef = useRef(null)

  // Poll unread count every 30 s
  useEffect(() => {
    let cancelled = false
    const poll = () => {
      notificationApi.unreadCount()
        .then(d => { if (!cancelled) setUnread(d.count) })
        .catch(() => {})
    }
    poll()
    const id = setInterval(poll, 30_000)
    return () => { cancelled = true; clearInterval(id) }
  }, [])

  // Close bell dropdown on outside click
  useEffect(() => {
    const handler = (e) => {
      if (bellRef.current && !bellRef.current.contains(e.target)) {
        setBellOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const openBell = () => {
    if (!bellOpen) {
      notificationApi.list()
        .then(setNotifs)
        .catch(() => {})
    }
    setBellOpen(v => !v)
  }

  const markAllRead = () => {
    notificationApi.markAllRead()
      .then(() => {
        setUnread(0)
        setNotifs(ns => ns.map(n => ({ ...n, read: true })))
      })
      .catch(() => {})
  }

  const markOne = (id) => {
    notificationApi.markRead(id)
      .then(() => {
        setNotifs(ns => ns.map(n => n.id === id ? { ...n, read: true } : n))
        setUnread(u => Math.max(0, u - 1))
      })
      .catch(() => {})
  }

  const logout = () => {
    authApi.logout().finally(() => {
      setUser(null)
      navigate('/login')
    })
  }

  return (
    <nav className={styles.nav}>
      <span className={styles.brand}>ShiftMate</span>

      <div className={styles.links}>
        {isManager && <NavLink to="/schedule" className={navClass}>Schedule</NavLink>}
        <NavLink to="/my-schedule" className={navClass}>My Schedule</NavLink>
        {isManager && <NavLink to="/employees" className={navClass}>Employees</NavLink>}
        <NavLink to="/time-off" className={navClass}>Time Off</NavLink>
        <NavLink to="/swaps" className={navClass}>Swaps</NavLink>
        {isManager && <NavLink to="/reports" className={navClass}>Reports</NavLink>}
      </div>

      <div className={styles.right}>
        {/* Notification bell */}
        <div className={styles.bellWrap} ref={bellRef}>
          <button className={styles.bell} onClick={openBell} title="Notifications">
            🔔
            {unread > 0 && <span className={styles.badge}>{unread > 99 ? '99+' : unread}</span>}
          </button>

          {bellOpen && (
            <div className={styles.dropdown}>
              <div className={styles.dropHeader}>
                <span>Notifications</span>
                {unread > 0 && (
                  <button className={styles.markAll} onClick={markAllRead}>Mark all read</button>
                )}
              </div>
              <ul className={styles.notifList}>
                {notifications.length === 0 && (
                  <li className={styles.empty}>No notifications</li>
                )}
                {notifications.map(n => (
                  <li
                    key={n.id}
                    className={n.read ? styles.notifRead : styles.notifUnread}
                    onClick={() => !n.read && markOne(n.id)}
                  >
                    <span className={styles.notifMsg}>{n.message}</span>
                    <span className={styles.notifTime}>{fmtTime(n.createdAt)}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        <span className={styles.userLabel}>{user?.firstName}</span>
        <button className={styles.logoutBtn} onClick={logout}>Logout</button>
      </div>
    </nav>
  )
}

NavBar.propTypes = {}

const navClass = ({ isActive }) =>
  isActive ? `${styles.link} ${styles.active}` : styles.link

function fmtTime(isoStr) {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' })
    + ' ' + d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
}
