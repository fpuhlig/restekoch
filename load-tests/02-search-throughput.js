// Scenario 2: Search throughput with varied ingredient lists.
// Sends 50 GET /api/search requests with different ingredient combinations.
// No Gemini calls, just embedding + Redis KNN. Measures how many searches
// per second the stack handles with a real embedding round trip each time.

import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, warmup } from './lib/common.js';

const searchDuration = new Trend('search_duration_ms', true);

const INGREDIENT_SETS = [
  'tomato,basil,garlic',
  'chicken,lemon,rosemary',
  'pasta,olive oil,parmesan',
  'rice,soy sauce,ginger',
  'potato,onion,butter',
  'beef,mushroom,red wine',
  'salmon,dill,capers',
  'egg,flour,milk',
  'cheese,bread,ham',
  'avocado,lime,cilantro',
  'cucumber,yogurt,mint',
  'carrot,celery,onion',
  'spinach,feta,phyllo',
  'apple,cinnamon,sugar',
  'shrimp,garlic,butter',
  'lentil,cumin,coriander',
  'pork,apple,sage',
  'tuna,mayo,onion',
  'zucchini,parmesan,thyme',
  'chocolate,butter,sugar',
  'bell pepper,onion,oregano',
  'kale,garlic,lemon',
  'quinoa,black bean,corn',
  'tofu,soy sauce,sesame',
  'bacon,egg,cheese',
];

export const options = {
  scenarios: {
    search_throughput: {
      executor: 'shared-iterations',
      vus: 2,
      iterations: 50,
      maxDuration: '3m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // http_req_duration p95 calibrated after pilot run
  },
  tags: {
    scenario: '02-search-throughput',
  },
};

export function setup() {
  warmup(() => http.get(`${BASE_URL}/api/search?ingredients=tomato`), 10);
}

export default function () {
  const set = INGREDIENT_SETS[Math.floor(Math.random() * INGREDIENT_SETS.length)];
  const res = http.get(
    `${BASE_URL}/api/search?ingredients=${encodeURIComponent(set)}&limit=10`,
    { tags: { endpoint: 'search' } },
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

  searchDuration.add(res.timings.duration);
}
