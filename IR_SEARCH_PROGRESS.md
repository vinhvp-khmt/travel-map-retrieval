# IR Search & Evaluation Progress

> Scope: custom POI retrieval/ranking and a reproducible, self-seeded IR evaluation corpus.
>
> A checkbox is marked complete only after implementation, automated verification, and reproducible evidence are committed.

## Milestone summary

- [ ] M0 — Reproducible baseline and progress tracking
- [x] M1 — Frontend uses the backend IR search path
- [ ] M2 — Versioned, explainable ranking and reliable index lifecycle
- [ ] M3 — Deterministic 30–50 POI evaluation corpus
- [ ] M4 — Graded qrels and quantitative evaluator
- [ ] M5 — Baseline comparison report and final verification

## M0 — Reproducible baseline

- [x] Existing custom inverted index and BM25 implementation identified.
- [x] Existing PostGIS spatial candidate retrieval identified.
- [x] Existing temporal-fit implementation, including overnight logic, identified.
- [x] Existing backend `scoreDetail` response identified.
- [x] Record passing backend test baseline — 59 tests passed on 2026-09-18 after integrating current `origin/main`.
- [x] Record passing frontend test and build baseline — 18 tests passed and the production build succeeded on 2026-09-18.
- [ ] Confirm the seed and evaluation commands from a clean local database.
- [ ] Commit and push this progress checklist with the verified baseline.

## M1 — Backend IR is the product search path

- [x] Replace the frontend Geoapify Places result path with `/api/v1/search`.
- [x] Forward query, GPS, radius, visit time, and supported filters to the backend.
- [x] Remove synthetic Geoapify BM25/rating/temporal/final scores.
- [x] Keep Geoapify only for map tiles and address/geocoding suggestions.
- [x] Ensure internal POI search remains usable when Geoapify Places is unavailable.
- [x] Update frontend unit/E2E tests to verify that product search calls the backend.
- [x] Verify backend and frontend test suites — backend 59 tests; frontend 17 tests and production build pass.
- [x] Commit and push M1.

## M2 — Versioned and explainable ranking

### Index lifecycle

- [ ] Promote the inverted index to an application-scoped service.
- [ ] Build the index at application startup from ACTIVE POIs.
- [ ] Refresh the index after POI approval/update and rating changes.
- [ ] Stop rebuilding the entire index inside each search request.
- [ ] Test ACTIVE-only indexing and refresh behavior.

### Ranking profiles

- [x] Add explicit `bm25-only` baseline profile.
- [x] Add explicit `distance-only` baseline profile.
- [x] Preserve the current weighted formula as `full-v1`.
- [x] Add `full-v2` without changing `full-v1` output.
- [ ] Allow the requested ranking profile to be selected through the search API.
- [ ] Add a deterministic tie-break order.
- [ ] Return the selected ranking version/profile in the response.

### Rating confidence and score transparency

- [x] Make v2 rating use both average rating and rating count.
- [x] Implement and document a Bayesian/prior-adjusted rating formula.
- [x] Test 5.0/1 review against 4.5/200 reviews.
- [x] Keep the documented v1 linear spatial decay unchanged.
- [x] Document and test the v2 spatial formula if it differs.
- [ ] Expose raw values, normalized component scores, weights, contributions, and final score.
- [ ] Make `finalScore` reproducible from `scoreDetail` within rounding tolerance.
- [x] Add ranking-profile regression tests; expanded score-detail regression remains pending.
- [x] Verify backend and frontend test suites.
- [ ] Commit and push M2.

## M3 — Deterministic POI evaluation corpus

- [ ] Expand the seed corpus to between 30 and 50 POIs.
- [ ] Use stable IDs so qrels remain valid across reseeds.
- [ ] Give each evaluation POI a natural 2–3 sentence description.
- [ ] Cover coffee/work, garden, rooftop, pet-friendly, bakery, dining, attraction, and lodging intents as appropriate.
- [ ] Add close-but-textually-irrelevant POIs.
- [ ] Add textually-relevant-but-more-distant POIs.
- [ ] Add ACTIVE, PENDING, and inactive examples.
- [ ] Add at least three `18:00–02:00` overnight POIs.
- [ ] Add a 24-hour POI and weekly-closure cases.
- [ ] Add rating distributions including 5.0/1, 4.8/5, 4.5/200, 4.2/500, low-rated, and unrated POIs.
- [ ] Add 3–5 same-brand branches and near-name non-brand POIs for diversity experiments.
- [ ] Ensure reseeding is deterministic and idempotent.
- [ ] Add automated corpus invariants for count and edge-case coverage.
- [ ] Verify seed execution against PostgreSQL/PostGIS.
- [ ] Commit and push M3.

## M4 — Queries, qrels, and metrics

### Judgments

- [ ] Create 20–30 fixed evaluation queries.
- [ ] Store query text, latitude, longitude, radius, visit time, and expected intent.
- [ ] Include accented and accent-free Vietnamese queries.
- [ ] Include lexical, spatial, temporal, rating-confidence, and mixed-intent cases.
- [ ] Define relevance grades 0–3 in a judgment guide.
- [ ] Create version-controlled query–POI qrels.
- [ ] Ensure judgments are authored independently of ranker output.
- [ ] Review ambiguous judgments and record decisions.

### Evaluator

- [x] Implement Precision@5.
- [x] Implement Average Precision and MAP.
- [x] Implement graded DCG/NDCG@10.
- [x] Add hand-calculated metric unit tests.
- [x] Run the existing evaluation profiles over the same query/context set.
- [ ] Export per-query results and macro averages.
- [ ] Report missing judgments and invalid POI/query IDs as errors.
- [x] Provide one documented command that regenerates the current synthetic report.
- [ ] Commit and push M4.

## M5 — Comparison report and completion evidence

- [ ] Generate results for `distance-only`.
- [ ] Generate results for `bm25-only`.
- [ ] Generate results for `full-v1`.
- [ ] Generate results for `full-v2`.
- [ ] Report P@5, MAP, and NDCG@10 for every profile.
- [ ] Report absolute and relative changes from each baseline.
- [ ] Include per-query regressions, not only aggregate averages.
- [ ] Record corpus version, qrels version, ranker configuration, and Git commit.
- [ ] Run the complete backend test suite.
- [ ] Run the complete frontend test and production build.
- [ ] Recreate the seed and evaluation report from documented commands.
- [ ] Update README with architecture, Geoapify boundary, and reproduction steps.
- [ ] Commit and push M5.

## Completion criteria

The assignment is complete only when the UI uses the custom backend ranker, the repository contains a deterministic 30–50 POI corpus plus graded qrels, and one reproducible command compares BM25-only, distance-only, full-v1, and full-v2 using P@5, MAP, and NDCG@10.
