package com.knoxcorner.banticket.ban;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.HistoryEvent.BanType;
import com.knoxcorner.banticket.util.Util;

import java.util.List;
import java.util.UUID;

import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;

public class PermanentBan extends Ban
{

	/**
	 * Permanent ban constructor
	 * @param playerUUID player-to-be-banned's UUID
	 * @param reason reason for ban
	 * @param info supplementary info about this player's ban
	 * @param bannerUUID UUID of player who entered ban command, or null for console
	 * @param ipBan true for IP ban, otherwise false
	 */
	public PermanentBan(UUID playerUUID, String reason, String info, UUID bannerUUID, boolean ipBan)
	{
		super(playerUUID, reason, info, bannerUUID, ipBan, BanType.PERMBAN);
	}

	@Override
	public boolean isPermanent()
	{
		return true;
	}


	/**
	 * Will add a player to the server's own ban list
	 * @param banned true if player should be banned, false if they should be unbanned
	 * @return 0 - Success<br>1 - Success, but player hasn't logged in before<br>2 - Ban already exists/Not banned
	 */
	@Override
	public byte setOnServerBanList(boolean banned, List<String> ipsorname) //Bad setup, need to fix at some point
	{
		if(!banned) //unban
		{
			if(this.isIpBan())
			{
				//TODO: Existing ban check
				
				for(int i = 0; i < ipsorname.size(); i++)
				{	
					if(BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).isBanned(ipsorname.get(i)))
						BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).pardon(ipsorname.get(i));
				}

				
				
				IpBan ipban = new IpBan(ipsorname, this.getReason(), this.getInfo(), -1,
						this.getUUID(), this.getBannerUUID(), false,
						BanTicket.banTicket.getConfigManager().getExpireTime() + System.currentTimeMillis(),
						BanTicket.banTicket.getConfigManager().getApproveOnExpire());
				BanTicket.banTicket.getIpBanManager().removeBan(ipban);
				
				return 0;
			}
			else
			{
				for(int i = 0; i < ipsorname.size(); i++)
				{	
					if(BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).isBanned(ipsorname.get(i)))
						BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).pardon(ipsorname.get(i));
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
				if(BanTicket.banTicket.getConfigManager().getSaveToMinecraft())
				{
					for(int i = 0; i < ipsorname.size(); i++)
					{
						BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).addBan(
								ipsorname.get(i),
								this.getReason(),
								null,
								banSource);
					}
				}
				IpBan ipban = new IpBan(ipsorname, this.getReason(), this.getInfo(), -1,
						this.getUUID(), this.getBannerUUID(), false,
						BanTicket.banTicket.getConfigManager().getExpireTime() + System.currentTimeMillis(),
						BanTicket.banTicket.getConfigManager().getApproveOnExpire());
				return BanTicket.banTicket.getIpBanManager().addBan(ipban);
			}
			else
			{
				if(BanTicket.banTicket.getConfigManager().getSaveToMinecraft())
				{
					BanTicket.banTicket.getServer().getBanList(BanList.Type.NAME).addBan(
						getOfflinePlayer().getName(),
						this.getReason(),
						null,
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

	@Override
	public boolean isOver()
	{
		return false;
	}

	@Override
	public String getBanMessage()
	{
		return "Permanently Banned for:\n"
		+ this.getReason()
		+ "\nExtra info:\n"
		+ this.getInfo();
	}

}
