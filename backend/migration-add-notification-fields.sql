-- Add new columns to notifications table
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS title VARCHAR(255) NOT NULL DEFAULT 'Notification';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS type VARCHAR(100);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS proposal_id BIGINT;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_proposal_id ON notifications(proposal_id);