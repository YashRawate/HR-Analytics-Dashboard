@echo off
REM NMDC HR Analytics - Java/Spring Boot launcher
REM Builds (if needed) and runs the Spring Boot app on http://localhost:5000

echo ========================================================
echo   NMDC HR Analytics Intelligence - Java Backend
echo ========================================================

if not exist "Users.xlsx" (
    echo [WARN] Users.xlsx not found in project root.
    echo        Copy Users.sample.xlsx to Users.xlsx and edit credentials,
    echo        or login will fail.
)

mvn spring-boot:run

pause
