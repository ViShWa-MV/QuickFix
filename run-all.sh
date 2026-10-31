#!/usr/bin/env bash
#
# Run the whole QuickFix stack: 3 Spring Boot backends, then the Vite frontend.
#
#   ./run-all.sh          start everything (Ctrl+C stops all)
#
# Backends run on Java 25; the frontend on Node. Per-service logs are written
# to ./logs/. Databases are created if missing (password read from the local
# catalog-service application.properties, so no secret lives in this script).

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jdk-25.0.2}"
export JAVA_HOME
LOG_DIR="$ROOT/logs"
mkdir -p "$LOG_DIR"

PIDS=()

cleanup() {
  echo ""
  echo "⏹  Stopping all services..."
  # Kill whatever is listening on our ports (reliable regardless of process tree).
  for port in 8081 8082 8083 5173; do
    fuser -k "${port}/tcp" 2>/dev/null || true
  done
  kill "${PIDS[@]}" 2>/dev/null || true
  wait 2>/dev/null || true
  echo "✓  Stopped."
}
trap cleanup INT TERM

# --- sanity: JDK 25 ---
if [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "✗ JDK not found at JAVA_HOME=$JAVA_HOME"
  echo "  Set JAVA_HOME to your Java 25 install, e.g.:"
  echo "    JAVA_HOME=/path/to/jdk-25 ./run-all.sh"
  exit 1
fi
echo "JAVA_HOME=$JAVA_HOME"
"$JAVA_HOME/bin/java" -version 2>&1 | head -1

# --- ensure databases exist (best effort) ---
DB_PW="$(grep -m1 '^spring.datasource.password=' \
  "$ROOT/catalog-service/src/main/resources/application.properties" 2>/dev/null \
  | cut -d= -f2- || true)"
if command -v psql >/dev/null 2>&1 && [ -n "$DB_PW" ]; then
  for db in quickfix_user quickfix_catalog quickfix_booking; do
    exists="$(PGPASSWORD="$DB_PW" psql -h localhost -U postgres -tAc \
      "SELECT 1 FROM pg_database WHERE datname='$db'" 2>/dev/null || true)"
    if [ "$exists" != "1" ]; then
      echo "• creating database $db"
      PGPASSWORD="$DB_PW" psql -h localhost -U postgres -c "CREATE DATABASE $db" >/dev/null 2>&1 || true
    fi
  done
fi

wait_for_port() { # port name
  local port="$1" name="$2" i=0
  printf "  waiting for %s (:%s)" "$name" "$port"
  until (exec 3<>"/dev/tcp/localhost/$port") 2>/dev/null; do
    if [ "$i" -ge 90 ]; then echo " TIMEOUT — see $LOG_DIR/$name.log"; return 1; fi
    printf "."; sleep 2; i=$((i + 1))
  done
  exec 3>&- 2>/dev/null || true
  echo " up"
}

start_backend() { # name  mvn-args...
  local name="$1"; shift
  echo "▶ starting $name"
  ( cd "$ROOT" && ./mvnw "$@" spring-boot:run ) > "$LOG_DIR/$name.log" 2>&1 &
  PIDS+=("$!")
}

echo ""
echo "Starting backends..."
start_backend user-service
start_backend catalog-service -f catalog-service/pom.xml
start_backend booking-service -f booking-service/pom.xml

wait_for_port 8081 user-service
wait_for_port 8082 catalog-service
wait_for_port 8083 booking-service

echo ""
echo "Starting frontend..."
if [ ! -d "$ROOT/QuickFix-FrontEnd/node_modules" ]; then
  echo "• installing frontend dependencies (first run)"
  ( cd "$ROOT/QuickFix-FrontEnd" && npm install ) > "$LOG_DIR/frontend-install.log" 2>&1
fi
( cd "$ROOT/QuickFix-FrontEnd" && npm run dev ) > "$LOG_DIR/frontend.log" 2>&1 &
PIDS+=("$!")
wait_for_port 5173 frontend

cat <<EOF

✅ QuickFix is up:
   Frontend          http://localhost:5173
   user-service      http://localhost:8081
   catalog-service   http://localhost:8082
   booking-service   http://localhost:8083

   Logs:  $LOG_DIR/
   Stop:  press Ctrl+C here (or run ./stop-all.sh)
EOF

# Keep running in the foreground so Ctrl+C triggers cleanup.
wait
