#!/usr/bin/env bash
set -euo pipefail

SERVICES=(auth-service operations-service reservations-service feedback-service cocktails-service reports-service api-gateway eureka-server)
NO_BOOTJAR=(eureka-server)

usage() {
  echo "Usage: $0 [all | <service-name> | clean]"
  echo "  all              — build and redeploy every service"
  echo "  <service-name>   — build and redeploy one service"
  echo "  clean            — remove images and build cache for THIS project"
  echo ""
  echo "Services: ${SERVICES[*]}"
  exit 1
}

# Funcție pentru curățenie chirurgicală (fără să strici alte proiecte de muncă)
cleanup_project() {
  echo "🧹 Cleaning up project: odin-restaurant-microservices..."

  # Oprește containerele și șterge volumele anonime ale proiectului
  docker compose down --remove-orphans

  # Șterge doar imaginile care aparțin acestui proiect
  local project_images
  project_images=$(docker images -q --filter "label=com.docker.compose.project=odin-restaurant-microservices")
  if [[ -n "$project_images" ]]; then
    docker rmi -f $project_images
  fi

  # Curăță cache-ul de build care nu a fost folosit de 24h (sigur pentru alte proiecte)
  docker builder prune --filter "until=24h" -f

  # Curățare locală Gradle
  ./gradlew clean

  echo "✨ Clean completed!"
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

  echo "▶ Docker build $svc..."
  # AM SCOS --no-cache pentru a salva spațiu și timp
  docker compose build "$svc"

  # Ștergem doar imaginile "dangling" (fără nume) lăsate în urmă de acest build
  docker image prune -f --filter "label=com.docker.compose.project=odin-restaurant-microservices"

  docker compose up -d --no-deps "$svc"
  echo "✓ $svc deployed"
}

[[ $# -lt 1 ]] && usage

TARGET=$1

case "$TARGET" in
  all)
    echo "▶ Building all JARs..."
    ./gradlew bootJar -q
    echo "▶ Docker build all services..."
    docker compose build
    docker compose up -d
    echo "✓ All services deployed"
    ;;
  clean)
    cleanup_project
    ;;
  *)
    # Verifică dacă serviciul e valid
    VALID=false
    for s in "${SERVICES[@]}"; do [[ "$s" == "$TARGET" ]] && VALID=true && break; done
    if [[ "$VALID" == false ]]; then
      echo "Unknown service: $TARGET"
      usage
    fi
    build_and_deploy "$TARGET"
    ;;
esac