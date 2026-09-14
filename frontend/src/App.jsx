import { useState } from 'react'
import Layout from './components/Layout.jsx'
import AnalysisWizard from './features/analysis-wizard/AnalysisWizard.jsx'
import RegisterForm from './features/auth/RegisterForm.jsx'

function App() {
  const [screen, setScreen] = useState('analysis')

  return (
    <Layout onOpenRegister={() => setScreen('register')}>
      {screen === 'register' ? (
        <RegisterForm onContinueAsGuest={() => setScreen('analysis')} />
      ) : (
        <AnalysisWizard />
      )}
    </Layout>
  )
}

export default App
