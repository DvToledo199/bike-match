#!/bin/sh
# Runs only when the PostgreSQL volume is created for the first time.
# The backend tests use this separate database, never the development one.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -c "CREATE DATABASE bikematch_test"
