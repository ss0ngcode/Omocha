package org.omocha.domain.auction;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpiredAuctionFetcherImpl implements ExpiredAuctionFetcher {

	private final AuctionReader auctionReader;

	@Override
	public List<Long> fetchExpiredAuctionIds() {
		return auctionReader.getExpiredBiddingAuctionList();
	}
}
