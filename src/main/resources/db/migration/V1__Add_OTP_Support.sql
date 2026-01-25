-- Add phone verification fields to users table
ALTER TABLE users ADD COLUMN phone_verified BIT DEFAULT 0;
ALTER TABLE users ADD COLUMN phone_verification_date DATETIME2;

-- Create OTP verifications table
CREATE TABLE otp_verifications (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    phone_number NVARCHAR(20) NOT NULL,
    otp_code NVARCHAR(255) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    expires_at DATETIME2 NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    verified BIT NOT NULL DEFAULT 0,
    blocked BIT NOT NULL DEFAULT 0
);

-- Create indexes for performance
CREATE INDEX idx_phone_number ON otp_verifications(phone_number);
CREATE INDEX idx_expires_at ON otp_verifications(expires_at);
CREATE INDEX idx_created_at ON otp_verifications(created_at);

-- Add comments for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Stores OTP verification codes for phone number verification',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'otp_verifications';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Phone number to verify',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'otp_verifications',
    @level2type = N'COLUMN', @level2name = N'phone_number';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Encrypted OTP code',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'otp_verifications',
    @level2type = N'COLUMN', @level2name = N'otp_code';