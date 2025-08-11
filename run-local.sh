#!/bin/bash
# Script to load environment variables and run the Spring Boot application

# Check if .env file exists
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    export $(cat .env | grep -v '^#' | xargs)
else
    echo "Warning: .env file not found. Using default values or system environment variables."
fi

# Run the Spring Boot application
echo "Starting Spring Boot application..."
mvn spring-boot:run
