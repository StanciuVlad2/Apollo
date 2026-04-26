#!/usr/bin/env bash
set -euo pipefail

SERVICES=(auth-service operations-service reservations-service feedback-service cocktails-service api-gateway eureka-server)
NO_BOOTJAR=(eureka-server)

usage() {
  echo "Usage: $0 [all | <service-name>]"
  echo "  all              — build and redeploy every service"
  echo "  <service-name>   — build and redeploy one service"
  echo ""
  echo "Services: ${SERVICES[*]}"
  exit 1
}

needs_bootjar() {
  local svc=$1
  for s in "${NO_BOOTJAR[@]}"; do [[ "$s" == "$svc" ]] && return 1; done
  return 0
}

build_and_deploy() {
  local svc=$1
  echo "▶ Building $svc..."
  if needs_bootjar "$svc"; then
    ./gradlew ":${svc}:bootJar" -q
  fi
  echo "▶ Docker build + restart $svc..."
  docker compose build --no-cache "$svc"
  docker compose up -d --no-deps "$svc"
  echo "✓ $svc deployed"
}

[[ $# -lt 1 ]] && usage

TARGET=$1

if [[ "$TARGET" == "all" ]]; then
  echo "▶ Building all JARs..."
  ./gradlew bootJar -q
  echo "▶ Docker build + restart all services..."
  docker compose build --no-cache
  docker compose up -d
  echo "✓ All services deployed"
else
  VALID=false
  for s in "${SERVICES[@]}"; do [[ "$s" == "$TARGET" ]] && VALID=true && break; done
  if [[ "$VALID" == false ]]; then
    echo "Unknown service: $TARGET"
    usage
  fi
  build_and_deploy "$TARGET"
fi
