#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    export $(grep -v '^#' .env | xargs)
else
    echo "Warning: .env file not found!"
fi

# Navigate to backend directory
cd backend

# Run Spring Boot application
echo "Starting backend with OpenAI API key loaded..."
mvn spring-boot:run