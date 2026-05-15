#!/bin/sh

set -eu

if [ "$#" -eq 0 ]; then
    echo "usage: scripts/record-commit-prompt.sh \"prompt text\"" >&2
    exit 1
fi

git_dir=$(git rev-parse --git-dir)
prompt_log="$git_dir/codex-commit-prompts.md"

printf -- '- %s\n' "$*" >> "$prompt_log"
