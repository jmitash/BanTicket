package com.knoxcorner.banticket.ban;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bukkit.BanList;
import org.bukkit.OfflinePlayer;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.HistoryEvent.BanType;
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
	public PermanentBanRequest(UUID playerUUID, String reason, String info, UUID bannerUUID, boolean ipBan)
	{
		super(playerUUID, reason, info, bannerUUID, ipBan);
		this.startTime = System.currentTimeMillis();
		this.expireTime = BanTicket.banTicket.getConfigManager().getExpireTime();
		this.approveExpire = BanTicket.banTicket.getConfigManager().getApproveOnExpire();
		this.setBanType(BanType.PERMBANREQ);
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
	public PermanentBanRequest(UUID playerUUID, String reason, String info, UUID bannerUUID, boolean ipBan, long startTime, long expireTime, boolean aoe)
	{
		super(playerUUID, reason, info, bannerUUID, ipBan);
		this.startTime = startTime;
		this.expireTime = expireTime;
		this.approveExpire = aoe;
		this.setBanType(BanType.PERMBANREQ);
	}

	public boolean isExpired()
	{
		return this.startTime + this.expireTime < System.currentTimeMillis();
	}
	
	public PermanentBan expire(List<String> ips)
	{
		this.setOnServerBanList(false, ips);
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
	
	/**
	 * Will add a player to the server's own ban list, ban depends on expire time and approve on expire
	 * @param banned true if player should be banned, false if they should be unbanned
	 * @return 0 - Success<br>1 - Success, but player hasn't logged in before<br>2 - Ban already exists/Not banned
	 */
	@Override
	public byte setOnServerBanList(boolean banned, List<String> ipsorname)
	{
		if(!banned) //unban
		{
			if(this.isIpBan())
			{
				//TODO: Existing ban check
				
				IpBan ipban = new IpBan(ipsorname, this.getReason(), this.getInfo(), -1,
						this.getUUID(), this.getBannerUUID(), true,
						this.expireTime + System.currentTimeMillis(),
						this.approveExpire);
				BanTicket.banTicket.getIpBanManager().removeBan(ipban);
				
				for(int i = 0; i < ipsorname.size(); i++)
				{
					if(BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).isBanned(ipsorname.get(i)))
						BanTicket.banTicket.getServer().getBanList(BanList.Type.IP).pardon(ipsorname.get(i));
				}
				return 0;
			}
			else
			{
				for(int i = 0; i < ipsorname.size(); i++)
				{
					if(BanTicket.banTicket.getServer().getBanList(BanList.Type.NAME).isBanned(ipsorname.get(i)))
						BanTicket.banTicket.getServer().getBanList(BanList.Type.NAME).pardon(ipsorname.get(i));
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
								new Date(this.expireTime + this.startTime),
								banSource);
					}
				}
				
				IpBan ipban = new IpBan(ipsorname, this.getReason(), this.getInfo(), -1,
						this.getUUID(), this.getBannerUUID(), true,
						this.expireTime + System.currentTimeMillis(),
						this.approveExpire);
				BanTicket.banTicket.getIpBanManager().addBan(ipban);
				
				return 0;
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
	public String getBanMessage()
	{
		return "You have been banned by a low-ranking staff member until a\n"
				+ "higher ranked staff member can approve/deny this ban.\n"
				+ "Ban will auto-" + (this.approveExpire ? "approved " : "denied ")
				+ "after " + Util.msToTime(startTime + expireTime - System.currentTimeMillis())
				+ ".\n\n" + super.getBanMessage();
	}
}
