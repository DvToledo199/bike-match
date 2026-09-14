import { useEffect, useState } from 'react'

function readRoute() {
  const path = window.location.hash.slice(1)
  if (/^\/bikes\/[1-9]\d*$/.test(path)) return { screen: 'bikeDetail', bikeId: path.split('/')[2] }
  const screens = { '/': 'home', '/analyze': 'analysis', '/catalog': 'catalog', '/login': 'login', '/register': 'register', '/my-bikes': 'myBikes' }
  return { screen: screens[path] ?? 'home' }
}

export default function useAppRoute() {
  const [route, setRoute] = useState(readRoute)
  useEffect(() => {
    const update = () => setRoute(readRoute())
    window.addEventListener('hashchange', update)
    return () => window.removeEventListener('hashchange', update)
  }, [])
  return route
}
