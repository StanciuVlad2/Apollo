#!/bin/bash
until pg_isready -h "$HOST" -p "$PORT" -d "$DB" -U "$USER" >/dev/null 2>&1; do
  echo "Waiting for PostgreSQL at $HOST:$PORT..."
  sleep 1
done
done
echo "PostgreSQL is up - starting app"
exec java -Dspring.profiles.active=docker -jar /app/app.jar

