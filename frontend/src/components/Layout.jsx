import { useTranslation } from 'react-i18next'
import styles from './Layout.module.css'

function Layout({ children }) {
  const { t } = useTranslation()

  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <span className={styles.logo}>{t('app.title')}</span>
      </header>
      <main className={styles.main}>
        {children}
      </main>
    </div>
  )
}

export default Layout
