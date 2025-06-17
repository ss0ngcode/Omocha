package org.omocha.domain.auction;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpiredAuctionFetcherImpl implements ExpiredAuctionFetcher {

	private final AuctionReader auctionReader;

	@Override
	public List<Long> fetchExpiredAuctionIds() {
		List<Auction> expiredBiddingAuctionList = auctionReader.getExpiredBiddingAuctionList();

		return expiredBiddingAuctionList.stream()
			.map(Auction::getAuctionId)
			.collect(Collectors.toList());
	}
}
