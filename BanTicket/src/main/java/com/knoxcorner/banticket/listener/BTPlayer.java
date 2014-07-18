package com.knoxcorner.banticket.listener;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.Ban;
import com.knoxcorner.banticket.ban.HistoryEvent;
import com.knoxcorner.banticket.ban.BanType;
import com.knoxcorner.banticket.util.BanList;



/**
 * Holds extra information surrounding online players
 * @author Jacob
 *
 */
public class BTPlayer
{

	private UUID uuid;
	private HashMap<String, Integer> ipMap;
	private LinkedHashMap<Long, String> prevNamesMap;
	private BanList bans;
	private LinkedList<HistoryEvent> history;
	
	/**
	 * Constructor for file load
	 * @param uuid the player's UUID
	 * @param ipMap HashMap of IPs and login counts from them
	 * @param prevNames Map of previous usernames and dates logged in with
	 */
	public BTPlayer(UUID uuid, HashMap<String, Integer> ipMap, LinkedHashMap<Long, String> prevNames, BanList bans, LinkedList<HistoryEvent> history)
	{
		this.uuid = uuid;
		this.ipMap = ipMap;
		this.prevNamesMap = prevNames;
		this.bans = bans;
		this.history = history;
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
		this.ipMap = new HashMap<String, Integer>();
		this.prevNamesMap = new LinkedHashMap<Long, String>();
		this.bans = new BanList();
		this.history = new LinkedList<HistoryEvent>();
		this.history.add(new HistoryEvent(BanType.INFO, "First join"));
	}
	
	public void addIP(String IP)
	{
		this.ipMap.put(IP, ipMap.get(IP) + 1);
	}
	
	public void addName(String name)
	{
		if(!prevNamesMap.values().toArray()[prevNamesMap.size() - 1].equals(name)) //Grab most recent - if name hasn't changed
			prevNamesMap.put(System.currentTimeMillis(), name);
	}
	
	public LinkedList<HistoryEvent> getHistory()
	{
		return this.history;
	}
	
	public BanList getBans()
	{
		return this.bans;
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
		
		byte success = ban.setOnServerBanList(true);
		Player player;
		if((player = BanTicket.banTicket.getServer().getPlayer(uuid)) != null)
		{
			player.kickPlayer(ban.getBanMessage());
		}
		return success;
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
