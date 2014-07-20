package com.knoxcorner.banticket.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.Ban;

public class PlayerListener implements Listener
{
	private BanTicket pl;
	
	public PlayerListener(BanTicket banTicket)
	{
		this.pl = banTicket;
		pl.getServer().getPluginManager().registerEvents(this, pl);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent ple)
	{
		
		BTPlayer btpl;
		if(!pl.getPlayerSaveManager().playerExists(ple.getUniqueId()))
		{
			pl.getLogger().info(ple.getName() + " has joined for first time; build profile");
			btpl = new BTPlayer(ple.getUniqueId(), ple.getAddress().getHostAddress(), ple.getName());
		}
		else
		{
			pl.getLogger().info(ple.getName() + " has joined again; load profile");
			btpl = pl.getPlayerSaveManager().loadPlayer(ple.getUniqueId(), ple.getAddress().getHostAddress());
		}
		
		if(btpl.getBans().hasActiveBan())
		{
			for(Ban ban : btpl.getBans())
			{
				if(!ban.isOver())
				{
					pl.getLogger().info(ple.getName() + " tried to join, but is banned by BanTicket.");
					ple.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ban.getBanMessage());
					btpl.save();
					return;
				}
			}
			pl.getLogger().severe("Entered unenterable code while looking for ban");
			return;
		}
		
		btpl.save();
		
		pl.players.add(btpl);
	}
} 
