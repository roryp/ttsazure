@echo off
REM Script to load environment variables and run the Spring Boot application

REM Check if .env file exists
if exist .env (
    echo Loading environment variables from .env file...
    for /f "usebackq tokens=*" %%i in (`.env`) do (
        if not "%%i"=="" if not "%%i:~0,1%%"=="#" set %%i
    )
) else (
    echo Warning: .env file not found. Using default values or system environment variables.
)

REM Run the Spring Boot application
echo Starting Spring Boot application...
mvn spring-boot:run
