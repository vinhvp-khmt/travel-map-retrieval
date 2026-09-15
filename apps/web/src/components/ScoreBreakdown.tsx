import type { ScoreDetail } from '../types/search'
import { Card, CardContent } from '@/components/ui/card'

const signals: Array<[keyof ScoreDetail, string]> = [
  ['bm25', 'Phù hợp'], ['spatial', 'Khoảng cách'], ['temporal', 'Thời gian'], ['rating', 'Đánh giá'],
]

export function ScoreBreakdown({ score }: { score: ScoreDetail }) {
  return (
    <Card className="bg-secondary/40 shadow-none" aria-label="Chi tiết điểm xếp hạng">
      <CardContent className="grid gap-3 p-4">
      {signals.map(([key, label]) => (
        <div className="grid gap-1" key={key}>
          <div className="flex items-center justify-between text-xs text-muted-foreground"><span>{label}</span><strong>{Math.round(score[key] * 100)}%</strong></div>
          <progress className="h-1.5 w-full accent-accent" max="1" value={score[key]} aria-label={label} />
        </div>
      ))}
      <div className="flex items-center justify-between border-t pt-3 font-serif text-sm">Điểm tổng <strong className="text-3xl text-primary">{Math.round(score.finalScore * 100)}</strong></div>
      </CardContent>
    </Card>
  )
}
