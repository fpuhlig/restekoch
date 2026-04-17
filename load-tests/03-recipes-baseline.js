// Scenario 3: Recipes baseline (Firestore paginated read).
// Sends 500 GET /api/recipes?limit=20 requests. Measures baseline throughput
// for a simple paginated Firestore read (20 docs per request = 10,000 reads
// total, safely under the 50k/day free tier).
//
// This is the cheapest path in the stack: no Gemini, no Redis vector search,
// just Firestore pagination through the RecipeService.

import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, warmup } from './lib/common.js';

const recipesDuration = new Trend('recipes_duration_ms', true);

export const options = {
  scenarios: {
    recipes_baseline: {
      executor: 'shared-iterations',
      vus: 4,
      iterations: 500,
      maxDuration: '5m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.001'],
    // http_req_duration p95 calibrated after pilot run
  },
  tags: {
    scenario: '03-recipes-baseline',
  },
};

export function setup() {
  warmup(() => http.get(`${BASE_URL}/api/recipes?limit=20`), 10);
}

export default function () {
  const offset = Math.floor(Math.random() * 1900);
  const res = http.get(
    `${BASE_URL}/api/recipes?limit=20&offset=${offset}`,
    { tags: { endpoint: 'recipes' } },
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
    'returns array': (r) => {
      try {
        return Array.isArray(JSON.parse(r.body));
      } catch (e) {
        return false;
      }
    },
  });

  recipesDuration.add(res.timings.duration);
}
