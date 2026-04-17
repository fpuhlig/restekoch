// Scenario 4: Scan stress test with 10 concurrent VUs and 10 different images.
// Each VU picks a random image for each iteration. First iteration per unique
// image is a true cache miss (L1 + L2). Subsequent iterations of the same
// image hit L1. Budget: worst case 10 distinct Gemini calls at ~$0.003 each.
//
// Ramp-up 30 seconds from 1 to 10 VUs. Ramp-down 10 seconds. No hard spike,
// to stay within the default 60 RPM Vertex AI limit for Gemini Flash.

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL } from './lib/common.js';

const cacheHitRate = new Rate('cache_hit_rate');
const l1HitRate = new Rate('l1_hit_rate');
const scanDuration = new Trend('scan_duration_ms', true);

// open() must be called at the top level. SharedArray JSON-serializes values,
// which corrupts ArrayBuffer. Load all 10 images at init time.
const fridge1 = open('./fixtures/fridge1.jpg', 'b');
const fridge2 = open('./fixtures/fridge2.jpg', 'b');
const fridge3 = open('./fixtures/fridge3.jpg', 'b');
const fridge4 = open('./fixtures/fridge4.jpg', 'b');
const fridge5 = open('./fixtures/fridge5.jpg', 'b');
const fridge6 = open('./fixtures/fridge6.jpg', 'b');
const fridge7 = open('./fixtures/fridge7.jpg', 'b');
const fridge8 = open('./fixtures/fridge8.jpg', 'b');
const fridge9 = open('./fixtures/fridge9.jpg', 'b');
const fridge10 = open('./fixtures/fridge10.jpg', 'b');
const images = [
  { name: 'fridge1.jpg', bytes: fridge1 },
  { name: 'fridge2.jpg', bytes: fridge2 },
  { name: 'fridge3.jpg', bytes: fridge3 },
  { name: 'fridge4.jpg', bytes: fridge4 },
  { name: 'fridge5.jpg', bytes: fridge5 },
  { name: 'fridge6.jpg', bytes: fridge6 },
  { name: 'fridge7.jpg', bytes: fridge7 },
  { name: 'fridge8.jpg', bytes: fridge8 },
  { name: 'fridge9.jpg', bytes: fridge9 },
  { name: 'fridge10.jpg', bytes: fridge10 },
];

export const options = {
  scenarios: {
    scan_stress: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '60s', target: 10 },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    // http_req_duration p95 calibrated after pilot run (Gemini Vision is slow)
  },
  tags: {
    scenario: '04-scan-stress',
  },
};

export function setup() {
  // Warm up JVM JIT with a cheap GET. Do not prime scan with an image,
  // that would pollute the cache state the test is measuring.
  for (let i = 0; i < 5; i++) {
    http.get(`${BASE_URL}/api/cache/stats`);
  }
}

export default function () {
  const pick = images[Math.floor(Math.random() * images.length)];
  const payload = {
    image: http.file(pick.bytes, pick.name, 'image/jpeg'),
  };
  const res = http.post(`${BASE_URL}/api/scan`, payload, {
    tags: { endpoint: 'scan', image: pick.name },
    timeout: '60s',
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  if (res.status === 200) {
    // Minimal parse: only the fields we need for metrics.
    // Avoid full JSON inspection under concurrency.
    try {
      const body = JSON.parse(res.body);
      cacheHitRate.add(body.cached === true);
      l1HitRate.add(body.cacheLevel === 'L1' || body.cacheLevel === 'L1+L2');
    } catch (e) {
      // malformed response, do not count
    }
    scanDuration.add(res.timings.duration);
  }
}
