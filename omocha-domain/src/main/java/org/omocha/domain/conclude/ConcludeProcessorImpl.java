package org.omocha.domain.conclude;

import java.util.Optional;

import org.omocha.domain.auction.Auction;
import org.omocha.domain.auction.AuctionReader;
import org.omocha.domain.bid.Bid;
import org.omocha.domain.bid.BidReader;
import org.omocha.domain.member.Member;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcludeProcessorImpl implements ConcludeProcessor {

	private final AuctionReader auctionReader;
	private final BidReader bidReader;
	private final ConcludeStore concludeStore;

	@Override
	@Transactional
	@Retryable(
		retryFor = {
			DataAccessResourceFailureException.class,
			CannotGetJdbcConnectionException.class
		},
		maxAttempts = 3,
		backoff = @Backoff(delay = 200)
	)
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

	// Spring AOP가 프록시 기반으로 동작하기 때문에 public으로 처리
	@Recover
	public ConcludeInfo.ConcludeResult recoverForConclusion(
		Exception e,
		Long expiredAuctionId
	) {
		// 추후 낙찰 처리 실패에 대한 알림 추가
		log.error("낙찰 처리를 최종 실패했습니다. auctionId: {}", expiredAuctionId, e);
		return null;
	}
}
