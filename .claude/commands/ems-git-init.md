---
argument-hint: [remote-url] [branch-name]
description: Initialize git in this local project and do the first commit/push
---
This project is not yet under git. Do the following:

1. Check if a `.git` folder already exists — if so, stop and tell me instead of proceeding.
2. Run `git init` in the current directory.
3. Inspect the project and generate a sensible `.gitignore` for its stack (node_modules, venv, build artifacts, .env, etc.) — create one if it doesn't exist.
4. Stage all files with `git add .`.
5. Create an initial commit with a clear message summarizing what the project is (infer from README/package.json/etc.), e.g. "Initial commit".
6. Set the default branch to $2 (default to `main` if not provided).
7. If $1 is provided, add it as the `origin` remote and push the branch to it.
8. If $1 is not provided, just tell me the repo is initialized locally and remind me to add a remote later with `git remote add origin <url>`.

Show me the commands you ran and their output.