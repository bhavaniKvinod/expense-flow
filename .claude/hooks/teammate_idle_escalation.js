#!/usr/bin/env node
/**
 * TeammateIdle escalation hook.
 *
 * Fires when an agent-team teammate transitions to idle (requires
 * CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1). The TeammateIdle event has no
 * configurable idle-duration threshold, so we approximate "idle for N seconds"
 * as an escalation DELAY: on the idle event we wait N seconds, then append an
 * escalation record to a log file inside the project.
 *
 * Configure via env (defaults in parens):
 *   TEAMMATE_IDLE_ESCALATION_SECONDS   idle/escalation delay in seconds (20)
 *   TEAMMATE_IDLE_LOG                   log file path (<project>/.claude/logs/teammate-idle.log)
 *
 * Meant to run as an async hook (async: true) so the 20s wait never blocks a turn.
 *
 * Caveat: TeammateIdle fires once per idle transition and there is no
 * "teammate resumed" event, so a teammate that becomes active again within the
 * delay window is still logged. Entries are timestamped so this is visible.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const DELAY_SECONDS = Number(process.env.TEAMMATE_IDLE_ESCALATION_SECONDS || 20);
const LOG_PATH =
  process.env.TEAMMATE_IDLE_LOG ||
  path.join(process.cwd(), '.claude', 'logs', 'teammate-idle.log');

let raw = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (d) => (raw += d));
process.stdin.on('end', () => {
  let payload = {};
  try {
    payload = JSON.parse(raw) || {};
  } catch {
    payload = { _parse_error: true, _raw: raw };
  }

  // The TeammateIdle payload schema is undocumented — pull the most likely
  // identifier fields defensively, keep the full payload for completeness.
  const teammate =
    payload.teammate_name ||
    payload.teammate_id ||
    payload.name ||
    payload.agent_id ||
    payload.team_name ||
    'unknown-teammate';
  const sessionId = payload.session_id || 'unknown-session';
  const detectedAt = new Date().toISOString();

  // Wait out the idle/escalation window, then log. Fail open on any error.
  setTimeout(() => {
    const escalatedAt = new Date().toISOString();
    const entry = {
      event: 'TeammateIdle',
      level: 'ESCALATION',
      teammate,
      session_id: sessionId,
      idle_detected_at: detectedAt,
      escalated_at: escalatedAt,
      idle_seconds_threshold: DELAY_SECONDS,
      note: `Teammate went idle; escalation logged after ${DELAY_SECONDS}s.`,
      payload,
    };
    try {
      fs.mkdirSync(path.dirname(LOG_PATH), { recursive: true });
      fs.appendFileSync(LOG_PATH, JSON.stringify(entry) + '\n');
    } catch {
      /* fail open — never crash the teammate on a logging error */
    }
    process.exit(0);
  }, DELAY_SECONDS * 1000);
});
