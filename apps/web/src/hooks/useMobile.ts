import { useEffect, useState } from 'react'

const MOBILE_QUERY = '(max-width: 767px)'

export function useMobile() {
  const [isMobile, setIsMobile] = useState(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return false
    return window.matchMedia(MOBILE_QUERY).matches
  })

  useEffect(() => {
    if (!window.matchMedia) return
    const media = window.matchMedia(MOBILE_QUERY)
    const update = () => setIsMobile(media.matches)

    update()
    media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [])

  return isMobile
}
