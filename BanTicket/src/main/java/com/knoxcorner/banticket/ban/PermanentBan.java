package com.knoxcorner.banticket.ban;

import com.knoxcorner.banticket.BanTicket;

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
	 * @param banIp true for IP ban, otherwise false
	 */
	public PermanentBan(UUID playerUUID, String reason, String info, UUID bannerUUID, List<String> ips)
	{
		super(playerUUID, reason, info, bannerUUID, ips, BanType.PERMBAN);
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
	public byte setOnServerBanList(boolean banned)
	{
		if(!banned)
		{
		
			return 0;
		}
		
		if(!banned && BanTicket.banTicket.getServer().getBannedPlayers().contains(getOfflinePlayer()))
		{
			return 2; //Already banned
		}
		
		if(BanTicket.banTicket.getServer().getOfflinePlayer(getUUID()).hasPlayedBefore() && !this.isIpBan())
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
				for(int i = 0; i < ips.size(); i++)
				{
					BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).addBan(
							ips.get(i),
							this.getReason(),
							null,
							banSource);
				}
				return 0;
			}
			else
			{
				BanTicket.banTicket.getServer().getBanList(BanList.Type.NAME).addBan(
						getOfflinePlayer().getName(),
						this.getReason(),
						null,
						banSource);
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
