package com.knoxcorner.banticket.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.user.BTPlayer;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Util
{
	private static BanTicket banTicket = BanTicket.banTicket;

	
	/**
	 * Finds all previously-joined players via given name, and returns them in descending order of most recent join time
	 * @param name player to search for
	 * @return descending sorted players via most recent join time
	 */
	public static OfflinePlayer[] findPossibleOfflinePlayers(String name)
	{
		ArrayList<OfflinePlayer> matching = new ArrayList<OfflinePlayer>();
		OfflinePlayer[] offPlayers = banTicket.getServer().getOfflinePlayers();
		for(int i = 0; i < offPlayers.length; i++)
		{
			if(offPlayers[i].getName().equalsIgnoreCase(name))
				matching.add(offPlayers[i]);
		}
		OfflinePlayer[] retPlayers = new OfflinePlayer[matching.size()];
		retPlayers = matching.toArray(retPlayers);
		
		if(retPlayers.length < 2)
			return retPlayers;
		
		//Sort by last join date (bubble for simplicity)
		for(int i = retPlayers.length; i > 0; i--)
		{
			boolean moved = false;
			for(int j = 1; j < i; j++)
			{
				if(retPlayers[j - 1].getLastPlayed() < retPlayers[j].getLastPlayed())
				{
					moved = true;
					OfflinePlayer temp = retPlayers[j - 1];
					retPlayers[j - 1] = retPlayers[j];
					retPlayers[j] = temp;
				}
			}
			if(!moved)
				break;
		}
		return retPlayers;
	}
	
	/**
	 * Converts time in milliseconds to human-readable format 
	 * @param l time in milliseconds
	 * @return Time in 0d0h0m0s format
	 */
	public static String msToTime(long l)
	{
		long days = l / 1000 / 60 / 60 / 24;
		l -= days * 24 * 60 * 60 * 1000;
		long hours = l / 1000 / 60 / 60;
		l -= hours * 60 * 60 * 1000;
		long mins = l / 1000 / 60;
		l -= mins * 60 * 1000;
		long secs = l / 1000;
		return String.format("%dd%dh%dm%ds", days, hours, mins, secs);
	}
	
	public static long msFromTime(String time)
	{
		if(time == null || time.length() <= 1)
			return -1;
		
		//Make sure time doesn't have any illegal chars
		char[] chars = time.toLowerCase().toCharArray();
		for(int i = 0; i < chars.length; i++)
		{
			if(!"0123456789dhms".contains(Character.toString(chars[i])))
			{
				return -1;
			}
		}
		
		int days = 0, hours = 0, mins = 0, secs = 0;
		
		int offset = 0;
		String temp;
		while(offset < chars.length)
		{
			temp = "";
			for(int i = offset;; i++)
			{
				String chr = Character.toString(chars[i]);
				if(i == 0 && "dhms".contains(chr)) //No empty labels
				{
					return -1;
				}
				
				if("0123456789".contains(chr)) //number
				{
					temp += chr;
					if(i == chars.length - 1) //Unlabeled value - end of string
						return -1;
					continue;
				}
				
				if(chars[i] == 'd')
				{
					try
					{
						days += Integer.parseInt(temp);
						offset = i + 1;
						break;
					}
					catch (NumberFormatException nfe)
					{
						return -1;
					}
				}
				else if(chars[i] == 'h')
				{
					try
					{
						hours += Integer.parseInt(temp);
						offset = i + 1;
						break;
					}
					catch (NumberFormatException nfe)
					{
						return -1;
					}
				}
				else if(chars[i] == 'm')
				{
					try
					{
						mins += Integer.parseInt(temp);
						offset = i + 1;
						break;
					}
					catch (NumberFormatException nfe)
					{
						return -1;
					}
				}
				else if(chars[i] == 's')
				{
					try
					{
						secs += Integer.parseInt(temp);
						offset = i + 1;
						break;
					}
					catch (NumberFormatException nfe)
					{
						return -1;
					}
				}
				
				
			}
		}
		
		 
		return	(1000 * secs)
				+ (1000 * 60 * mins)
				+ (1000 * 60 * 60 * hours)
				+ (1000 * 60 * 60 * 24 * days);
		
	}
	
	public static String getDate()
	{
		return getDate(System.currentTimeMillis());
	}
	
	public static String getDate(long l)
	{
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(l));
	}

	public static List<String> getCommonIps(HashMap<String, Integer> ipMap)
	{
		int totalLogins = 0;
		for(int i : ipMap.values())
			totalLogins += i;
		
		boolean enoughLogins = totalLogins > BanTicket.banTicket.getConfigManager().getMinTotalLogins();
		
		int num = BanTicket.banTicket.getConfigManager().getNumberCommonIps();
		
		List<String> ips = new ArrayList<String>();
		
		List<Integer> vals = new ArrayList<Integer>(ipMap.values());
		Collections.sort(vals, Collections.reverseOrder());
		vals = vals.subList(0, num);
		
		for(Map.Entry<String, Integer> entry : ipMap.entrySet())
		{
			if(vals.contains(entry.getValue()))
			{
				if(entry.getValue() < BanTicket.banTicket.getConfigManager().getMinLoginsFromIp() && enoughLogins)
					continue;
				ips.add(entry.getKey());
				if(ips.size() == num)
					break;
			}
		}
		
		if(ipMap.size() > 0 && !ips.contains(ipMap.keySet().toArray()[ipMap.size() - 1])) //last IP
			ips.add((String) ipMap.keySet().toArray()[ipMap.size() - 1]);

		
		return ips;
	}
	
	public static List<String> newList(String str)
	{
		ArrayList<String> strs = new ArrayList<String>(1);
		strs.add(str);
		return strs;
	}

	public static String compoundString(String[] args, int offset)
	{
		String s = "";
		for(int i = offset; i < args.length; i++)
		{
			s += args[i];
			if(i < args.length - 1)
				s += " ";
		}
		return s;
	}

	public static boolean isIp(String s)
	{
		for(int i = 0; i < s.length() - 1; i++)
		{
			if(!"0123456789.".contains(s.substring(i, i+1)))
			{
				return false;
			}
		}
		return true;
	}

	public static boolean handleBanError(CommandSender cs, byte err, BTPlayer player)
	{
		switch(err)
		{
		case 0: return true;
		case 1: cs.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " doesn't have a Bukkit save file, but will be banned on join."); return true;
		case 2: cs.sendMessage(ChatColor.RED + player.getMostRecentName() + " is already banned by Bukkit."); return false;
		case 3: cs.sendMessage(ChatColor.RED + player.getMostRecentName() + " is already banned by BanTicket."); return false;
		}
		return false;
	}

	public static int parseInt(String is, CommandSender s)
	{
		int num = -1;
		for(int i = 0; i < is.length(); i++)
		{
			//if()
		}
		try
		{
			num = Integer.parseInt(is);
		} catch (NumberFormatException nfe)
		{
			s.sendMessage(ChatColor.RED +"\"" + is + "\" is not a valid number.");
			return -1;
		}
		return num;
	}
	
	public static Player findOnlinePlayer(String name)
	{
		for (Player pl : BanTicket.banTicket.getServer().getOnlinePlayers()) {
			if (pl.getName().equalsIgnoreCase(name)) {
				return pl;
			}
		}
		return null;
	}
	
	public static BTPlayer findBTPlayer(String name)
	{
		for(BTPlayer btpl : BanTicket.banTicket.getBTPlayers())
		{
			if(btpl.getMostRecentName().equalsIgnoreCase(name))
				return btpl;
		}
		
		OfflinePlayer[] posPlay = Util.findPossibleOfflinePlayers(name);
		
		if(posPlay.length == 1)
		{
			return BanTicket.banTicket.getPlayerSaveManager().loadPlayer(posPlay[0].getUniqueId());
		}
		//TODO: Handle multiple possible layers
		
		return null;
	}
	

}
