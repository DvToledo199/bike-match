ALTER TABLE users
    ADD COLUMN username VARCHAR(30) NOT NULL;

CREATE UNIQUE INDEX users_username_lower_unique
    ON users (LOWER(username));
ALTER TABLE users
    ADD CONSTRAINT users_username_length_check
        CHECK (char_length(username) BETWEEN 3 AND 30);