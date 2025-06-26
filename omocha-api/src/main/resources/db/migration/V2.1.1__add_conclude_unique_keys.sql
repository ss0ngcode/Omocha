ALTER TABLE conclude
    ADD CONSTRAINT uk_conclude_auction UNIQUE (auction_id);