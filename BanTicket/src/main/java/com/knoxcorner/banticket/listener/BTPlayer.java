package com.knoxcorner.banticket.listener;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.Ban;
import com.knoxcorner.banticket.ban.HistoryEvent;
import com.knoxcorner.banticket.ban.HistoryEvent.BanType;
import com.knoxcorner.banticket.util.BanList;
import com.knoxcorner.banticket.util.Util;



/**
 * Holds extra information surrounding online players
 * @author Jacob
 *
 */
public class BTPlayer
{

	private UUID uuid;
	private LinkedHashMap<String, Integer> ipMap;
	private LinkedHashMap<Long, String> prevNamesMap;
	private BanList bans;
	private LinkedList<HistoryEvent> history;
	
	/**
	 * Constructor for file load
	 * @param uuid the player's UUID
	 * @param ipMap HashMap of IPs and login counts from them
	 * @param prevNames Map of previous usernames and dates logged in with
	 */
	public BTPlayer(UUID uuid, LinkedHashMap<String, Integer> ipMap, LinkedHashMap<Long, String> prevNames, BanList bans, LinkedList<HistoryEvent> history)
	{
		this.uuid = uuid;
		this.ipMap = ipMap;
		this.prevNamesMap = prevNames;
		this.bans = bans;
		this.history = history;
		
		bans.update(this.getCommonIps());
		
		OfflinePlayer player = BanTicket.banTicket.getServer().getOfflinePlayer(uuid);
		if(player.hasPlayedBefore() && !player.isBanned() && BanTicket.banTicket.getConfigManager().getSaveToMinecraft()) //Somehow unbanned on server
		{
			for(Ban ban : bans)
			{
				if(!ban.isOver())
				{
					BanTicket.banTicket.getLogger().warning(player.getName() + " is banned by BanTicket, but not Bukkit; Banning");
					BanTicket.banTicket.getLogger().info("UUID: " + uuid.toString());
					ban.setOnServerBanList(true, ban.isIpBan() ? this.getCommonIps() : Util.newList(getMostRecentName()));
					break;
				}
			}
		}
	}
	
	/**
	 * Constructor for first-join player
	 * @param uuid Player's UUID
	 * @param ip Player's IP as string
	 * @param name Player's current username
	 */
	public BTPlayer(UUID uuid, String ip, String name)
	{
		this.uuid = uuid;
		this.ipMap = new LinkedHashMap<String, Integer>();
		this.prevNamesMap = new LinkedHashMap<Long, String>();
		this.bans = new BanList();
		this.history = new LinkedList<HistoryEvent>();
		this.history.add(new HistoryEvent(BanType.INFO, "First join"));
		this.addIP(ip);
		this.addName(name);
	}
	
	public void addIP(String IP)
	{
		if(this.ipMap.containsKey(IP))
		{
			int count = this.ipMap.remove(IP);
			this.ipMap.put(IP, count + 1);
		}
		else
			this.ipMap.put(IP, 1);
	}
	
	public void addName(String name)
	{
		Object[] vals = prevNamesMap.values().toArray();
		if(vals.length > 0) //Grab most recent - if name hasn't changed
		{
			if(!vals[vals.length - 1].equals(name))
				prevNamesMap.put(System.currentTimeMillis(), name);
		}
		else
		{
			prevNamesMap.put(System.currentTimeMillis(), name);
		}
			
			
	}
	
	/**
	 * Adds a ban to this player and activates it
	 * @param ban the ban to add
	 * @return status of ban <p>0 - Success<br>1 - Success, but no player file on server<br>2 - Ban already exists on server<br>3 - Ban already exists on plugin
	 */
	public byte addBan(Ban ban)
	{
		for(Ban prevBan : bans)
		{
			if(!prevBan.isOver())
				return 3; //Can't ban player again
		}
		
		this.bans.add(ban);
		this.addHistory(new HistoryEvent(ban));
		
		byte success = ban.setOnServerBanList(true, this.getCommonIps());
		Player player;
		if((player = BanTicket.banTicket.getServer().getPlayer(uuid)) != null)
		{
			player.kickPlayer(ban.getBanMessage());
		}
		return success;
	}
	
	public void addHistory(HistoryEvent histEvent)
	{
		this.history.add(histEvent);
	}

	public String getMostRecentName()
	{
		return (String) prevNamesMap.values().toArray()[prevNamesMap.size() - 1];
	}
	
	public LinkedList<HistoryEvent> getHistory()
	{
		return this.history;
	}
	
	public BanList getBans()
	{
		return this.bans;
	}
	
	public UUID getUUID()
	{
		return this.uuid;
	}
	
	public HashMap<String, Integer> getIpMap()
	{
		return this.ipMap;
	}
	
	public LinkedHashMap<Long, String> getNameMap()
	{
		return this.prevNamesMap;
	}
	
	public List<String> getCommonIps()
	{		
		return Util.getCommonIps(ipMap);
	}
	
	
	public String getLastIp()
	{
		if(this.ipMap.size() > 0)
			return (String) this.ipMap.keySet().toArray()[this.ipMap.size() - 1];
		else
			return null;
	}
	
	public void save()
	{
		BanTicket.banTicket.getPlayerSaveManager().savePlayer(this);
	}
	
	public boolean equals(Object UUIDorPlayer)
	{
		if(UUIDorPlayer instanceof UUID)
		{
			return ((UUID) UUIDorPlayer).equals(this.uuid);
		}
		else if(UUIDorPlayer instanceof BTPlayer)
		{
			return ((BTPlayer) UUIDorPlayer).uuid.equals(this.uuid);
		}
		return false;
	}
	
	
	
}
