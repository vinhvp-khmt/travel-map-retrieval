import type { SearchResult } from '../types/search'

export const cafe: SearchResult = {
  poiId: '671da296-f24b-4c60-b4d5-8a40534bc001',
  name: 'Cà phê Yên Tĩnh', category: 'Cà phê', address: '12 Nguyễn Huệ, Quận 1',
  latitude: 10.7769, longitude: 106.7009, distanceMeters: 240, open: true,
  scoreDetail: { bm25: 1, spatial: 0.88, temporal: 1, rating: 0.9, finalScore: 0.91 },
}
