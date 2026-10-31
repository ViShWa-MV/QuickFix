#!/usr/bin/env bash
#
# Stop the whole QuickFix stack (frees ports 8081/8082/8083/5173).

echo "Stopping QuickFix services..."
for port in 8081 8082 8083 5173; do
  if fuser -k "${port}/tcp" 2>/dev/null; then
    echo "  • freed :$port"
  fi
done
# Backstop in case anything was started differently.
pkill -f 'spring-boot:run' 2>/dev/null || true
pkill -f 'QuickFix-FrontEnd' 2>/dev/null || true
echo "✓ Stopped."
