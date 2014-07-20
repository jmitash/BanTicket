package com.knoxcorner.banticket.ban;

import java.util.UUID;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.util.Util;

public class PermanentBanRequest extends PermanentBan implements Expirable
{
	private long startTime;
	private long expireTime;
	private boolean approveExpire;

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
		this.expireTime = BanTicket.banTicket.getConfigManager().getExpireTime();
		this.approveExpire = BanTicket.banTicket.getConfigManager().getApproveOnExpire();
	}
	
	/**
	 * Constructor for bans from file
	 * @param playerUUID player-to-be-banned's UUID
	 * @param reason reason for ban
	 * @param info supplementary info about this player's ban
	 * @param bannerUUID UUID of player who entered ban command, or null for console
	 * @param banIp true for IP ban, otherwise false
	 * @param startTime the time this ban started in ms
	 * @param expireTime time till expires
	 * @param aoe approve on expire
	 */
	public PermanentBanRequest(UUID playerUUID, String reason, String info, UUID bannerUUID, boolean ipban, long startTime, long expireTime, boolean aoe)
	{
		super(playerUUID, reason, info, bannerUUID, ipban);
		this.startTime = startTime;
		this.expireTime = expireTime;
		this.approveExpire = aoe;
	}

	public boolean isExpired()
	{
		return this.startTime + this.expireTime < System.currentTimeMillis();
	}
	
	public PermanentBan expire()
	{
		this.setOnServerBanList(false);
		if(this.approveExpire)
			return new PermanentBan(this.getUUID(), this.getReason(), Util.getDate() + " Auto Renewal; " + this.getInfo(), this.getBannerUUID(), this.isIpBan());
		else
			return null;
	}

	public long getStartTime()
	{
		return this.startTime;
	}

	public long getExpireTime()
	{
		return this.expireTime;
	}

	public boolean getApproveOnExpire()
	{
		return this.approveExpire;
	}

}
