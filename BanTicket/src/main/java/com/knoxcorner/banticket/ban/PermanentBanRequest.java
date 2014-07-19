package com.knoxcorner.banticket.ban;

import java.util.UUID;

import com.knoxcorner.banticket.BanTicket;

public class PermanentBanRequest extends PermanentBan
{
	private long startTime;

	/**
	 * Constructor for new bans only
	 * @param playerUUID player-to-be-banned's UUID
	 * @param reason reason for ban
	 * @param info supplementary info about this player's ban
	 * @param bannerUUID UUID of player who entered ban command, or null for console
	 * @param banIp true for IP ban, otherwise false
	 */
	public PermanentBanRequest(UUID playerUUID, String reason, String info, UUID bannerUUID, boolean ipban)
	{
		super(playerUUID, reason, info, bannerUUID, ipban);
		this.startTime = System.currentTimeMillis();
	}
	
	/**
	 * Constructor for new bans only
	 * @param playerUUID player-to-be-banned's UUID
	 * @param reason reason for ban
	 * @param info supplementary info about this player's ban
	 * @param bannerUUID UUID of player who entered ban command, or null for console
	 * @param banIp true for IP ban, otherwise false
	 * @param startTime the time this ban started in ms
	 */
	public PermanentBanRequest(UUID playerUUID, String reason, String info, UUID bannerUUID, boolean ipban, long startTime)
	{
		super(playerUUID, reason, info, bannerUUID, ipban);
		this.startTime = startTime;
	}
	
	@Override
	public boolean isPending()
	{
		return true;
	}
	
	@Override
	public boolean isExpired()
	{
		return this.startTime + BanTicket.banTicket.getConfigManager().getExpireTime() < System.currentTimeMillis();
	}
	
	@Override
	public PermanentBan expire()
	{
		if(BanTicket.banTicket.getConfigManager().getApproveOnExpire())
			return new PermanentBan(this.getUUID(), this.getReason(), this.getInfo(), this.getBannerUUID(), this.isIpBan());
		else
			return null;
	}

}
