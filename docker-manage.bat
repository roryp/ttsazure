@echo off
REM Docker management script for TTS Azure App

set CONTAINER_NAME=tts-app
set IMAGE_NAME=tts-azure-app

if "%1"=="build" goto build
if "%1"=="run" goto run
if "%1"=="stop" goto stop
if "%1"=="logs" goto logs
if "%1"=="status" goto status
if "%1"=="restart" goto restart
if "%1"=="clean" goto clean
goto help

:build
echo 🔨 Building Docker image...
docker build -t %IMAGE_NAME% .
echo ✅ Build complete!
goto end

:run
echo 🚀 Starting container with environment variables...
docker stop %CONTAINER_NAME% 2>nul
docker rm %CONTAINER_NAME% 2>nul
docker run -d --name %CONTAINER_NAME% --env-file .env -p 8080:8080 %IMAGE_NAME%
echo ✅ Container started!
echo 🌐 Application available at: http://localhost:8080
goto end

:stop
echo 🛑 Stopping container...
docker stop %CONTAINER_NAME%
echo ✅ Container stopped!
goto end

:logs
echo 📋 Showing container logs...
docker logs -f %CONTAINER_NAME%
goto end

:status
echo 📊 Container status:
docker ps -a --filter name=%CONTAINER_NAME%
goto end

:restart
echo 🔄 Restarting container...
docker restart %CONTAINER_NAME%
echo ✅ Container restarted!
goto end

:clean
echo 🧹 Cleaning up...
docker stop %CONTAINER_NAME% 2>nul
docker rm %CONTAINER_NAME% 2>nul
docker rmi %IMAGE_NAME% 2>nul
echo ✅ Cleanup complete!
goto end

:help
echo 🚀 TTS Azure App Docker Management
echo.
echo Usage: %0 {build^|run^|stop^|logs^|status^|restart^|clean}
echo.
echo Commands:
echo   build   - Build the Docker image
echo   run     - Run the container with .env file
echo   stop    - Stop the running container
echo   logs    - Show container logs (follow mode)
echo   status  - Show container status
echo   restart - Restart the container
echo   clean   - Stop and remove container and image
echo.
echo Examples:
echo   %0 build    # Build the image
echo   %0 run      # Start the application
echo   %0 logs     # Watch the logs

:end
