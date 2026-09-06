import { useTranslation } from 'react-i18next'
import styles from './Layout.module.css'

function Layout({ children }) {
  const { t } = useTranslation()

  return (
    <div className={styles.layout}>
      <a className={styles.skipLink} href="#main-content">{t('app.skipToContent')}</a>
      <header className={styles.header}>
        <span className={styles.logo}>{t('app.title')}</span>
      </header>
      <main id="main-content" className={styles.main} tabIndex="-1">
        {children}
      </main>
    </div>
  )
}

export default Layout
