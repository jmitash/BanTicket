package com.knoxcorner.banticket.util;

import java.util.ArrayList;

import com.knoxcorner.banticket.BanTicket;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class Util
{
	private static BanTicket banTicket = BanTicket.banTicket;

	/**
	 * Finds a Player entity by name if he/she is online
	 * @param name of player to search for
	 * @return Player if found, otherwise null
	 */
	public static Player findOnlinePlayer(String name)
	{
		Player[] pls = banTicket.getServer().getOnlinePlayers();
		Player player = null;
		for(int i = 0; i < pls.length; i++)
		{
			if(pls[i].getName().equalsIgnoreCase(name))
			{
				player = pls[i];
				break;
			}
		}
		
		return player;
	}
	
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
			for(int i = offset; i < chars.length; i++)
			{
				String chr = Character.toString(chars[i]);
				if(i == 0 && "dhms".contains(chr)) //No empty labels
				{
					return -1;
				}
				
				if("0123456789".contains(chr)) //number
				{
					temp += chr;
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
	
}
