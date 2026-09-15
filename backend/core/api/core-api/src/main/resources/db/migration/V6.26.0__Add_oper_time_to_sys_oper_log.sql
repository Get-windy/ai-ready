-- Add oper_time column to sys_oper_log table if not exists
-- Fixes: PSQLException "relation sys_oper_log column oper_time does not exist"
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS oper_time TIMESTAMP;
