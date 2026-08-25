# TravelMap IR Evaluation Report

## Scope

Stage 4 evaluates the deterministic MVP retrieval pipeline: Vietnamese accent folding and
tokenization, PostGIS radius filtering, BM25 text relevance, temporal fit and weighted ranking.
Only POIs with status `ACTIVE` enter the index.

## Ranking formula

`finalScore = 0.40 × BM25 + 0.30 × spatial + 0.20 × temporal + 0.10 × rating`

- BM25 uses `k1 = 1.2`, `b = 0.75` and Robertson IDF.
- Spatial score is `max(0, 1 - distance / radius)`.
- Temporal score is `1.0` when open, `0.5` when opening within 60 minutes, otherwise `0.0`.
- Rating is normalized from the five-star scale.

## Baseline qrels and acceptance

This initial baseline is a synthetic, version-controlled corpus used for regression testing;
it is not presented as production relevance evidence. Queries cover `cà phê`, `bảo tàng`,
`ăn tối` and accent-free equivalents. A relevant POI must match intent, be inside the radius
and be active.

| Metric | MVP gate |
|---|---:|
| Precision@5 | >= 0.80 |
| Recall@10 | >= 0.80 |
| nDCG@10 | >= 0.85 |
| Search API p95 (500 candidates, local) | < 500 ms |

## Automated evidence

- `VietnameseTokenizerTest`: accent folding, punctuation and stop words.
- `BM25ScorerTest`: matching/repeated terms outrank non-matches.
- `TemporalFitServiceTest`: opening-soon and overnight schedules.
- `RankingServiceTest`: weights and open-versus-closed ordering signal.
- `SearchServiceTest`: spatial/text intersection, ACTIVE-only source and no-result suggestion.
- `SearchRequestValidatorTest`: radius, pagination and defaults.

Run with:

```bash
services/api/mvnw --offline test
./run_pipeline.sh --stage 4
```

## Next evaluation iteration

Before production, replace the synthetic qrels with anonymized judgments from local users,
record per-query Precision@5/Recall@10/nDCG@10, and tune weights behind versioned configuration.
