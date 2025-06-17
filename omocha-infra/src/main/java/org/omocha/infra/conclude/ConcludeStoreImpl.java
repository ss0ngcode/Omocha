package org.omocha.infra.conclude;

import java.time.LocalDateTime;

import org.omocha.domain.auction.Auction;
import org.omocha.domain.bid.Bid;
import org.omocha.domain.conclude.Conclude;
import org.omocha.domain.conclude.ConcludeStore;
import org.omocha.infra.conclude.repository.ConcludeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConcludeStoreImpl implements ConcludeStore {

	private final ConcludeRepository concludeRepository;
	private final JdbcTemplate jdbcTemplate;

	@Override
	public Conclude store(Auction auction, Bid highestBid) {

		Conclude conclude = Conclude.builder()
			.concludePrice(highestBid.getBidPrice())
			.concludedAt(highestBid.getCreatedAt())
			.auction(auction)
			.buyer(highestBid.getBuyer())
			.build();

		return concludeRepository.save(conclude);
	}

	@Override
	public void concludeAuctionWithBid(Auction auction, Bid highestBid) {
		// 1. 경매 낙찰 테이블 INSERT
		String concludeSql = "INSERT INTO conclude (auction_id, buyer_member_id, conclude_price, concluded_at) VALUES (?, ?, ?, ?)";
		jdbcTemplate.update(
			concludeSql,
			auction.getAuctionId(),
			highestBid.getBuyer().getMemberId(),
			highestBid.getBidPrice().getValue(),
			highestBid.getCreatedAt()
		);

		// 2. 경매 상태를 'CONCLUDED'로 변경
		String auctionUpdateSql = "UPDATE auction SET auction_status = ? WHERE auction_id = ?";
		jdbcTemplate.update(
			auctionUpdateSql,
			Auction.AuctionStatus.CONCLUDED.getDescription(),
			auction.getAuctionId()
		);

		// 3. 채팅방 생성
		String chatRoomSql = "INSERT INTO chatroom (room_name, buyer_member_id, seller_member_id, auction_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
		jdbcTemplate.update(
			chatRoomSql,
			auction.getTitle(),
			highestBid.getBuyer().getMemberId(),
			auction.getMemberId(),
			auction.getAuctionId(),
			LocalDateTime.now(),
			LocalDateTime.now()
		);
	}

	@Override
	public void concludeAuctionWithNoBids(Auction auction) {
		// 경매 상태를 'NO_BIDS'로 변경
		String auctionUpdateSql = "UPDATE auction SET auction_status = ? WHERE auction_id = ?";
		jdbcTemplate.update(
			auctionUpdateSql,
			Auction.AuctionStatus.NO_BIDS.getDescription(),
			auction.getAuctionId()
		);
	}
}
