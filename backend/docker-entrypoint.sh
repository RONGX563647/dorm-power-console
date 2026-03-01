#!/bin/sh
set -e

echo "=========================================="
echo "  Dorm Power Backend Starting..."
echo "=========================================="

echo "Environment Variables:"
echo "  DB_HOST: ${DB_HOST}"
echo "  DB_PORT: ${DB_PORT}"
echo "  DB_NAME: ${DB_NAME}"
echo "  DB_USERNAME: ${DB_USERNAME}"
echo "  DB_PASSWORD: ${#DB_PASSWORD} characters"
echo "  JWT_SECRET: ${#JWT_SECRET} characters"

echo ""
echo "Waiting for PostgreSQL to be ready..."

max_attempts=60
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" > /dev/null 2>&1; then
        echo "✅ PostgreSQL is ready!"
        break
    fi
    
    attempt=$((attempt + 1))
    echo "Waiting for PostgreSQL... (attempt $attempt/$max_attempts)"
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ Error: PostgreSQL is not ready after $max_attempts attempts"
    exit 1
fi

echo ""
echo "Testing database connection..."

export PGPASSWORD="$DB_PASSWORD"

if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ Database connection successful!"
else
    echo "❌ Error: Cannot connect to database"
    echo "Attempting to diagnose..."
    
    echo "Checking if database exists..."
    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d postgres -c "SELECT 1 FROM pg_database WHERE datname='$DB_NAME';" | grep -q 1; then
        echo "Database exists"
    else
        echo "Database does not exist, creating..."
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d postgres -c "CREATE DATABASE $DB_NAME;" || true
    fi
    
    echo "Retrying connection..."
    if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
        echo "✅ Database connection successful after retry!"
    else
        echo "❌ Error: Still cannot connect to database"
        exit 1
    fi
fi

unset PGPASSWORD

echo ""
echo "Starting Spring Boot application..."
echo "=========================================="

exec java $JAVA_OPTS \
    -Dspring.datasource.url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
    -Dspring.datasource.username="${DB_USERNAME}" \
    -Dspring.datasource.password="${DB_PASSWORD}" \
    -Dsecurity.jwt.secret="${JWT_SECRET}" \
    -jar target/dorm-power-backend-1.0.0.jar
