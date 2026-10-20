#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
case " $* " in
  *" verify "*|*" test "*)
    if [ -z "${B2BWEB_DB_URL:-}" ]; then exec python3 tools/dev/verify_backend.py "$@"; fi ;;
esac
if command -v mvn >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
  exec mvn "$@"
fi
exec python3 tools/dev/maven.py "$@"
