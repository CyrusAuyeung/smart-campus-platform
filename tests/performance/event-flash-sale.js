import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    flash_sale: {
      executor: 'constant-vus',
      vus: 20,
      duration: '20s'
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<800']
  }
};

const API_BASE = __ENV.API_BASE || 'http://localhost:8080/api';
const EVENT_ID = __ENV.EVENT_ID || 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';

export default function () {
  const userId = `00000000-0000-0000-0000-${String(__VU).padStart(12, '0')}`;
  const payload = JSON.stringify({ userId, eventId: EVENT_ID });
  const response = http.post(`${API_BASE}/v1/events/reserve`, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

  check(response, {
    'response is 200 or 4xx': (res) => [200, 400, 409].includes(res.status)
  });

  sleep(0.2);
}
