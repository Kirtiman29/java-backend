-- Rename 'username' to 'email' in the admin table
ALTER TABLE admin CHANGE username email VARCHAR(100) NOT NULL UNIQUE;

-- Add OTP columns to the admin table if they are missing
ALTER TABLE admin ADD COLUMN otp VARCHAR(6) NULL;
ALTER TABLE admin ADD COLUMN otp_expiry_date TIMESTAMP NULL;

-- Update the existing admin record with the specified developer email
UPDATE admin SET email = 'developer@ruchitadesigncompany.com' WHERE id = 1;
