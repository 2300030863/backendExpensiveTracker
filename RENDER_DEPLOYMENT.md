# Render Deployment Guide - Expense Tracker Backend

This guide explains how to deploy your Expense Tracker backend application to Render with MySQL database.

## Prerequisites

1. **Render Account**: Sign up at [render.com](https://render.com)
2. **GitHub Repository**: Push your code to GitHub
3. **Gmail App Password**: For email functionality (if using Gmail)

## Configuration Changes Made

### 1. **pom.xml**
- Changed packaging from `war` to `jar` for standalone deployment
- Kept embedded Tomcat for running on Render

### 2. **application.properties**
- Added environment variable support with fallback values
- Configured for production deployment:
  - `${PORT:8086}` - Render assigns the PORT
  - `${JWT_SECRET:...}` - Secure JWT secret
  - Email settings with environment variables

### 3. **application-mysql.properties**
- MySQL connection uses environment variables:
  - `${DATABASE_URL:...}` - Database connection string
  - `${DB_USERNAME:...}` - Database username
  - `${DB_PASSWORD:...}` - Database password

### 4. **render.yaml**
- Defines web service and MySQL database
- Automatic environment variable linking
- Health check configuration

### 5. **build.sh**
- Maven build script for Render
- Cleans and packages the application

## Deployment Steps

### Step 1: Push to GitHub

```bash
git add .
git commit -m "Configure for Render deployment with MySQL"
git push origin main
```

### Step 2: Create Services on Render

#### Option A: Using render.yaml (Recommended)

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click **"New +"** → **"Blueprint"**
3. Connect your GitHub repository
4. Render will detect `render.yaml` and create both services automatically
5. Review the services and click **"Apply"**

#### Option B: Manual Setup

**Create MySQL Database:**
1. Click **"New +"** → **"MySQL"**
2. Name: `expense-tracker-db`
3. Database: `expense_tracker`
4. User: `expense_tracker_user`
5. Region: Choose closest to you (e.g., Oregon)
6. Plan: Free
7. Click **"Create Database"**

**Create Web Service:**
1. Click **"New +"** → **"Web Service"**
2. Connect your GitHub repository
3. Select the `backend` directory
4. Settings:
   - **Name**: `expense-tracker-backend`
   - **Region**: Same as database
   - **Branch**: `main`
   - **Root Directory**: Leave empty (or specify if repo has multiple projects)
   - **Environment**: `Java`
   - **Build Command**: `chmod +x build.sh && ./build.sh`
   - **Start Command**: `java -jar target/ExpenseTrackerApplication.jar`
   - **Plan**: Free

### Step 3: Configure Environment Variables

In the Web Service settings, add these environment variables:

**Required Variables:**
- `DATABASE_URL` - Copy from MySQL database "External Database URL"
- `DB_USERNAME` - Copy from MySQL database credentials
- `DB_PASSWORD` - Copy from MySQL database credentials
- `JWT_SECRET` - Generate a secure random string (64+ characters)

**Email Variables (Required for password reset):**
- `MAIL_USERNAME` - Your Gmail address
- `MAIL_PASSWORD` - Gmail App Password (not your regular password)
- `MAIL_FROM` - Sender email address

**Optional Variables (with defaults):**
- `MAIL_HOST` - smtp.gmail.com (default)
- `MAIL_PORT` - 587 (default)
- `DDL_AUTO` - update (default)
- `SHOW_SQL` - false (recommended for production)
- `LOG_LEVEL` - INFO (default)

### Step 4: Deploy

1. After configuring environment variables, click **"Create Web Service"**
2. Render will automatically:
   - Pull your code from GitHub
   - Run the build command
   - Start your application
   - Monitor health checks

3. Wait for deployment to complete (typically 5-10 minutes for first deploy)

### Step 5: Get Your Application URL

Once deployed, you'll get a URL like:
```
https://expense-tracker-backend.onrender.com
```

Your API will be available at:
```
https://expense-tracker-backend.onrender.com/expense-tracker-api
```

## Important Notes

### Database Connection

Render MySQL databases use this format:
```
mysql://user:password@host:port/database
```

The application is configured to automatically convert this to JDBC format.

### Cold Starts

Free tier services spin down after 15 minutes of inactivity. First request after inactivity may take 30-60 seconds.

### Logs

View logs in Render Dashboard:
1. Go to your web service
2. Click **"Logs"** tab
3. Monitor for errors or issues

### Health Checks

The application should respond at:
```
https://your-app.onrender.com/expense-tracker-api/actuator/health
```

If using Spring Boot Actuator, add this dependency to [pom.xml](pom.xml):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

And add to [application.properties](src/main/resources/application.properties):
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=when-authorized
```

## Troubleshooting

### Database Connection Issues

If you see connection errors:
1. Verify `DATABASE_URL` format is correct
2. Check database is running in Render dashboard
3. Ensure database and web service are in same region
4. Check database credentials are correct

### Build Failures

If Maven build fails:
1. Check Java version is 17
2. Ensure `mvnw` wrapper has execute permissions
3. Review build logs in Render dashboard

### Application Won't Start

1. Check logs for startup errors
2. Verify all required environment variables are set
3. Ensure database is accessible
4. Check JWT_SECRET is set and valid

### Email Not Working

1. Verify Gmail App Password (not regular password)
2. Enable "Less secure app access" or use App Password
3. Check SMTP settings are correct
4. Review email-related logs

## Testing the Deployment

### Test API Endpoints

```bash
# Health check
curl https://your-app.onrender.com/expense-tracker-api/actuator/health

# Register user
curl -X POST https://your-app.onrender.com/expense-tracker-api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!"
  }'

# Login
curl -X POST https://your-app.onrender.com/expense-tracker-api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!"
  }'
```

## Auto-Deploy on Git Push

Render automatically redeploys when you push to your GitHub repository:

```bash
git add .
git commit -m "Update feature"
git push origin main
```

Render will detect the push and start a new deployment.

## Updating Environment Variables

1. Go to your web service in Render dashboard
2. Click **"Environment"** tab
3. Update variables
4. Click **"Save Changes"**
5. Service will automatically restart

## Cost

- **Free Tier Limits**:
  - MySQL: 1GB storage, shared resources
  - Web Service: 750 hours/month, spins down after 15 min inactivity
  - Bandwidth: Limited

- **Paid Plans**: If you need always-on service, upgrade to paid plan ($7-25/month)

## Security Recommendations

1. **Use Strong JWT Secret**: Generate with `openssl rand -base64 64`
2. **Secure Database Password**: Use Render's auto-generated passwords
3. **Environment Variables**: Never commit secrets to Git
4. **HTTPS Only**: Render provides free SSL certificates
5. **Update Dependencies**: Keep Spring Boot and libraries updated

## Support

- [Render Documentation](https://render.com/docs)
- [Render Community](https://community.render.com)
- [Spring Boot on Render](https://render.com/docs/deploy-spring-boot)

---

## Quick Reference

**Build Command:**
```bash
chmod +x build.sh && ./build.sh
```

**Start Command:**
```bash
java -jar target/ExpenseTrackerApplication.jar
```

**Application URL Format:**
```
https://<service-name>.onrender.com/expense-tracker-api
```

**Database Connection:**
- Render provides: `mysql://user:pass@host:port/dbname`
- App converts to: `jdbc:mysql://host:port/dbname?params`
