#!/usr/bin/env bash
#
# run-codeql.sh: run CodeQL code scanning locally with the same languages, build modes,
# query suite, and config as .github/workflows/codeql.yml, so findings can be caught
# before pushing.
#
# Languages: js (JavaScript/TypeScript + HTML), python, java, csharp, go.
# Query suite and paths-ignore: .github/codeql/codeql-config.yml (security-extended).
#
# Prerequisites:
#   - The official CodeQL bundle (codeql-bundle-<platform>.tar.gz from the
#     github/codeql-action releases), extracted to ~/codeql or pointed to by CODEQL_BIN.
#     On macOS, prefer the bundle over the Homebrew cask, and clear the quarantine flag:
#       xattr -r -d com.apple.quarantine ~/codeql
#   - Go toolchain on PATH, only for the go language (Go is built with autobuild).
#   Java and C# are analyzed without building (build mode "none"), so no JDK or .NET
#   SDK is needed.
#
# Usage:
#   ./scripts/run-codeql.sh                 # js python java csharp go
#   ./scripts/run-codeql.sh js python       # any subset
#
# Output:
#   $CODEQL_DB_DIR/<lang>-db/     CodeQL databases (default: $TMPDIR/codeql-dbs/<repo>, outside the repo)
#   .codeql/<lang>.sarif          raw SARIF results (gitignored)
#   .codeql/<lang>.csv            human-readable findings
#   A combined findings table is printed at the end.
#
set -euo pipefail

# --- Resolve repo root (script lives in <root>/scripts) ---
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# --- CodeQL CLI location (override with CODEQL_BIN) ---
: "${CODEQL_BIN:=$HOME/codeql/codeql}"
# The bundle is usually extracted under $HOME; silence the "installed in home directory" advisory.
export CODEQL_ALLOW_INSTALLATION_ANYWHERE=true

OUT_DIR="$REPO_ROOT/.codeql"
mkdir -p "$OUT_DIR"
# Databases live outside the repo: build-mode "none" scans the whole source root, so a
# database inside it (with its copied sources and downloaded SDKs) would be scanned too.
: "${CODEQL_DB_DIR:=${TMPDIR:-/tmp}/codeql-dbs/$(basename "$REPO_ROOT")}"
mkdir -p "$CODEQL_DB_DIR"

SUITE_SUFFIX="security-extended"
CODEQL_CONFIG="$REPO_ROOT/.github/codeql/codeql-config.yml"

if [[ $# -gt 0 ]]; then
  LANGS=("$@")
else
  LANGS=(js python java csharp go)
fi

if [[ ! -x "$CODEQL_BIN" ]]; then
  echo "ERROR: CodeQL CLI not found at '$CODEQL_BIN'." >&2
  echo "       Install the official bundle (github/codeql-action releases) or set CODEQL_BIN." >&2
  exit 1
fi

# analyze <short-name> <codeql-language> <build-mode>
analyze() {
  local lang="$1" cql_lang="$2" build_mode="$3" db="$CODEQL_DB_DIR/${1}-db"

  echo "==================================================================="
  echo ">>> [$lang] creating database (build mode: $build_mode)…"
  echo "==================================================================="
  rm -rf "$db"
  local config_args=()
  # paths-ignore is applied when the database is created, so pass the config here.
  [[ -f "$CODEQL_CONFIG" ]] && config_args=(--codescanning-config="$CODEQL_CONFIG")
  "$CODEQL_BIN" database create "$db" \
    --language="$cql_lang" \
    --build-mode="$build_mode" \
    --source-root="$REPO_ROOT" \
    --overwrite \
    ${config_args[@]+"${config_args[@]}"}

  local suite="codeql/${cql_lang}-queries:codeql-suites/${cql_lang}-${SUITE_SUFFIX}.qls"
  echo ">>> [$lang] analyzing with ${suite}…"
  "$CODEQL_BIN" database analyze "$db" "$suite" \
    --format=sarif-latest \
    --output="$OUT_DIR/${lang}.sarif" \
    --sarif-add-snippets \
    --threads=0

  # Also emit a human-readable CSV.
  "$CODEQL_BIN" database analyze "$db" "$suite" \
    --format=csv \
    --output="$OUT_DIR/${lang}.csv" \
    --threads=0 \
    --rerun >/dev/null 2>&1 || true
}

for lang in "${LANGS[@]}"; do
  case "$lang" in
    js|javascript) analyze "js" "javascript" "none" ;;
    python|py)     analyze "python" "python" "none" ;;
    java)          analyze "java" "java" "none" ;;
    csharp|cs)     analyze "csharp" "csharp" "none" ;;
    go)
      if command -v go >/dev/null 2>&1; then
        analyze "go" "go" "autobuild"
      else
        echo "WARN: Go toolchain not on PATH; skipping go (CI still analyzes it)." >&2
      fi
      ;;
    *)
      echo "WARN: unknown language '$lang' (expected: js|python|java|csharp|go); skipping" >&2
      ;;
  esac
done

# --- Combined summary ---
echo
echo "==================================================================="
echo ">>> CodeQL findings summary"
echo "==================================================================="
total=0
for lang in "${LANGS[@]}"; do
  case "$lang" in javascript) lang=js ;; py) lang=python ;; cs) lang=csharp ;; esac
  sarif="$OUT_DIR/${lang}.sarif"
  [[ -f "$sarif" ]] || continue
  count=$(jq '[.runs[].results[]] | length' "$sarif" 2>/dev/null || echo 0)
  total=$((total + count))
  echo
  echo "### $lang: $count finding(s)"
  if [[ "$count" -gt 0 ]]; then
    jq -r '.runs[].results[]
      | "  [\(.rule.id // .ruleId)] "
        + (.locations[0].physicalLocation.artifactLocation.uri // "?")
        + ":" + ((.locations[0].physicalLocation.region.startLine // 0)|tostring)' \
      "$sarif" 2>/dev/null | sort | uniq
  fi
done
echo
echo ">>> TOTAL: $total finding(s). SARIF/CSV written to $OUT_DIR/"
echo ">>> Note: alerts dismissed on GitHub still appear in local results."
