package com.knoxcorner.banticket.ban;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import com.knoxcorner.banticket.BanTicket;

/**
 * Abstract ban type to hold ban information
 * @author Jacob
 */
public abstract class Ban
{
	private String reason;
	private String info;
	private UUID playerUUID;
	private UUID bannerUUID;
	private boolean banIp;
	private BanType banType;
	private OfflinePlayer player;
	
	
	/**
	 * Default ban constructor
	 * @param playerUUID player-to-be-banned's UUID
	 * @param reason reason for ban
	 * @param info supplementary info about this player's ban
	 * @param bannerUUID UUID of player who entered ban command, or null for console
	 * @param banIp true for IP ban, otherwise false
	 */
	public Ban(UUID playerUUID, String reason, String info, UUID bannerUUID, boolean banIp, BanType type)
	{
		Player possiblePlayer = BanTicket.banTicket.getServer().getPlayer(playerUUID); //Check online players first
		if(possiblePlayer != null)
		{
			this.player = possiblePlayer;
		}
		else //Player isn't online, check offline
		{
			this.player = BanTicket.banTicket.getServer().getOfflinePlayer(playerUUID);
		}
		
		this.reason = reason;
		this.info = info;
		this.banIp = banIp;
		this.playerUUID = playerUUID;
		this.bannerUUID = bannerUUID;
		this.banType = type;
	}
	
	/**
	 * Get reason player was banned
	 * @return ban reason
	 */
	public String getReason()
	{
		return reason;
	}
	
	/**
	 * Get extraneous info on this player ban
	 * @return extra info regarding ban
	 */
	public String getInfo()
	{
		if(info != null)
		{
			return info;
		}
		return "No extended info available.";
	}
	
	/**
	 * Gets the banned UUID
	 * @return banned UUID
	 */
	public UUID getUUID()
	{
		return playerUUID;
	}
	
	/**
	 * Get the offline player that was banned
	 * @return banned player
	 */
	public OfflinePlayer getOfflinePlayer()
	{
		return player;
	}
	
	/**
	 * Get UUID of person who banned
	 * @return UUID of banner
	 */
	public UUID getBannerUUID()
	{
		return bannerUUID;
	}
	
	/**
	 * Get whether this is an IP ban
	 * @return true if this is an IP ban
	 */
	public boolean isIpBan()
	{
		return banIp;
	}
	
	public BanType getType()
	{
		return this.banType;
	}
	

	public abstract boolean isOver();
	
	public abstract boolean isPermanent();
	
	public abstract boolean isPending();
	
	
	/**
	 * Tells whether the ban has run out of approval time
	 * @return true if expired, false otherwise
	 */
	public boolean isExpired()
	{
		return false;
	}
	
	public Ban expire()
	{
		return null;
	}
	
	public abstract String getBanMessage();
	
	/**
	 * Will add a player to the server's own ban list
	 * @param banned true if player should be banned, false if they should be unbanned
	 * @return 0 - Success<br>1 - Success, but player hasn't logged in before<br>2 - Ban already exists/Not banned
	 */
	public abstract byte setOnServerBanList(boolean banned);
	
	
	
}
