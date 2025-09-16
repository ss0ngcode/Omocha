package org.omocha.domain.auction;

public interface AuctionStore {
	Auction store(Auction auction);

	Auction storeAndFlush(Auction auction);

	void removeAuction(Auction auction);
}
