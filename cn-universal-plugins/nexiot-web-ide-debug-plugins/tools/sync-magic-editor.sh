#!/usr/bin/env bash
# Build the standalone Magic IDE source and copy its output into the backend plugin.
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/../../.." && pwd)"
source_dir="${1:-$repo_root/../magic-editor}"
source_dir="$(cd "$source_dir" && pwd)"
target_dir="$script_dir/../src/main/resources/magic-editor"
[[ -f "$source_dir/src/scripts/platform-session.mjs" ]] || {
  echo "Not the SSO-enabled Magic IDE source: $source_dir" >&2
  exit 1
}
(
  cd "$source_dir"
  node --test tests/*.test.mjs
  npm run build
)
# Vite may leave whitespace-only lines when replacing module script tags.
node -e 'const fs=require("node:fs");const p=process.argv[1];fs.writeFileSync(p,fs.readFileSync(p,"utf8").replace(/[\t ]+$/gm,""));' "$source_dir/dist/index.html"
[[ -f "$source_dir/dist/index.html" ]] || { echo 'Missing dist/index.html' >&2; exit 1; }
# Do not clean the target: preserve backend-specific resources and cached old hashed assets.
cp -R "$source_dir/dist/." "$target_dir/"
echo "Magic IDE synchronized: $source_dir/dist -> $target_dir"
