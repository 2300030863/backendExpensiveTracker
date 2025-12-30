-- SQL Script to fix the blocked column in users table
-- Run this script in your MySQL database

-- Add the blocked column if it doesn't exist (with default value false)
ALTER TABLE users ADD COLUMN IF NOT EXISTS blocked BOOLEAN DEFAULT false;

-- Update all NULL values to false
UPDATE users SET blocked = false WHERE blocked IS NULL;

-- Update all existing users to ensure they are not blocked by default
UPDATE users SET blocked = false;

-- Verify the update
SELECT id, username, email, role, blocked FROM users;
