package com.knoxcorner.banticket.ban;

import java.util.Date;
import java.util.UUID;

import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.util.Util;

public class TemporaryBanRequest extends TemporaryBan implements Expirable
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
	 * @param startTime the time this ban started in ms
	 * @param expireTime time till expires
	 * @param aoe approve on expire
	 */
	public TemporaryBanRequest(UUID playerUUID, String reason, String info,UUID bannerUUID, boolean banIp, long endTime)
	{
		super(playerUUID, reason, info, bannerUUID, banIp, endTime);
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
	public TemporaryBanRequest(UUID playerUUID, String reason, String info,UUID bannerUUID, boolean banIp, long endTime, long startTime, long expireTime, boolean aoe)
	{
		super(playerUUID, reason, info, bannerUUID, banIp, endTime);
		this.startTime = startTime;
		this.expireTime = expireTime;
		this.approveExpire = aoe;
	}
	
	public boolean isExpired()
	{
		return this.startTime + this.expireTime < System.currentTimeMillis();
	}
	
	public TemporaryBan expire()
	{
		this.setOnServerBanList(false);
		if(this.approveExpire && !super.isOver())
			return new TemporaryBan(this.getUUID(), this.getReason(), Util.getDate() + " Auto Renewal; " + this.getInfo(), this.getBannerUUID(), this.isIpBan(), this.getEndTime());
		else
			return null;
	}
	
	@Override
	public boolean isOver()
	{
		return super.isOver() || (this.isExpired() && !this.approveExpire);
	}
	
	@Override
	public byte setOnServerBanList(boolean banned)
	{
		if(BanTicket.banTicket.getServer().getBannedPlayers().contains(getOfflinePlayer()))
		{
			return 2; //Already banned
		}
		
		if(BanTicket.banTicket.getServer().getOfflinePlayer(getUUID()).hasPlayedBefore())
		{
			String banSource = null;
			if(getBannerUUID() != null)
			{
				banSource = getBannerUUID().toString();
				OfflinePlayer banner = BanTicket.banTicket.getServer().getOfflinePlayer(getBannerUUID());
				if(banner.hasPlayedBefore()) //Check to make sure banner's files weren't removed
				{
					banSource += ':';
					banSource += banner.getName();
				}
			}
			else
			{
				banSource = "CONSOLE";
			}
			
			if(!this.isIpBan())
			{
				Date date;
				if(!this.approveExpire //If it will die sooner from expire
						&& this.startTime + this.expireTime < this.getEndTime())
				{
					date = new Date(this.startTime + this.getExpireTime());
				}
				else
				{
					date = new Date(this.getEndTime());
				}
				BanTicket.banTicket.getServer()
				.getBanList(BanList.Type.NAME).addBan(
						getOfflinePlayer().getName(),
						this.getReason(),
						date,
						banSource);
				return 0; //No issue
			}
			else
			{
				//TODO: Add IP tracking
				/*
				BanTicket.banTicket.getServer()
				.getBanList(BanList.Type.NAME).addBan(
						getOfflinePlayer().,
						this.getReason(),
						endTime,
						banSource);
				*/
				return 0;
			}
		}
		else
		{
			return 1;
		}
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
