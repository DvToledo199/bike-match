import { useTranslation } from 'react-i18next'
import HealthCheck from './components/HealthCheck.jsx'
import './App.css'

function App() {
  const { t } = useTranslation()

  return (
    <main className="app">
      <h1>{t('app.title')}</h1>
      <p className="tagline">{t('app.tagline')}</p>
      <HealthCheck />
    </main>
  )
}

export default App
