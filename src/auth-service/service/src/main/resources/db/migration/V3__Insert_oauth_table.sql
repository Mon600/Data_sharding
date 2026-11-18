CREATE TABLE IF NOT EXISTS oauth_identities (
    id UUID PRIMARY KEY,
    provider VARCHAR(255) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT fk_oauth_identities_users
     FOREIGN KEY (user_id)
     REFERENCES users(id)
     ON DELETE CASCADE,

    CONSTRAINT unique_oauth_identities_provider UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_oauth_identities_user ON oauth_identities(user_id);

