UPDATE users u
SET email_verified = TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM email_verification_codes c WHERE c.user_id = u.id
);
