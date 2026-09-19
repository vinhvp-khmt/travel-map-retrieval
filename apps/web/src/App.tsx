import { useCallback, useEffect, useRef, useState } from 'react'
import { AlertCircle, Coffee, Compass, Eye, History, LogOut, MapPinned, Navigation, UserRound } from 'lucide-react'
import { searchPois } from './api/search'
import { MapView } from './components/MapView'
import { PoiDetail, type PoiDetailSubject } from './components/PoiDetail'
import { SearchForm } from './components/SearchForm'
import { AuthModal } from './components/AuthModal'
import { logout, type AuthSession } from './api/auth'
import { fetchSearchHistory, fetchViewedPois, recordPoiView } from './api/history'
import { distanceMeters } from './lib/geo'
import { useMobile } from './hooks/useMobile'
import type { SearchHistoryItem, SearchInput, SearchResponse, UserLocation, ViewedPoiItem } from './types/search'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardDescription, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'
import './App.css'

const EMPTY: SearchResponse = { normalizedQuery: '', page: 0, size: 20, total: 0, results: [], suggestion: null }
const PAGE_SIZE = 10

function App() {
  const isMobile = useMobile()
  const [data, setData] = useState<SearchResponse>(EMPTY)
  const [selected, setSelected] = useState<PoiDetailSubject>()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string>()
  const [userLocation, setUserLocation] = useState<UserLocation>()
  const [currentPage, setCurrentPage] = useState(0)
  const abortRef = useRef<AbortController | null>(null)
  const [showAuth, setShowAuth] = useState(false)
  const [logoutLoading, setLogoutLoading] = useState(false)
  const [session, setSession] = useState<AuthSession | undefined>(() => {
    try { return JSON.parse(sessionStorage.getItem('travelmap.session') ?? 'null') ?? undefined }
    catch { return undefined }
  })
  const [searchHistory, setSearchHistory] = useState<SearchHistoryItem[]>([])
  const [viewedPois, setViewedPois] = useState<ViewedPoiItem[]>([])

  // Phase 6/7: nạp "tìm kiếm gần đây" + "địa điểm đã xem" khi đăng nhập (và mỗi khi đổi
  // tài khoản). Lỗi mạng ở đây không nên chặn phần còn lại của trang — chỉ để danh sách rỗng.
  useEffect(() => {
    if (!session) return
    fetchSearchHistory(session.accessToken).then(setSearchHistory).catch(() => undefined)
    fetchViewedPois(session.accessToken).then(setViewedPois).catch(() => undefined)
  }, [session])

  function authenticated(next: AuthSession) {
    sessionStorage.setItem('travelmap.session', JSON.stringify(next)); setSession(next); setShowAuth(false)
  }

  async function signOut() {
    setLogoutLoading(true)
    try {
      if (session) await logout(session.refreshToken).catch(() => undefined)
      sessionStorage.removeItem('travelmap.session'); setSession(undefined)
      setSearchHistory([]); setViewedPois([])
    } finally {
      setLogoutLoading(false)
    }
  }

  async function search(input: SearchInput) {
    abortRef.current?.abort()
    abortRef.current = new AbortController()
    setUserLocation({ latitude: input.latitude, longitude: input.longitude })
    setLoading(true); setError(undefined); setSelected(undefined); setCurrentPage(0)
    try {
      setData(await searchPois(input, abortRef.current.signal))
      // Backend tự ghi lịch sử khi request có token; nạp lại danh sách để "Tìm kiếm gần
      // đây" phản ánh đúng ngay, không cần tải lại trang.
      if (session) fetchSearchHistory(session.accessToken).then(setSearchHistory).catch(() => undefined)
    }
    catch (reason) { if ((reason as Error).name !== 'AbortError') setError((reason as Error).message) }
    finally { setLoading(false) }
  }

  /** Bấm một mục "Tìm kiếm gần đây" thì search lại đúng truy vấn + vị trí đã lưu. */
  function searchFromHistory(item: SearchHistoryItem) {
    search({ query: item.query, latitude: item.latitude, longitude: item.longitude, radiusKm: item.radiusKm })
  }

  const selectPoi = useCallback((poi: PoiDetailSubject) => {
    setSelected(poi)
    if (!session) return
    // Fire-and-forget: ghi nhận "đã xem" không được làm chậm hay làm hỏng việc mở chi tiết.
    recordPoiView(poi.poiId, session.accessToken)
      .then(() => fetchViewedPois(session.accessToken))
      .then(setViewedPois)
      .catch(() => undefined)
  }, [session])

  /** Bấm một mục "Địa điểm đã xem" thì mở lại chi tiết POI đó (không có scoreDetail/open vì
   * đây là dữ liệu snapshot cũ, không phải kết quả của một lượt search sống). */
  function openViewedPoi(item: ViewedPoiItem) {
    selectPoi({
      poiId: item.poiId, name: item.name, category: item.category, address: item.address,
      latitude: item.latitude, longitude: item.longitude,
      distanceMeters: userLocation ? distanceMeters(userLocation, item) : undefined,
    })
  }
  const totalPages = Math.max(1, Math.ceil(data.results.length / PAGE_SIZE))
  const visibleResults = data.results.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE)

  if (!session) {
    return (
      <main className="min-h-screen">
        <header className={cn('sticky top-0 z-50 flex items-center justify-between border-b bg-background/90 px-[4vw] backdrop-blur', isMobile ? 'h-16' : 'h-18')}>
          <a className="flex items-center gap-3 font-serif text-xl font-bold text-foreground no-underline" href="/" aria-label="CoffeeScope trang chủ">
            <span className="grid h-10 w-10 place-items-center rounded-full bg-primary text-primary-foreground"><Coffee className="h-5 w-5" /></span>
            CoffeeScope
          </a>
          {!isMobile && <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Tìm đúng vibe coffee quanh bạn</p>}
        </header>
        <section className={cn('coffee-surface grid min-h-[calc(100vh-4.5rem)] items-center gap-10 px-[7vw] py-12', !isMobile && 'lg:grid-cols-[minmax(320px,1fr)_minmax(360px,440px)]')} aria-label="Đăng nhập CoffeeScope">
          <div>
            <p className="eyebrow">COFFEE FINDER</p>
            <h1 className={cn('max-w-3xl font-serif leading-none tracking-tight text-foreground', isMobile ? 'text-4xl' : 'text-5xl md:text-7xl')}>Đăng nhập để tìm quán coffee hợp gu.</h1>
            <p className="mt-6 max-w-xl text-base leading-8 text-muted-foreground">CoffeeScope giúp bạn tìm quán bánh, sân vườn, thú cưng, làm việc hoặc rooftop dựa trên vị trí hiện tại và vibe mong muốn.</p>
          </div>
          <AuthModal embedded onAuthenticated={authenticated} />
        </section>
      </main>
    )
  }

  return (
    <main className="min-h-screen">
      <header className={cn('sticky top-0 z-50 flex items-center justify-between border-b bg-background/90 px-[4vw] backdrop-blur', isMobile ? 'h-16 gap-2' : 'h-18')}>
        <a className={cn('flex items-center gap-3 text-foreground no-underline', isMobile ? 'min-w-0 flex-1' : 'font-serif text-xl font-bold')} href="/" aria-label="CoffeeScope trang chủ">
          <span className="grid h-10 w-10 place-items-center rounded-full bg-primary text-primary-foreground"><Coffee className="h-5 w-5" /></span>
          <span className={cn('font-serif font-bold', isMobile ? 'text-base' : 'text-xl')}>CoffeeScope</span>
        </a>
        <div className="flex items-center gap-2">
          {!isMobile && <Badge variant="secondary" className="max-w-52 overflow-hidden text-ellipsis whitespace-nowrap">{session.email}</Badge>}
          <Button variant="outline" size={isMobile ? 'icon' : 'sm'} loading={logoutLoading} onClick={signOut} aria-label="Đăng xuất"><LogOut className="h-4 w-4" />{!isMobile && 'Đăng xuất'}</Button>
          <Button variant="accent" size="icon" className="rounded-full" aria-label={session ? 'Tài khoản đã đăng nhập' : 'Đăng nhập hoặc đăng ký'} title={session?.email} onClick={() => setShowAuth(true)}>
            <UserRound className="h-4 w-4" />
          </Button>
        </div>
      </header>
      <section className={cn('coffee-surface border-b px-[4vw]', isMobile ? 'py-7' : 'py-10')}>
        <div className="mb-7 flex flex-col gap-3">
          <div>
            <p className="eyebrow">COFFEE FINDER</p>
            <h1 className={cn('w-full font-serif leading-none tracking-tight', isMobile ? 'text-4xl' : 'text-5xl md:text-7xl')}>Tìm quán coffee, <em className="font-normal text-primary">đúng vibe.</em></h1>
          </div>
          {loading && <Badge variant="accent" className="w-fit animate-pulse"><Navigation className="mr-1 h-3 w-3" />Đang tìm coffee shop quanh vị trí của bạn...</Badge>}
        </div>
        <SearchForm loading={loading} onSearch={search} onLocationChange={setUserLocation} />
      </section>
      <section className={cn('grid min-h-[calc(100vh-17rem)]', !isMobile && 'lg:grid-cols-[minmax(360px,38%)_1fr]')}>
        <div className={cn('bg-secondary/70 p-5', isMobile ? 'border-b' : 'border-r lg:p-8')}>
          <div className="mb-5 flex items-end justify-between border-b pb-5">
            <div><p className="eyebrow">QUÁN COFFEE</p><h2 className="font-serif text-3xl">{data.total ? `${data.total} quán phù hợp` : 'Chọn vibe coffee'}</h2></div>
            {data.normalizedQuery && <Badge variant="outline">“{data.normalizedQuery}”</Badge>}
          </div>
          {error && <Alert className="border-destructive/40 bg-destructive/10">
            <AlertCircle className="mr-2 inline h-4 w-4 text-destructive" />
            <AlertTitle className="inline text-destructive">Không tải được dữ liệu</AlertTitle>
            <AlertDescription className="mt-2 text-destructive">{error}</AlertDescription>
          </Alert>}
          {loading && <div className="grid gap-3" aria-label="Đang tải kết quả">
            {Array.from({ length: 5 }, (_, index) => <Card key={index} className="p-4">
              <div className="flex gap-3">
                <Skeleton className="h-9 w-9 rounded-full" />
                <div className="flex-1 space-y-2"><Skeleton className="h-4 w-2/3" /><Skeleton className="h-3 w-full" /></div>
                <Skeleton className="h-10 w-10 rounded-full" />
              </div>
            </Card>)}
          </div>}
          {!loading && !error && data.results.length === 0 && <Card className="grid min-h-80 place-items-center bg-card/70 p-8 text-center">
            <div className="grid justify-items-center gap-3">
              <span className="grid h-16 w-16 place-items-center rounded-full border border-primary text-primary"><Compass className="h-7 w-7" /></span>
              <CardTitle>{data.suggestion ? 'Chưa tìm thấy quán phù hợp' : 'Bắt đầu với một vibe'}</CardTitle>
              <CardDescription>{data.suggestion ?? 'Bấm “Dùng vị trí của tôi”, sau đó thử coffee bánh, sân vườn, thú cưng, làm việc hoặc rooftop.'}</CardDescription>
            </div>
          </Card>}
          {!loading && !error && data.results.length === 0 && (searchHistory.length > 0 || viewedPois.length > 0) && (
            <div className="mt-6 grid gap-6">
              {searchHistory.length > 0 && <section aria-label="Tìm kiếm gần đây">
                <p className="eyebrow mb-2 flex items-center gap-1.5"><History className="h-3.5 w-3.5" />Tìm kiếm gần đây</p>
                <div className="flex flex-wrap gap-2">
                  {searchHistory.map((item) => (
                    <Button key={item.id} variant="outline" size="sm" onClick={() => searchFromHistory(item)}>{item.query}</Button>
                  ))}
                </div>
              </section>}
              {viewedPois.length > 0 && <section aria-label="Địa điểm đã xem">
                <p className="eyebrow mb-2 flex items-center gap-1.5"><Eye className="h-3.5 w-3.5" />Địa điểm đã xem</p>
                <div className="grid gap-2">
                  {viewedPois.map((item) => (
                    <button key={item.poiId} className="flex items-center justify-between gap-3 rounded-lg border bg-card/70 p-3 text-left transition hover:bg-card" onClick={() => openViewedPoi(item)}>
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium">{item.name}</p>
                        <p className="truncate text-xs text-muted-foreground">{item.category} · {item.address}</p>
                      </div>
                      <Badge variant="secondary" className="shrink-0">{item.avgRating.toFixed(1)}★</Badge>
                    </button>
                  ))}
                </div>
              </section>}
            </div>
          )}
          <div className="grid">{!loading && visibleResults.map((poi, index) => (
            <button key={poi.poiId} className={`group grid w-full grid-cols-[2.25rem_1fr_auto] items-start gap-3 border-b p-4 text-left transition hover:bg-card ${selected?.poiId === poi.poiId ? 'bg-card shadow-sm' : 'bg-transparent'}`} onClick={() => selectPoi(poi)}>
              <span className="font-serif text-xs text-muted-foreground">{String(currentPage * PAGE_SIZE + index + 1).padStart(2, '0')}</span>
              <div><p className="eyebrow">{poi.category} · Coffee place</p><h3 className="font-serif text-xl leading-tight">{poi.name}</h3><p className="mt-1 text-xs text-muted-foreground">{poi.address}</p></div>
              <div className="grid justify-items-end gap-2"><span className="grid h-10 w-10 place-items-center rounded-full bg-primary text-xs font-bold text-primary-foreground">{Math.round(poi.scoreDetail.finalScore * 100)}</span><span className="text-xs text-muted-foreground">{poi.distanceMeters != null ? `${Math.round(poi.distanceMeters)} m` : '—'}</span></div>
            </button>
          ))}</div>
          {!loading && data.results.length > PAGE_SIZE && <nav className="mt-5 flex items-center justify-between gap-3 border-t pt-5" aria-label="Phân trang kết quả">
            <Button variant="outline" disabled={currentPage === 0} onClick={() => { setCurrentPage((page) => page - 1); setSelected(undefined) }}>Trước</Button>
            <span className="text-center text-xs text-muted-foreground">Trang {currentPage + 1}/{totalPages} · Hiển thị {visibleResults.length}/{data.total}</span>
            <Button variant="outline" disabled={currentPage >= totalPages - 1} onClick={() => { setCurrentPage((page) => page + 1); setSelected(undefined) }}>Sau</Button>
          </nav>}
        </div>
        <div className={cn('relative overflow-hidden bg-muted', isMobile ? 'min-h-[420px]' : 'min-h-[560px]')}>
          <MapView results={visibleResults} selectedId={selected?.poiId} userLocation={userLocation} onSelect={selectPoi} />
          <Badge variant="secondary" className="absolute bottom-3 left-3 z-[450]"><MapPinned className="mr-1 h-3 w-3" />Bản đồ © OpenStreetMap / Geoapify</Badge>
          {selected && <PoiDetail poi={selected} token={session.accessToken} onClose={() => setSelected(undefined)} />}
        </div>
      </section>
      {showAuth && <AuthModal onClose={() => setShowAuth(false)} onAuthenticated={authenticated} />}
    </main>
  )
}

export default App
