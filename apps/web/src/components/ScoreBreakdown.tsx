import type { ScoreDetail } from '../types/search'

const signals: Array<[keyof ScoreDetail, string]> = [
  ['bm25', 'Phù hợp'], ['spatial', 'Khoảng cách'], ['temporal', 'Thời gian'], ['rating', 'Đánh giá'],
]

export function ScoreBreakdown({ score }: { score: ScoreDetail }) {
  return (
    <div className="score-breakdown" aria-label="Chi tiết điểm xếp hạng">
      {signals.map(([key, label]) => (
        <div className="score-signal" key={key}>
          <div><span>{label}</span><strong>{Math.round(score[key] * 100)}%</strong></div>
          <progress max="1" value={score[key]} aria-label={label} />
        </div>
      ))}
      <div className="final-score">Điểm tổng <strong>{Math.round(score.finalScore * 100)}</strong></div>
    </div>
  )
}
