import i18n from 'i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import { initReactI18next } from 'react-i18next'
import en from './locales/en/translation.json'
import es from './locales/es/translation.json'

export const supportedLanguages = ['en', 'es']

// Keep <html lang> in step with the interface, so screen readers pronounce it correctly
// and the browser does not offer to translate a page that is already in the reader's language.
i18n.on('languageChanged', (language) => {
  document.documentElement.lang = language
})

// English is the base language. The detector uses the visitor's earlier choice first, then the
// browser language, and remembers any choice made with the language switcher.
i18n.use(LanguageDetector).use(initReactI18next).init({
  resources: {
    en: { translation: en },
    es: { translation: es },
  },
  supportedLngs: supportedLanguages,
  fallbackLng: 'en',
  detection: {
    order: ['localStorage', 'navigator'],
    caches: ['localStorage'],
    // es-ES, es-MX or en-GB all mean the same interface: keep only the language.
    convertDetectedLanguage: (language) => language.split('-')[0],
  },
  interpolation: {
    escapeValue: false, // React already escapes rendered strings
  },
})

export default i18n
