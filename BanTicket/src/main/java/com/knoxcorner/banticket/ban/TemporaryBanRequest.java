package com.knoxcorner.banticket.ban;

import java.util.UUID;

import com.knoxcorner.banticket.BanTicket;

public class TemporaryBanRequest extends TemporaryBan
{
	
	private long startTime;

	public TemporaryBanRequest(UUID playerUUID, String reason, String info,UUID bannerUUID, boolean banIp, long endTime)
	{
		super(playerUUID, reason, info, bannerUUID, banIp, endTime);
		this.startTime = System.currentTimeMillis();
	}
	
	public TemporaryBanRequest(UUID playerUUID, String reason, String info,UUID bannerUUID, boolean banIp, long endTime, long startTime)
	{
		super(playerUUID, reason, info, bannerUUID, banIp, endTime);
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
	public TemporaryBan expire()
	{
		if(BanTicket.banTicket.getConfigManager().getApproveOnExpire() && !super.isOver())
			return new TemporaryBan(this.getUUID(), this.getReason(), this.getInfo(), this.getBannerUUID(), this.isIpBan(), this.getEndTime());
		else
			return null;
	}
	
	@Override
	public boolean isOver()
	{
		return super.isOver() || (this.isExpired() && !BanTicket.banTicket.getConfigManager().getApproveOnExpire());
	}

}
