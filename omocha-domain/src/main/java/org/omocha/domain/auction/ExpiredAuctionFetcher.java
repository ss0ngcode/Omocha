package org.omocha.domain.auction;

import java.util.List;

public interface ExpiredAuctionFetcher {

	List<Long> fetchExpiredAuctionIds();
}
