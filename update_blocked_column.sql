-- Update all NULL blocked values to false
USE expense_tracker;

UPDATE users SET blocked = false WHERE blocked IS NULL;

-- Verify the update
SELECT COUNT(*) as total_users, 
       SUM(CASE WHEN blocked = 1 THEN 1 ELSE 0 END) as blocked_users,
       SUM(CASE WHEN blocked = 0 THEN 1 ELSE 0 END) as active_users,
       SUM(CASE WHEN blocked IS NULL THEN 1 ELSE 0 END) as null_blocked
FROM users;
