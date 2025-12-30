# Docker Deployment Guide

This guide explains how to run your Expense Tracker application using Docker and Docker Compose.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) installed
- [Docker Compose](https://docs.docker.com/compose/install/) (included with Docker Desktop)

## Files Created

1. **Dockerfile** - Multi-stage build for the Spring Boot application
2. **.dockerignore** - Excludes unnecessary files from Docker build
3. **docker-compose.yml** - Orchestrates MySQL and backend services

## Quick Start

### 1. Build and Run with Docker Compose

```bash
# Build and start all services
docker-compose up --build

# Or run in detached mode (background)
docker-compose up -d --build
```

This will:
- Start MySQL database on port 3307
- Build the Spring Boot application
- Start the backend on port 8086
- Wait for MySQL to be healthy before starting backend

### 2. Access the Application

**Backend API**: http://localhost:8086/expense-tracker-api

**API Endpoints**:
- Health Check: http://localhost:8086/expense-tracker-api/actuator/health
- Register: POST http://localhost:8086/expense-tracker-api/auth/register
- Login: POST http://localhost:8086/expense-tracker-api/auth/login

### 3. View Logs

```bash
# View all logs
docker-compose logs

# View backend logs only
docker-compose logs backend

# Follow logs in real-time
docker-compose logs -f backend

# View MySQL logs
docker-compose logs mysql
```

### 4. Stop Services

```bash
# Stop services (preserves data)
docker-compose stop

# Stop and remove containers (preserves volumes)
docker-compose down

# Stop and remove everything including volumes (WARNING: deletes data)
docker-compose down -v
```

## Docker Commands (Without Docker Compose)

### Build the Image

```bash
# Build the Docker image
docker build -t expense-tracker-backend:latest .
```

### Run MySQL Container

```bash
# Run MySQL
docker run -d \
  --name expense-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=expense_tracker \
  -e MYSQL_USER=expense_tracker_user \
  -e MYSQL_PASSWORD=expense_password \
  -p 3307:3306 \
  mysql:8.0
```

### Run Backend Container

```bash
# Run the backend (after MySQL is ready)
docker run -d \
  --name expense-backend \
  --link expense-mysql:mysql \
  -e DATABASE_URL="jdbc:mysql://mysql:3306/expense_tracker?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC" \
  -e DB_USERNAME=expense_tracker_user \
  -e DB_PASSWORD=expense_password \
  -e JWT_SECRET=mySecretKey123456789012345678901234567890123456789012345678901234567890 \
  -e MAIL_HOST=smtp.gmail.com \
  -e MAIL_PORT=587 \
  -e MAIL_USERNAME=your-email@gmail.com \
  -e MAIL_PASSWORD=your-app-password \
  -e MAIL_FROM=your-email@gmail.com \
  -p 8086:8086 \
  expense-tracker-backend:latest
```

## Configuration

### Update Email Settings

Edit [docker-compose.yml](docker-compose.yml) and update:

```yaml
MAIL_USERNAME: your-email@gmail.com
MAIL_PASSWORD: your-app-password  # Use Gmail App Password
MAIL_FROM: your-email@gmail.com
```

### Change Database Credentials

Edit [docker-compose.yml](docker-compose.yml) in the MySQL service:

```yaml
MYSQL_ROOT_PASSWORD: your-root-password
MYSQL_PASSWORD: your-user-password
```

Update the backend service environment variables accordingly.

### Modify Port Mappings

If ports 8086 or 3307 are already in use:

```yaml
ports:
  - "9090:8086"  # Map to different host port
```

## Dockerfile Explanation

### Multi-Stage Build

**Stage 1: Build**
- Uses Maven with Java 17
- Downloads dependencies (cached layer)
- Builds the application JAR

**Stage 2: Runtime**
- Uses lightweight JRE Alpine image
- Copies only the JAR file
- Runs as non-root user (security)
- Optimized for size (~200MB vs ~800MB)

### Security Features

- Non-root user execution
- Minimal Alpine base image
- Only necessary files copied
- Health check included

## Docker Compose Features

### Service Dependencies

```yaml
depends_on:
  mysql:
    condition: service_healthy
```

Backend waits for MySQL to be healthy before starting.

### Health Checks

Both services have health checks:
- **MySQL**: Checks database availability
- **Backend**: Checks Spring Boot health endpoint

### Persistent Data

```yaml
volumes:
  mysql_data:
```

Database data persists between container restarts.

### Networking

```yaml
networks:
  expense-tracker-network:
```

Isolated network for service communication.

## Troubleshooting

### Backend Can't Connect to Database

**Check MySQL is running:**
```bash
docker-compose ps
```

**Check MySQL logs:**
```bash
docker-compose logs mysql
```

**Verify database credentials match**

### Build Fails

**Clear Maven cache and rebuild:**
```bash
docker-compose down
docker-compose build --no-cache
docker-compose up
```

### Port Already in Use

**Find and stop conflicting process:**
```bash
# Windows
netstat -ano | findstr :8086
taskkill /PID <PID> /F

# Change port in docker-compose.yml
ports:
  - "9090:8086"
```

### Application Not Starting

**Check logs:**
```bash
docker-compose logs backend
```

**Common issues:**
- Missing environment variables
- Database not ready (increase start_period in healthcheck)
- Insufficient memory (increase Docker Desktop resources)

### Email Not Working

**Verify Gmail App Password:**
1. Use App Password, not regular password
2. Enable 2FA on Gmail account
3. Generate App Password in Google Account settings

## Useful Commands

```bash
# View running containers
docker-compose ps

# Restart a specific service
docker-compose restart backend

# Rebuild a specific service
docker-compose up -d --build backend

# Execute command in container
docker-compose exec backend bash

# Check container resource usage
docker stats

# Remove all stopped containers
docker container prune

# Remove unused images
docker image prune

# View container details
docker inspect expense-tracker-backend
```

## Production Deployment

For production on Render or other platforms:

1. **Use the Dockerfile** for building
2. **Set environment variables** in platform dashboard
3. **Use managed database** (Render MySQL, AWS RDS, etc.)
4. **Enable SSL/HTTPS** at platform level
5. **Set proper resource limits**

See [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md) for Render-specific deployment.

## Development Workflow

### Hot Reload (Development Mode)

For development with hot reload, mount source code:

```yaml
volumes:
  - ./src:/app/src
  - ./target:/app/target
```

Then use Spring Boot DevTools in [pom.xml](pom.xml).

### Running Tests

```bash
# Run tests in container
docker-compose exec backend ./mvnw test
```

## Resource Requirements

**Minimum:**
- 2GB RAM
- 10GB disk space

**Recommended:**
- 4GB RAM
- 20GB disk space

Adjust Docker Desktop resources in Settings → Resources.

## Next Steps

1. ✅ Build and run with Docker Compose
2. ✅ Test API endpoints
3. ✅ Configure email settings
4. ✅ Set up frontend connection
5. ✅ Deploy to production (see RENDER_DEPLOYMENT.md)

## Support

- [Docker Documentation](https://docs.docker.com)
- [Docker Compose Documentation](https://docs.docker.com/compose)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker)
