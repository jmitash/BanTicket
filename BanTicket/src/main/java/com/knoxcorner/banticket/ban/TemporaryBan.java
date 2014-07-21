package com.knoxcorner.banticket.ban;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.HistoryEvent.BanType;
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
	 * @param ipBan true for IP ban, otherwise false
	 * @param endTime ban's expire time in standard ms time
	 */
	public TemporaryBan(UUID playerUUID, String reason, String info, UUID bannerUUID, boolean ipBan, long endTime)
	{
		super(playerUUID, reason, info, bannerUUID, ipBan, BanType.TEMPBAN);
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
	public byte setOnServerBanList(boolean banned, List<String> ipsorname)
	{
		if(!banned) //unban
		{
			if(this.isIpBan())
			{
				//TODO: Existing ban check
				
				//Waiting on UUID support
				/*Iterator<BanEntry> bei = BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).getBanEntries().iterator();
				while(bei.hasNext())
				{
					BanEntry be = bei.next();
					if(be.getUuid().equals(this.getUUID())
				}*/
				for(int i = 0; i < ipsorname.size(); i++)
				{
					BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).pardon(ipsorname.get(i));
				}
				return 0;
			}
			else
			{
				for(int i = 0; i < ipsorname.size(); i++)
				{
					BanTicket.banTicket.getServer().getBanList(BanList.Type.NAME).pardon(ipsorname.get(0));
				}
				return 0;
			}
		}
		
		if(BanTicket.banTicket.getServer().getBannedPlayers().contains(getOfflinePlayer()))
		{
			return 2; //Already banned
		}
		
		if(this.getOfflinePlayer().hasPlayedBefore())
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
			
			if(this.isIpBan())
			{
				for(int i = 0; i < ipsorname.size(); i++)
				{
					BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).addBan(
							ipsorname.get(i),
							this.getReason(),
							new Date(endTime),
							banSource);
				}
				return 0;
			}
			else
			{
				if(BanTicket.banTicket.getConfigManager().getSaveToMinecraft())
				{
					BanTicket.banTicket.getServer().getBanList(BanList.Type.NAME).addBan(
						getOfflinePlayer().getName(),
						this.getReason(),
						new Date(endTime),
						banSource);
				}
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
