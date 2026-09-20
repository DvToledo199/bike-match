import { useEffect, useRef, useState } from 'react'
import Layout from './components/Layout.jsx'
import HomePage from './features/home/HomePage.jsx'
import AnalysisWizard from './features/analysis-wizard/AnalysisWizard.jsx'
import LoginForm from './features/auth/LoginForm.jsx'
import RegisterForm from './features/auth/RegisterForm.jsx'
import MyBikesPage from './features/my-bikes/MyBikesPage.jsx'
import BikeDetailPage from './features/my-bikes/BikeDetailPage.jsx'
import CatalogPage from './features/catalog/CatalogPage.jsx'
import ModerationPage from './features/moderation/ModerationPage.jsx'
import AdminPage from './features/admin/AdminPage.jsx'
import useAppRoute from './hooks/useAppRoute.js'
import { clearSession, getSession } from './services/session.js'

function currentPath() {
  return window.location.hash.slice(1) || '/'
}

function App() {
  const route = useAppRoute()
  const [detailOrigin, setDetailOrigin] = useState('/')
  const [session, setSession] = useState(getSession)
  const previousScreen = useRef(route.screen)
  // Last screen that was neither the login nor the registration, to return there after signing in.
  const returnPath = useRef('/')
  const navigate = (path) => { window.location.hash = path }

  useEffect(() => {
    if (previousScreen.current !== route.screen) {
      document.getElementById('main-content')?.focus()
      window.scrollTo?.(0, 0)
      previousScreen.current = route.screen
    }
  }, [route.screen])

  useEffect(() => {
    if (route.screen !== 'login' && route.screen !== 'register') {
      returnPath.current = currentPath()
    }
  }, [route])

  function startSession(nextSession) {
    setSession(nextSession)
    navigate(returnPath.current)
  }

  function logout() {
    clearSession()
    setSession(null)
    navigate('/')
  }

  function openBike(bikeId, origin) {
    setDetailOrigin(origin)
    navigate(`/bikes/${bikeId}`)
  }

  const needsLogin = route.screen === 'myBikes' && !session

  return (
    <Layout session={session} screen={route.screen} onLogout={logout}>
      {/* Keep the draft mounted so visiting the catalog or login preserves it. */}
      <div hidden={route.screen !== 'analysis'}>
        <AnalysisWizard active={route.screen === 'analysis'} session={session} onSaved={(id) => openBike(id, '/my-bikes')} />
      </div>
      {route.screen === 'home' && <HomePage onOpenBikeDetail={(id) => openBike(id, '/')} />}
      {route.screen === 'register' && <RegisterForm onRegistered={startSession} onContinueAsGuest={() => navigate('/analyze')} />}
      {(route.screen === 'login' || needsLogin) && (
        <LoginForm onLoggedIn={startSession} onContinueAsGuest={() => navigate('/analyze')} />
      )}
      {route.screen === 'myBikes' && session && <MyBikesPage onOpenBikeDetail={(id) => openBike(id, '/my-bikes')} />}
      {route.screen === 'catalog' && <CatalogPage onOpenBikeDetail={(id) => openBike(id, '/catalog')} />}
      {route.screen === 'moderation' && <ModerationPage session={session} />}
      {route.screen === 'admin' && <AdminPage session={session} />}
      {route.screen === 'bikeDetail' && <BikeDetailPage key={route.bikeId} bikeId={route.bikeId}
        backLabelKey={detailOrigin === '/my-bikes' ? 'bikeDetail.back' : detailOrigin === '/catalog' ? 'bikeDetail.backCatalog' : 'bikeDetail.backHome'}
        onBack={() => navigate(detailOrigin)} />}
    </Layout>
  )
}

export default App
