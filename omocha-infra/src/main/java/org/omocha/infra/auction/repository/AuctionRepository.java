package org.omocha.infra.auction.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.omocha.domain.auction.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuctionRepository extends JpaRepository<Auction, Long>, AuctionRepositoryCustom {
	@Query("SELECT a.auctionId FROM Auction a WHERE a.auctionStatus = :status AND a.endDate BETWEEN :startDate AND :endDate")
	List<Long> findAuctionIdsByStatusAndEndDateBetween(
		Auction.AuctionStatus status,
		LocalDateTime startDate,
		LocalDateTime endDate
	);
}
