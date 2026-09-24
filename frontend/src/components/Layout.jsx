import { useTranslation } from 'react-i18next'
import LanguageSwitcher from './LanguageSwitcher.jsx'
import styles from './Layout.module.css'

function Layout({ children, session, screen, onLogout }) {
  const { t } = useTranslation()
  return (
    <div className={styles.layout}>
      <a className={styles.skipLink} href="#main-content" onClick={(event) => { event.preventDefault(); document.getElementById('main-content')?.focus() }}>{t('app.skipToContent')}</a>
      <header className={styles.header}>
        <a className={styles.logo} href="#/" aria-label={t('app.homeLabel')}>
          <span className={styles.logoMark} aria-hidden="true">/</span>{t('app.title')}
        </a>
        <nav className={styles.accountActions} aria-label={t('app.navigation')}>
          <a href="#/catalog" aria-current={screen === 'catalog' ? 'page' : undefined}>{t('catalog.navLabel')}</a>
          {session ? <>
            <a href="#/my-bikes" aria-current={screen === 'myBikes' ? 'page' : undefined}>{t('myBikes.navLabel')}</a>
            {(session.role === 'MODERATOR' || session.role === 'ADMIN') && (
              <a href="#/moderation" aria-current={screen === 'moderation' ? 'page' : undefined}>{t('moderation.navLabel')}</a>
            )}
            {session.role === 'ADMIN' && (
              <a href="#/admin" aria-current={screen === 'admin' ? 'page' : undefined}>{t('admin.navLabel')}</a>
            )}
            {session.username && <span className={styles.username}>@{session.username}</span>}
            <button type="button" onClick={onLogout}>{t('auth.logout')}</button>
          </> : <>
            <a href="#/login" aria-current={screen === 'login' ? 'page' : undefined}>{t('auth.openLogin')}</a>
            <a href="#/register" aria-current={screen === 'register' ? 'page' : undefined}>{t('auth.openRegister')}</a>
          </>}
          <a href="#/analyze" className={styles.analyzeLink} aria-current={screen === 'analysis' ? 'page' : undefined}>{t('app.analyze')}</a>
        </nav>
        <div className={styles.language}><LanguageSwitcher /></div>
      </header>
      <main id="main-content" className={styles.main} data-screen={screen} tabIndex="-1">{children}</main>
      <footer className={styles.footer}><span>{t('app.title')}</span><span>{t('app.footer')}</span></footer>
    </div>
  )
}

export default Layout
