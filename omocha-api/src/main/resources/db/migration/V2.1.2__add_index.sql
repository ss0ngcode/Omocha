CREATE INDEX idx_bid_auctionId ON bid (auction_id);

CREATE INDEX idx_auction_auctionStatus_endDate_auctionId ON auction (auction_status, end_date, auction_id);