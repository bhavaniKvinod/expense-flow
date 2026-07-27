#!/usr/bin/env node
/**
 * UserPromptSubmit hook: expand & optimize the user's prompt.
 *
 * Reads the submitted prompt from stdin (hook JSON), asks a fast headless
 * Claude (Haiku) to rewrite it for clarity/completeness while preserving
 * intent, and injects the rewrite back as additionalContext (augment mode —
 * the original prompt is never discarded).
 *
 * Design guarantees:
 *  - Fails OPEN: any error / empty result / timeout => no output, original
 *    prompt is used unchanged. The optimizer can never block a turn.
 *  - No recursion: the inner `claude -p` call would itself fire this hook;
 *    the CLAUDE_PROMPT_OPTIMIZER_ACTIVE guard short-circuits that.
 *  - Prompt is piped via stdin (not argv) to avoid shell-escaping issues.
 */
'use strict';

// --- Recursion guard: the inner headless claude call re-triggers this hook. ---
if (process.env.CLAUDE_PROMPT_OPTIMIZER_ACTIVE === '1') process.exit(0);

const { spawnSync } = require('child_process');

const MODEL = 'claude-haiku-4-5-20251001';
const TIMEOUT_MS = 60000;
const MIN_LENGTH = 15; // don't bother optimizing trivial prompts

let raw = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (d) => (raw += d));
process.stdin.on('end', () => {
  let prompt = '';
  try {
    prompt = (JSON.parse(raw).prompt || '').toString();
  } catch {
    process.exit(0); // malformed input => fail open
  }

  const trimmed = prompt.trim();

  // Skip: empty, trivial, slash-commands, or input-box bash (`!`) commands.
  if (
    trimmed.length < MIN_LENGTH ||
    trimmed.startsWith('/') ||
    trimmed.startsWith('!')
  ) {
    process.exit(0);
  }

  const instruction =
    'You are a prompt optimizer for a software-engineering AI agent working ' +
    'inside a code repository. Rewrite the USER PROMPT below so it is clearer, ' +
    'more specific, and complete, while preserving the user\'s original intent ' +
    'exactly. Make implicit requirements explicit, expand vague references, and ' +
    'give multi-part requests a clear structure. Do NOT answer or perform the ' +
    'task. Do NOT invent requirements the user did not imply. If the prompt is ' +
    'already clear, return it essentially unchanged. Output ONLY the rewritten ' +
    'prompt — no preamble, commentary, or code fences.\n\n' +
    'USER PROMPT:\n' +
    prompt;

  let res;
  try {
    res = spawnSync('claude', ['-p', '--model', MODEL], {
      input: instruction,
      encoding: 'utf8',
      timeout: TIMEOUT_MS,
      shell: true,
      env: { ...process.env, CLAUDE_PROMPT_OPTIMIZER_ACTIVE: '1' },
    });
  } catch {
    process.exit(0);
  }

  if (!res || res.status !== 0 || !res.stdout) process.exit(0);

  const optimized = res.stdout.trim();
  // Guard against noise / no-op rewrites.
  if (!optimized || optimized === trimmed) process.exit(0);

  const output = {
    systemMessage: '✨ Prompt optimized',
    hookSpecificOutput: {
      hookEventName: 'UserPromptSubmit',
      additionalContext:
        '[Prompt Optimizer] The user\'s prompt was automatically expanded for ' +
        'clarity. The user\'s original message remains the authoritative intent; ' +
        'use this optimized version to resolve ambiguity and fill in gaps:\n\n' +
        optimized,
    },
  };

  process.stdout.write(JSON.stringify(output));
  process.exit(0);
});
