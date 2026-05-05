import { createContext, useContext, useState, useCallback } from 'react'
import PropTypes from 'prop-types'
import styles from './ConfirmDialog.module.css'

const ConfirmContext = createContext(null)

export function ConfirmProvider({ children }) {
  const [dialog, setDialog] = useState(null)

  const confirm = useCallback((title, message, { confirmText = 'Confirm', cancelText = 'Cancel', variant = 'default' } = {}) => {
    return new Promise(resolve => {
      setDialog({ title, message, confirmText, cancelText, variant, resolve })
    })
  }, [])

  const close = (result) => {
    dialog?.resolve(result)
    setDialog(null)
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {dialog && (
        <div className={styles.overlay} onClick={() => close(false)}>
          <dialog
            open
            className={styles.dialog}
            onClick={e => e.stopPropagation()}
            aria-labelledby="confirm-title"
            aria-describedby={dialog.message ? 'confirm-message' : undefined}
          >
            <h3 id="confirm-title" className={styles.title}>{dialog.title}</h3>
            {dialog.message && (
              <p id="confirm-message" className={styles.message}>{dialog.message}</p>
            )}
            <div className={styles.actions}>
              <button className={styles.cancelBtn} onClick={() => close(false)}>
                {dialog.cancelText}
              </button>
              <button
                className={`${styles.confirmBtn} ${styles[dialog.variant] || ''}`}
                onClick={() => close(true)}
              >
                {dialog.confirmText}
              </button>
            </div>
          </dialog>
        </div>
      )}
    </ConfirmContext.Provider>
  )
}

ConfirmProvider.propTypes = { children: PropTypes.node.isRequired }

export const useConfirm = () => useContext(ConfirmContext)
