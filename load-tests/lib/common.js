// Shared helpers for all load test scenarios.
// - BASE_URL defaults to gateway on localhost. Override via env for remote testing.
// - Warm-up helper discards first N requests to let JVM JIT settle before measurement.

export const BASE_URL = __ENV.BASE_URL || 'http://localhost';

export function warmup(fn, count = 10) {
  for (let i = 0; i < count; i++) {
    try {
      fn();
    } catch (e) {
      // swallow warm-up errors, they are expected while backend warms up
    }
  }
}
