#!/usr/bin/env node
// tool_call hook — logs every tool call with a short task summary to
// .claude/hooks/tool-calls.log.
// Invoked as a PreToolUse hook; receives the hook payload as JSON on stdin.

let raw = "";
process.stdin.on("data", (d) => (raw += d));
process.stdin.on("end", () => {
  let payload;
  try {
    payload = JSON.parse(raw);
  } catch {
    process.exit(0); // never break the tool call on a parse error
  }

  const tool = payload.tool_name || "unknown";
  const input = payload.tool_input || {};

  // Derive a human-readable "task summary" from whichever field the tool carries.
  const summary =
    input.description || // Bash, Task/Agent
    input.command || // Bash (fallback)
    input.pattern || // Grep/Glob
    input.query || // WebSearch
    input.url || // WebFetch
    input.file_path || // Write/Edit/Read
    input.path || // Glob/Grep path
    input.prompt || // Agent/Task
    "(no summary)";

  const line =
    JSON.stringify({
      time: new Date().toISOString(),
      session: payload.session_id || null,
      tool,
      summary: String(summary).replace(/\s+/g, " ").slice(0, 300),
    }) + "\n";

  const fs = require("fs");
  const path = require("path");
  const logFile = path.join(__dirname, "tool-calls.log");
  try {
    fs.appendFileSync(logFile, line);
  } catch {
    // swallow — logging must never block the tool
  }
  process.exit(0);
});
