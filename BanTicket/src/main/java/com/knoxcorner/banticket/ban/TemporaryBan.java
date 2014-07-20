package com.knoxcorner.banticket.ban;

import java.util.Date;
import java.util.UUID;

import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.util.Util;

public class TemporaryBan extends Ban
{

	private long endTime;
	
	/**
	 * Temporary ban constructor
	 * @param playerUUID player-to-be-banned's UUID
	 * @param reason reason for ban
	 * @param info supplementary info about this player's ban
	 * @param bannerUUID UUID of player who entered ban command, or null for console
	 * @param banIp true for IP ban, otherwise false
	 * @param endTime ban's expire time in standard ms time
	 */
	public TemporaryBan(UUID playerUUID, String reason, String info, UUID bannerUUID, boolean banIp, long endTime)
	{
		super(playerUUID, reason, info, bannerUUID, banIp, BanType.TEMPBAN);
		this.endTime = endTime;
	}

	@Override
	public boolean isOver()
	{
		return System.currentTimeMillis() > endTime;
	}

	@Override
	public boolean isPermanent()
	{
		return false;
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
				BanTicket.banTicket.getServer()
				.getBanList(BanList.Type.NAME).addBan(
						getOfflinePlayer().getName(),
						this.getReason(),
						new Date(endTime),
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
	
	public long getEndTime()
	{
		return this.endTime;
	}


	@Override
	public String getBanMessage()
	{
		return Util.msToTime(endTime - System.currentTimeMillis()) + 
		" remaining; You have been banned for\n"
		+ this.getReason()
		+ "\nExtra info:\n"
		+ this.getInfo();
	}

}
