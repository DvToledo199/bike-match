import { useTranslation } from 'react-i18next'
import styles from './Layout.module.css'

function Layout({ children, session, onOpenRegister, onOpenLogin, onOpenMyBikes, onLogout }) {
  const { t } = useTranslation()

  return (
    <div className={styles.layout}>
      <a className={styles.skipLink} href="#main-content">{t('app.skipToContent')}</a>
      <header className={styles.header}>
        <span className={styles.logo}>{t('app.title')}</span>
        <div className={styles.accountActions}>
          {session ? (
            <>
              <button type="button" className={styles.accountButton} onClick={onOpenMyBikes}>
                {t('myBikes.navLabel')}
              </button>
              <span className={styles.sessionLabel}>{session.username}</span>
              <button type="button" className={styles.accountButton} onClick={onLogout}>
                {t('auth.logout')}
              </button>
            </>
          ) : (
            <>
              <button type="button" className={styles.accountButton} onClick={onOpenLogin}>
                {t('auth.openLogin')}
              </button>
              <button type="button" className={styles.accountButton} onClick={onOpenRegister}>
                {t('auth.openRegister')}
              </button>
            </>
          )}
        </div>
      </header>
      <main id="main-content" className={styles.main} tabIndex="-1">
        {children}
      </main>
    </div>
  )
}

export default Layout
