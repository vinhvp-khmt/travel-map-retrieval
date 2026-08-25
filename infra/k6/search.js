import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: Number(__ENV.VUS || 10),
  duration: __ENV.DURATION || '15s',
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<500'] },
};

export default function () {
  const base = __ENV.BASE_URL || 'http://localhost:8080';
  const response = http.get(`${base}/api/v1/search?q=cafe&latitude=10.7769&longitude=106.7009&radiusKm=2`);
  check(response, { 'search returns 200': (r) => r.status === 200 });
}
