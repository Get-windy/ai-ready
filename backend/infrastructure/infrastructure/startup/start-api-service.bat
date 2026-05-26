@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: AI-Ready API Service Startup Script for Windows
:: Usage: start-api-service.bat [port]

set "PORT=%~1"
if "%PORT%"=="" set "PORT=8080"

set "JAR_FILE=target\ai-ready-minimal-1.0.0-SNAPSHOT.jar"
set "LOG_DIR=..\logs"
set "LOG_FILE=%LOG_DIR%\api-service.log"

echo Starting AI-Ready API Service...
echo Port: %PORT%
echo Log: %LOG_FILE%

:: Create log directory if not exists
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

:: Check if JAR file exists
if not exist "%JAR_FILE%" (
    echo Error: JAR file not found at %JAR_FILE%
    echo Please build the project first: mvn clean package
    exit /b 1
)

:: Stop existing service on the same port
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
    echo Stopping existing service on port %PORT% (PID: %%a)...
    taskkill /F /PID %%a >nul 2>&1
    timeout /t 2 /nobreak >nul
)

:: Start the service
echo Starting Java application...
start /B java -jar "%JAR_FILE%" --server.port=%PORT% --spring.profiles.active=dev > "%LOG_FILE%" 2>&1

:: Get the PID (approximate for Windows)
for /f "tokens=2" %%a in ('tasklist ^| findstr "java.exe"') do (
    echo Service started with PID: %%a
)

:: Wait for service to be ready
echo Waiting for service to be ready...
for /L %%i in (1,1,30) do (
    curl -s http://localhost:%PORT%/actuator/health >nul 2>&1
    if !errorlevel! == 0 (
        echo Service is ready!
        echo Health check: http://localhost:%PORT%/actuator/health
        exit /b 0
    )
    timeout /t 1 /nobreak >nul
)

echo Warning: Service may not be fully started yet. Check logs: %LOG_FILE%
exit /b 1
