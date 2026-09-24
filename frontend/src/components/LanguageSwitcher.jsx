import { useTranslation } from 'react-i18next'
import { supportedLanguages } from '../i18n.js'
import styles from './LanguageSwitcher.module.css'

// Each language is named in itself, so a reader can find theirs whatever is on screen.
const OWN_NAMES = { en: 'English', es: 'Español' }

function LanguageSwitcher() {
  const { t, i18n } = useTranslation()
  return (
    <div className={styles.switcher} role="group" aria-label={t('language.label')}>
      {supportedLanguages.map((language) => (
        <button
          key={language}
          type="button"
          lang={language}
          aria-label={OWN_NAMES[language]}
          aria-pressed={i18n.resolvedLanguage === language}
          onClick={() => i18n.changeLanguage(language)}
        >
          {language.toUpperCase()}
        </button>
      ))}
    </div>
  )
}

export default LanguageSwitcher
