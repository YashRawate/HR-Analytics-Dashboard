#!/usr/bin/env bash
# NMDC HR Analytics - Java/Spring Boot launcher
# Builds (if needed) and runs the Spring Boot app on http://localhost:5000
set -e

echo "========================================================"
echo "  NMDC HR Analytics Intelligence - Java Backend"
echo "========================================================"

if [ ! -f "Users.xlsx" ]; then
    echo "[WARN] Users.xlsx not found in project root."
    echo "       Copy Users.sample.xlsx to Users.xlsx and edit credentials,"
    echo "       or login will fail."
fi

mvn spring-boot:run
