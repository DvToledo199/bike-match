-- A moderator's removal deletes the bike completely, so the notice keeps what its owner
-- needs to recognise it, plus the reason, until the owner dismisses it.
CREATE TABLE bike_removal_notices (
    id            BIGSERIAL PRIMARY KEY,
    owner_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    brand         VARCHAR(80)  NOT NULL,
    model         VARCHAR(100) NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    removed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    dismissed_at  TIMESTAMPTZ,

    CONSTRAINT bike_removal_notices_reason_not_blank_check
        CHECK (char_length(btrim(reason)) > 0)
);

CREATE INDEX bike_removal_notices_owner_id_index ON bike_removal_notices (owner_id);
