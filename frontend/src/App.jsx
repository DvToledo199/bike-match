import { useState } from 'react'
import Layout from './components/Layout.jsx'
import AnalysisWizard from './features/analysis-wizard/AnalysisWizard.jsx'
import LoginForm from './features/auth/LoginForm.jsx'
import RegisterForm from './features/auth/RegisterForm.jsx'
import MyBikesPage from './features/my-bikes/MyBikesPage.jsx'
import BikeDetailPage from './features/my-bikes/BikeDetailPage.jsx'
import { clearSession, getSession } from './services/session.js'

function App() {
  const [screen, setScreen] = useState('analysis')
  const [selectedBikeId, setSelectedBikeId] = useState(null)
  const [session, setSession] = useState(getSession)

  function logout() {
    clearSession()
    setSession(null)
    setSelectedBikeId(null)
    setScreen('analysis')
  }

  return (
    <Layout
      session={session}
      onOpenRegister={() => setScreen('register')}
      onOpenLogin={() => setScreen('login')}
      onOpenMyBikes={() => setScreen('myBikes')}
      onLogout={logout}
    >
      {screen === 'register' ? (
        <RegisterForm onContinueAsGuest={() => setScreen('analysis')} />
      ) : screen === 'login' ? (
        <LoginForm
          onLoggedIn={(nextSession) => { setSession(nextSession); setScreen('analysis') }}
          onContinueAsGuest={() => setScreen('analysis')}
        />
      ) : screen === 'myBikes' ? (
        <MyBikesPage onOpenBikeDetail={(bikeId) => { setSelectedBikeId(bikeId); setScreen('bikeDetail') }} />
      ) : screen === 'bikeDetail' ? (
        <BikeDetailPage bikeId={selectedBikeId} onBack={() => setScreen('myBikes')} />
      ) : (
        <AnalysisWizard />
      )}
    </Layout>
  )
}

export default App
