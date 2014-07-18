package com.knoxcorner.banticket.util;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

import com.knoxcorner.banticket.ban.Ban;

@SuppressWarnings("serial")
public class BanList extends ArrayList<Ban>
{


	/**
	 * Searches for given UUID
	 * @param uuid identifier to search for
	 * @return true if found; otherwise false
	 */
	public boolean containsUUID(UUID uuid)
	{
		Ban ban;
		for(int i = 0; i < this.size(); i++)
		{
			ban = this.get(i);
			if(ban.getUUID().equals(uuid))
				return true;
		}
		return false;
	}
	
	/**
	 * Searches for player in list
	 * @param player player to search for
	 * @return true if found; otherwise false
	 */
	public boolean containsPlayer(OfflinePlayer player)
	{
		Ban ban;
		for(int i = 0; i < this.size(); i++)
		{
			ban = this.get(i);
			if(ban.getOfflinePlayer().equals(player))
				return true;
		}
		return false;
	}
	
	/**
	 * Retrieves the corresponding ban to the UUID
	 * @param uuid identifier to search for
	 * @return ban if found, otherwise null
	 */
	public Ban getByUUID(UUID uuid)
	{
		Ban ban;
		for(int i = 0; i < this.size(); i++)
		{
			ban = this.get(i);
			if(ban.getUUID().equals(uuid))
				return ban;
		}
		return null;
	}
	
	/**
	 * Retrieves the corresponding index to the UUID
	 * @param uuid identifier to search for
	 * @return ban if found, otherwise -1
	 */
	public int getIndexByUUID(UUID uuid)
	{
		for(int i = 0; i < this.size(); i++)
		{
			if(get(i).getUUID().equals(uuid))
				return i;
		}
		return -1;
	}
	
	/**
	 * Unbans the corresponding player with UUID and removes from list
	 * @param uuid identifier to search for
	 * @return -1 - could not find ban<br>0 - success<br>1 - success, but player hasn't logged in before<br>2 - player was not banned 
	 */
	public byte unbanUUID(UUID uuid)
	{
		Ban ban;
		for(int i = 0; i < this.size(); i++)
		{
			ban = this.get(i);
			if(ban.getUUID().equals(uuid))
			{
				return ban.setOnServerBanList(false);
			}
		}
		return -1; //not found
	}
}
