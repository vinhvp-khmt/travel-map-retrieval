import type { SearchResult } from '../types/search'
import { ScoreBreakdown } from './ScoreBreakdown'

export function PoiDetail({ poi, onClose }: { poi: SearchResult; onClose: () => void }) {
  return (
    <aside className="poi-detail" aria-label={`Chi tiết ${poi.name}`}>
      <button className="close-button" onClick={onClose} aria-label="Đóng chi tiết">×</button>
      <p className="eyebrow">{poi.category} · Geoapify</p>
      <h2>{poi.name}</h2>
      <p>{poi.address}</p>
      <div className="poi-facts">
        <span>{Math.round(poi.distanceMeters)} m</span>
        <span className={poi.open ? 'open' : 'closed'}>{poi.open ? 'Đang mở cửa' : 'Đang đóng cửa'}</span>
      </div>
      <ScoreBreakdown score={poi.scoreDetail} />
    </aside>
  )
}
