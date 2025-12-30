#!/bin/bash

echo "Starting Maven build..."

# Clean and package the application
./mvnw clean package -DskipTests

echo "Build completed successfully!"
