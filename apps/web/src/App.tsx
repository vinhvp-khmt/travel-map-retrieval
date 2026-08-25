import { useCallback, useRef, useState } from 'react'
import { searchGeoapify } from './api/geoapify'
import { MapView } from './components/MapView'
import { PoiDetail } from './components/PoiDetail'
import { SearchForm } from './components/SearchForm'
import { AuthModal } from './components/AuthModal'
import { logout, type AuthSession } from './api/auth'
import type { SearchInput, SearchResponse, SearchResult, UserLocation } from './types/search'
import './App.css'

const EMPTY: SearchResponse = { normalizedQuery: '', page: 0, size: 20, total: 0, results: [], suggestion: null }
const PAGE_SIZE = 10

function toSearchResponse(input: SearchInput, external: SearchResult[]): SearchResponse {
  const seen = new Set<string>()
  const results = external.filter((item) => {
    const key = `${item.name.toLowerCase()}:${item.latitude.toFixed(4)}:${item.longitude.toFixed(4)}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
  return {
    normalizedQuery: input.query.trim().toLowerCase(),
    page: 0,
    size: 20,
    total: results.length,
    results,
    suggestion: results.length ? null : 'Không tìm thấy địa điểm thật quanh vị trí hiện tại. Hãy thử tăng bán kính hoặc đổi từ khóa.',
  }
}

function App() {
  const [data, setData] = useState<SearchResponse>(EMPTY)
  const [selected, setSelected] = useState<SearchResult>()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const [userLocation, setUserLocation] = useState<UserLocation>()
  const [currentPage, setCurrentPage] = useState(0)
  const abortRef = useRef<AbortController | null>(null)
  const [showAuth, setShowAuth] = useState(false)
  const [session, setSession] = useState<AuthSession | undefined>(() => {
    try { return JSON.parse(sessionStorage.getItem('travelmap.session') ?? 'null') ?? undefined }
    catch { return undefined }
  })

  function authenticated(next: AuthSession) {
    sessionStorage.setItem('travelmap.session', JSON.stringify(next)); setSession(next); setShowAuth(false)
  }

  async function signOut() {
    if (session) await logout(session.refreshToken).catch(() => undefined)
    sessionStorage.removeItem('travelmap.session'); setSession(undefined)
  }

  async function search(input: SearchInput) {
    abortRef.current?.abort()
    abortRef.current = new AbortController()
    setUserLocation({ latitude: input.latitude, longitude: input.longitude })
    setLoading(true); setError(undefined); setSelected(undefined); setCurrentPage(0)
    try {
      const external = await searchGeoapify(input, abortRef.current.signal)
      setData(toSearchResponse(input, external))
    }
    catch (reason) { if ((reason as Error).name !== 'AbortError') setError((reason as Error).message) }
    finally { setLoading(false) }
  }

  const selectPoi = useCallback((poi: SearchResult) => setSelected(poi), [])
  const totalPages = Math.max(1, Math.ceil(data.results.length / PAGE_SIZE))
  const visibleResults = data.results.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE)

  if (!session) {
    return (
      <main className="auth-page">
        <header className="topbar">
          <a className="brand" href="/" aria-label="TravelMap trang chủ"><span>TM</span> TravelMap</a>
          <p>Khám phá thành phố theo cách của bạn</p>
        </header>
        <section className="login-gate" aria-label="Đăng nhập TravelMap">
          <div className="login-gate-copy">
            <p className="eyebrow">BẢN ĐỒ DU LỊCH THÔNG MINH</p>
            <h1>Đăng nhập để vào TravelMap.</h1>
            <p>TravelMap cần tài khoản để lưu phiên làm việc, lấy vị trí hiện tại và trả kết quả địa điểm phù hợp quanh bạn.</p>
          </div>
          <AuthModal embedded onAuthenticated={authenticated} />
        </section>
      </main>
    )
  }

  return (
    <main>
      <header className="topbar">
        <a className="brand" href="/" aria-label="TravelMap trang chủ"><span>TM</span> TravelMap</a>
        <p>Khám phá thành phố theo cách của bạn</p>
        <div className="account-area">
          {session && <><span className="account-email">{session.email}</span><button className="logout-button" onClick={signOut}>Đăng xuất</button></>}
          <button className="profile-button" aria-label={session ? 'Tài khoản đã đăng nhập' : 'Đăng nhập hoặc đăng ký'} title={session?.email} onClick={() => setShowAuth(true)}>
            {session ? session.email.slice(0, 2).toUpperCase() : 'VN'}
          </button>
        </div>
      </header>
      <section className="search-shell">
        <div className="search-intro">
          <div><p className="eyebrow">BẢN ĐỒ DU LỊCH THÔNG MINH</p><h1>Tìm đúng nơi, <em>đúng lúc.</em></h1></div>
        </div>
        <SearchForm loading={loading} onSearch={search} onLocationChange={setUserLocation} />
      </section>
      <section className="explore-layout">
        <div className="results-panel">
          <div className="results-heading">
            <div><p className="eyebrow">KẾT QUẢ</p><h2>{data.total ? `${data.total} địa điểm` : 'Sẵn sàng khám phá'}</h2></div>
            {data.normalizedQuery && <span>“{data.normalizedQuery}”</span>}
          </div>
          {error && <div className="state-message error" role="alert">{error}</div>}
          {!error && data.results.length === 0 && <div className="state-message">
            <span className="compass">✦</span>
            <h3>{data.suggestion ? 'Chưa tìm thấy địa điểm' : 'Bắt đầu với một từ khóa'}</h3>
            <p>{data.suggestion ?? 'Bấm “Dùng vị trí của tôi”, sau đó thử “cafe”, “supermarket” hoặc “hotel”.'}</p>
          </div>}
          <div className="result-list">{visibleResults.map((poi, index) => (
            <button key={poi.poiId} className={`result-card ${selected?.poiId === poi.poiId ? 'selected' : ''}`} onClick={() => selectPoi(poi)}>
              <span className="result-index">{String(currentPage * PAGE_SIZE + index + 1).padStart(2, '0')}</span>
              <div><p className="eyebrow">{poi.category} · Geoapify</p><h3>{poi.name}</h3><p>{poi.address}</p></div>
              <div className="result-meta"><strong>{Math.round(poi.scoreDetail.finalScore * 100)}</strong><span>{Math.round(poi.distanceMeters)} m</span></div>
            </button>
          ))}</div>
          {data.results.length > PAGE_SIZE && <nav className="pagination" aria-label="Phân trang kết quả">
            <button disabled={currentPage === 0} onClick={() => { setCurrentPage((page) => page - 1); setSelected(undefined) }}>Trước</button>
            <span>Trang {currentPage + 1}/{totalPages} · Hiển thị {visibleResults.length}/{data.total}</span>
            <button disabled={currentPage >= totalPages - 1} onClick={() => { setCurrentPage((page) => page + 1); setSelected(undefined) }}>Sau</button>
          </nav>}
        </div>
        <div className="map-panel">
          <MapView results={visibleResults} selectedId={selected?.poiId} userLocation={userLocation} onSelect={selectPoi} />
          <div className="map-caption">Dữ liệu bản đồ © OpenStreetMap / Geoapify</div>
          {selected && <PoiDetail poi={selected} token={session.accessToken} onClose={() => setSelected(undefined)} />}
        </div>
      </section>
      {showAuth && <AuthModal onClose={() => setShowAuth(false)} onAuthenticated={authenticated} />}
    </main>
  )
}

export default App
