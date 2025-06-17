package org.omocha.domain.conclude;

import java.util.Optional;

import org.omocha.domain.auction.Auction;
import org.omocha.domain.auction.AuctionReader;
import org.omocha.domain.bid.Bid;
import org.omocha.domain.bid.BidReader;
import org.omocha.domain.member.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConcludeProcessorImpl implements ConcludeProcessor {

	private final AuctionReader auctionReader;
	private final BidReader bidReader;
	private final ConcludeStore concludeStore;

	@Override
	@Transactional
	public ConcludeInfo.ConcludeResult processConclusion(Long expiredAuctionId) {

		Auction auction = auctionReader.getAuction(expiredAuctionId);

		Optional<Bid> optionalHighestBid = bidReader.findHighestBid(expiredAuctionId);

		return optionalHighestBid.map(bid -> handleSuccessfulConclusion(auction, bid))
			.orElseGet(() -> handleNoBidsConclusion(auction));
	}

	private ConcludeInfo.ConcludeResult handleSuccessfulConclusion(Auction auction, Bid highestBid) {
		Member buyer = highestBid.getBuyer();

		concludeStore.concludeAuctionWithBid(
			auction,
			highestBid
		);

		return ConcludeInfo.ConcludeResult.toInfo(auction.getAuctionId(), buyer.getMemberId());
	}

	private ConcludeInfo.ConcludeResult handleNoBidsConclusion(Auction auction) {
		concludeStore.concludeAuctionWithNoBids(auction);

		return ConcludeInfo.ConcludeResult.toInfo(auction.getAuctionId(), null);
	}
}
