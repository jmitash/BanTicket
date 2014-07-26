package com.knoxcorner.banticket.user;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.Ban;
import com.knoxcorner.banticket.ban.Expirable;
import com.knoxcorner.banticket.ban.HistoryEvent;
import com.knoxcorner.banticket.ban.HistoryEvent.BanType;
import com.knoxcorner.banticket.ban.TemporaryBanRequest;
import com.knoxcorner.banticket.util.BanList;
import com.knoxcorner.banticket.util.Util;



/**
 * Holds extra information surrounding online players
 * @author Jacob
 *
 */
public class BTPlayer implements Reviewer
{

	private UUID uuid;
	private LinkedHashMap<String, Integer> ipMap;
	private LinkedHashMap<Long, String> prevNamesMap;
	private BanList bans;
	private LinkedList<HistoryEvent> history;
	private List<Expirable> banReviews;
	
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
	
	@Override
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
	
	public void sendFormattedReviews(CommandSender cs)
	{
		this.banReviews = new ArrayList<Expirable>(BanTicket.banTicket.getRequestBanManager().getBans()); //Grab a copy, so our numbers dont change
		if(this.banReviews.size() == 0)
		{
			cs.sendMessage(ChatColor.RED + "Nothing to review at the moment.");
		}
		for(int i = 0; i < this.banReviews.size(); i++)
		{
			Expirable e = this.banReviews.get(i);
			Ban b = (Ban) e;
			String msg = (i + 1) + ". ";
			if(b.isPermanent())
				msg += "permanent ";
			else
			{
				TemporaryBanRequest tbr = (TemporaryBanRequest) e;
				msg += (Util.msToTime(tbr.getEndTime() - tbr.getStartTime()) + " ");
			}
			
			String bannedName = null;
			for(OfflinePlayer op : BanTicket.banTicket.getServer().getOfflinePlayers()) //Find name in offline players
			{
				if(op.getUniqueId().equals(b.getUUID()))
				{
					bannedName = op.getName();
					break;
				}
			}
			
			if(bannedName == null) //Didnt find him, check our files
			{
				BTPlayer btpl = BanTicket.banTicket.getPlayerSaveManager().loadPlayer(b.getUUID());
				if(btpl != null)
					bannedName = btpl.getMostRecentName();
			}
			
			if(bannedName != null)
			{
				msg += (bannedName + " ");
			}
			else
			{
				msg += ("ERR ");
			}
			
			msg += b.getReason();
			
			cs.sendMessage(ChatColor.GREEN + msg);
		}
	}
	
	public void handleBanReview(int num, boolean accept, CommandSender cs)
	{
		if(this.banReviews == null)
		{
			cs.sendMessage(ChatColor.RED + "You must first enter /btr to see the list!");
			return;
		}
		
		if(num > this.banReviews.size())
		{
			cs.sendMessage(ChatColor.RED + "That ID number isn't on the list! Enter /btr to see the list.");
			return;
		}
		
		num--;
		
		Expirable e = this.banReviews.get(num);
		Ban b = (Ban) e;
		
		if(b.isOver())
		{
			cs.sendMessage(ChatColor.RED + "That ban has already ended. Enter /btr to refresh the list.");
			return;
		}
		
		if(e.isExpired())
		{
			cs.sendMessage(ChatColor.RED + "That ban has already expired. Enter /btr to refresh the list.");
			return;
		}
		
		if(!BanTicket.banTicket.getRequestBanManager().getBans().contains(e))
		{
			cs.sendMessage(ChatColor.RED + "That ban is no longer in the list. Enter /btr to refresh the list");
			return;
		}
		
		BTPlayer btpl = BanTicket.banTicket.getPlayerSaveManager().loadPlayer(b.getUUID());
		if(btpl == null)
		{
			cs.sendMessage(ChatColor.RED + "There was an issue loading the save file for the banned player.");
			return;
		}
		
		if(accept)
		{
			Ban pBan = btpl.getBans().getActiveBan();
			if(pBan == null)
			{
				cs.sendMessage(ChatColor.RED + "Player was missing ban request in save file. Please ban manually.");
				return;
			}
			Expirable pEBan = (Expirable) pBan;
			Ban renewBan = pEBan.accept();
			btpl.getBans().remove(pBan);
			btpl.getBans().add(renewBan);
			btpl.addHistory(new HistoryEvent(BanType.INFO, "Ban Request accepted by " + this.getMostRecentName()));
			btpl.save();
			renewBan.setOnServerBanList(true, btpl.getCommonIps());
			BanTicket.banTicket.getRequestBanManager().removeBan(e);
			cs.sendMessage(ChatColor.GREEN + "Ban approved. Use /btr to refresh the list");
		    BanTicket.banTicket.notify("banticket.notify.ban",
		    		ChatColor.DARK_GREEN + this.getMostRecentName() + " has approved " + btpl.getMostRecentName() + "'s ban",
		    		false);
		}
		else
		{
			Ban pBan = btpl.getBans().getActiveBan();
			if(pBan == null)
			{
				cs.sendMessage(ChatColor.RED + "Player was missing ban request in save file.");
				return;
			}
			btpl.getBans().remove(pBan);
			btpl.addHistory(new HistoryEvent(BanType.INFO, "Ban Request denied by " + this.getMostRecentName()));
			btpl.save();
			pBan.setOnServerBanList(false, btpl.getCommonIps());
			BanTicket.banTicket.getRequestBanManager().removeBan(e);
			
			cs.sendMessage(ChatColor.GREEN + "Ban denied. Use /btr to refresh the list");
		    BanTicket.banTicket.notify("banticket.notify.ban",
		    		ChatColor.DARK_GREEN + this.getMostRecentName() + " has denied " + btpl.getMostRecentName() + "'s ban",
		    		false);
		}
	}
	
	
	
	
	
}
