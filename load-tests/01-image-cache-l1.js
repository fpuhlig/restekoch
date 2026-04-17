// Scenario 1: Image Cache L1 Hit Rate.
// Sends the same image 100 times. First request misses (full Gemini pipeline),
// the next 99 should be L1 hits. Measures cache_hit_rate custom metric based on
// response.cacheLevel field.
//
// Pre-flight: DELETE /api/cache (see load-tests/README.md).
// Thresholds are baseline-calibrated (filled after pilot run).

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, warmup } from './lib/common.js';

const cacheHitRate = new Rate('cache_hit_rate');
const l1HitRate = new Rate('l1_hit_rate');
const scanDuration = new Trend('scan_duration_ms', true);

// open() must be called at the top level of the script, not inside default().
// Binary mode returns an ArrayBuffer that http.file() accepts directly.
const imageBytes = open('./fixtures/fridge1.jpg', 'b');

export const options = {
  scenarios: {
    image_cache_l1: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: 100,
      maxDuration: '5m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    cache_hit_rate: ['rate>0.95'],
    l1_hit_rate: ['rate>0.95'],
    // http_req_duration p95 calibrated after pilot run
  },
  tags: {
    scenario: '01-image-cache-l1',
  },
};

export function setup() {
  // Warm up JVM JIT with a handful of cheap GET requests.
  // Do not prime the scan endpoint here, the first real iteration must be a true miss.
  warmup(() => http.get(`${BASE_URL}/api/cache/stats`), 10);
}

export default function () {
  const payload = {
    image: http.file(imageBytes, 'fridge1.jpg', 'image/jpeg'),
  };
  const res = http.post(`${BASE_URL}/api/scan`, payload, {
    tags: { endpoint: 'scan' },
  });

  const ok = check(res, {
    'status is 200': (r) => r.status === 200,
    'has ingredients': (r) => {
      try {
        return JSON.parse(r.body).ingredients.length > 0;
      } catch (e) {
        return false;
      }
    },
  });

  if (ok && res.status === 200) {
    const body = JSON.parse(res.body);
    cacheHitRate.add(body.cached === true);
    l1HitRate.add(body.cacheLevel === 'L1' || body.cacheLevel === 'L1+L2');
    scanDuration.add(res.timings.duration);
  }

  sleep(0.1);
}
