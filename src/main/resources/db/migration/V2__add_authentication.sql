ALTER TABLE person ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE person ADD COLUMN IF NOT EXISTS role VARCHAR(50) NOT NULL DEFAULT 'USER';

CREATE INDEX IF NOT EXISTS idx_listing_published_at ON listing (published_at);
CREATE INDEX IF NOT EXISTS idx_listing_area ON listing (area);
CREATE INDEX IF NOT EXISTS idx_listing_state ON listing (state);
CREATE INDEX IF NOT EXISTS idx_favorite_person ON favorite (person_id);
CREATE INDEX IF NOT EXISTS idx_saved_search_person ON saved_search (person_id);
CREATE INDEX IF NOT EXISTS idx_message_sender ON message (sender_id);
CREATE INDEX IF NOT EXISTS idx_message_recipient ON message (recipient_id);
