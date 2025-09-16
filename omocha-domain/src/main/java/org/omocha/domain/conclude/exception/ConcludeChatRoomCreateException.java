package org.omocha.domain.conclude.exception;

import org.omocha.domain.auction.exception.AuctionException;
import org.omocha.domain.common.code.ErrorCode;

public class ConcludeChatRoomCreateException extends AuctionException {
	public ConcludeChatRoomCreateException(Long auctionId, Long buyerId, Long sellerId) {
		super(
			ErrorCode.CONCLUDE_CHATROOM_CREATE_FAILURE,
			String.format("낙찰 후 채팅방 생성에 실패했습니다. auctionId: %d, buyerId: %d, sellerId: %d",
				auctionId, buyerId, sellerId)
		);
	}
}
