/**
 * Optional smoke (not a CI gate): Scenario 6 — concurrent code submissions
 * against one LevelProgress should complete the level at most once and not
 * lose accepted codes (atomic threshold update).
 *
 * Prerequisites:
 *   - API running (e.g. docker-compose.local.yml)
 *   - Env vars set (see README.md)
 *
 * Run:
 *   k6 run load-tests/k6/scenario6-code-threshold.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';
const QUEST_ID = __ENV.QUEST_ID || '';
const TEAM_ID = __ENV.TEAM_ID || '';
const CODE_VALUE = __ENV.CODE_VALUE || 'SMOKECODE';

const submitOk = new Counter('code_submit_ok');
const submitFail = new Counter('code_submit_fail');
const levelCompletedSeen = new Counter('level_completed_responses');

export const options = {
  scenarios: {
    concurrent_submits: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 20),
      duration: __ENV.DURATION || '15s',
    },
  },
  thresholds: {
    // Informational only — adjust when you have a stable baseline.
    http_req_failed: ['rate<0.5'],
  },
};

function authHeaders() {
  const h = { 'Content-Type': 'application/json' };
  if (TOKEN) {
    h.Authorization = `Bearer ${TOKEN}`;
  }
  return h;
}

export default function () {
  if (!QUEST_ID || !TEAM_ID) {
    console.error('QUEST_ID and TEAM_ID are required');
    return;
  }

  const url = `${BASE_URL}/api/quests/progress/${QUEST_ID}/${TEAM_ID}/codes`;
  const res = http.post(
    url,
    JSON.stringify({ codeValue: CODE_VALUE }),
    { headers: authHeaders() }
  );

  const ok = check(res, {
    'status is 2xx or expected 4xx': (r) => r.status >= 200 && r.status < 500,
  });

  if (res.status >= 200 && res.status < 300) {
    submitOk.add(1);
    try {
      const body = res.json();
      if (body && (body.levelCompleted === true || body.levelStatus === 'COMPLETED')) {
        levelCompletedSeen.add(1);
      }
    } catch (_) {
      // ignore parse errors
    }
  } else {
    submitFail.add(1);
  }

  sleep(0.05);
}

export function handleSummary(data) {
  return {
    stdout: JSON.stringify(
      {
        note: 'Optional smoke — not a CI gate. Review counters and HTTP stats.',
        code_submit_ok: data.metrics.code_submit_ok,
        code_submit_fail: data.metrics.code_submit_fail,
        level_completed_responses: data.metrics.level_completed_responses,
        http_req_duration: data.metrics.http_req_duration,
      },
      null,
      2
    ),
  };
}
