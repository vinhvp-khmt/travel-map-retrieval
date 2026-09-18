# Kết quả đánh giá IR — Phần 2.4 (NDCG / MAP / P@5)

Tài liệu này ghi lại kết quả đo chất lượng xếp hạng của TravelMap, so sánh ba cấu hình chấm
điểm trên cùng một bộ truy vấn và dữ liệu vàng (golden data). Số liệu do `IrEvaluationTest`
sinh ra khi chạy `mvn verify`; bản chạy tham chiếu dưới đây khớp với dữ liệu trong repo.

## Ba cấu hình so sánh

| Tên | WeightProfile | Tín hiệu dùng |
|-----|---------------|----------------|
| **Full** | `V2` | BM25 (0.40) + khoảng cách *spatial decay* (0.30) + độ mở cửa (0.20) + rating *shrinkage* (0.10) |
| **Keyword-only** | `EVAL_KEYWORD_ONLY` | Chỉ BM25 (khớp từ khoá) |
| **Distance-only** | `EVAL_DISTANCE_ONLY` | Chỉ khoảng cách (tuyến tính) |

## Bảng kết quả (trung bình trên 16 truy vấn, 160 POI cố định, `diversify=false`)

| Cấu hình       |     P@5 |     MAP | NDCG@10 |
|----------------|---------|---------|---------|
| **Full (V2)**  | **0.7875** | **0.9221** | **0.9708** |
| Keyword-only   | 0.6000  | 0.5712  | 0.6369  |
| Distance-only  | 0.6000  | 0.5629  | 0.6099  |

**Full thắng cả ba độ đo.** `IrEvaluationTest` khẳng định điều này bằng assertion (test đỏ nếu
Full không vượt cả hai cấu hình còn lại trên P@5, MAP và NDCG@10).

## Vì sao Full thắng

Bộ dữ liệu vàng được thiết kế để phơi bày điểm yếu của từng cấu hình một tín hiệu:

- **Keyword-only** bị đánh lừa bởi các quán "nhồi từ khoá" (lặp tên/mô tả nhiều lần) nhưng nằm
  **rất xa** người dùng — chúng có BM25 cao nhất nên bị đẩy lên đầu, dù không liên quan (grade 0).
- **Distance-only** bị đánh lừa bởi các quán **rất gần** (tạp hoá, cửa hàng tiện lợi) nhưng chỉ
  bán món liên quan một cách hời hợt — gần nhất nên lên đầu, dù không liên quan (grade 0).
- **Full** kết hợp cả bốn tín hiệu nên vừa ưu tiên quán khớp từ khoá, vừa ưu tiên quán gần, vừa
  ưu tiên quán nhiều lượt đánh giá tốt (nhờ Bayesian shrinkage của 2.2) → đẩy đúng các quán
  liên quan (grade 2–3) lên đầu và dìm các "bẫy" xuống.

## Các độ đo (định nghĩa dùng trong `Metrics.java`)

- **P@k** — tỉ lệ tài liệu liên quan (grade ≥ 1) trong `k` vị trí đầu, chia cho `k`.
- **MAP** — trung bình của Average Precision qua các truy vấn; AP cộng precision tại mỗi vị trí
  trúng rồi chia cho tổng số tài liệu liên quan.
- **NDCG@k** — dùng gain `2^grade − 1` và chiết khấu `log2(i+2)`, chuẩn hoá theo thứ hạng lý tưởng.

## Cách tái tạo

```
cd services/api
mvn -Dtest=IrEvaluationTest test
```

Bảng cũng được ghi tự động ra `services/api/target/evaluation/results.md` sau mỗi lần chạy, và
in ra console (log CI). Dữ liệu nguồn: `src/test/resources/evaluation/corpus.json` (160 POI cố
định) và `src/test/resources/evaluation/qrels.json` (16 truy vấn, nhãn grade 0–3).

> Ghi chú về nhãn: bộ POI + nhãn ở đây là **dữ liệu tổng hợp có kiểm soát** để minh hoạ và kiểm
> thử hồi quy chất lượng một cách ổn định (theo hướng A đã chọn — seed POI cố định trong test).
> Có thể mở rộng bằng POI/truy vấn thật của nhóm sau này: chỉ cần thêm dòng vào hai file JSON,
> không phải sửa code.
