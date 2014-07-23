package com.knoxcorner.banticket.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.Ban;
import com.knoxcorner.banticket.ban.HistoryEvent;
import com.knoxcorner.banticket.ban.HistoryEvent.BanType;
import com.knoxcorner.banticket.ban.IpBan;

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
		/*int index = -1;
		if((index = pl.bannedPlayersCache.indexOf(ple.getUniqueId())) >= 0) //Cache banned so spam-joining doesn't cause lag
		{
			btpl = pl.bannedPlayersCache.get(index);
			Ban ban;
			if((ban = btpl.getBans().getActiveBan()) != null)
			{
				ple.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ban.getBanMessage());
				pl.getLogger().info(ple.getName() + " tried to join, but is CACHE banned by BanTicket.");
				return;
			}
			else
			{
				pl.bannedPlayersCache.remove(ple.getUniqueId());
			}
		}*/
				
		if(!pl.getPlayerSaveManager().playerExists(ple.getUniqueId()))
		{
			btpl = new BTPlayer(ple.getUniqueId(), ple.getAddress().getHostAddress(), ple.getName());
		}
		else
		{
			btpl = pl.getPlayerSaveManager().loadPlayer(ple.getUniqueId());
			btpl.addIP(ple.getAddress().getHostAddress());
		}
		
		Ban ban = btpl.getBans().getActiveBan();
		btpl.getBans().update(btpl.getCommonIps());
		if(ban != null)
		{
			pl.getLogger().info(ple.getName() + " tried to join, but is banned by BanTicket.");
			//pl.bannedPlayersCache.add(btpl);
			ple.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ban.getBanMessage());
			if(pl.getConfigManager().getLogLogins())
			{
				HistoryEvent he = new HistoryEvent(BanType.INFO, "Player connected");
				he.setExtraInfo("Player is currently banned.");
				btpl.addHistory(he);
			}
		}
		IpBan ipban = null;
		try
		{
			ipban = pl.getIpBanManager().getBan(ple.getAddress().getHostAddress());
		} catch (Exception e)
		{
			//Not banned (anymore)
		}
		if(ipban != null)
		{
			//TODO: option to ban accounts from IP if IP banned
			ple.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ipban.getBanMessage());
		}
		
		
		if(pl.getConfigManager().getLogLogins())
		{
			HistoryEvent he = new HistoryEvent(BanType.INFO, "Player connected");
			btpl.addHistory(he);
		}
		btpl.save();
		
		pl.addPlayer(btpl);
	}
	
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent pqe)
	{

		BTPlayer btpl;
		if((btpl = pl.removePlayer(pqe.getPlayer().getUniqueId())) != null)
		{
			pl.getLogger().info(btpl.getMostRecentName());
			if(pl.getConfigManager().getLogDisconnect())
			{
				HistoryEvent he = new HistoryEvent(BanType.INFO, "Player disconnected");
				btpl.addHistory(he);
				btpl.save();
			}
		}
		else
		{
			pl.getLogger().warning(pqe.getPlayer().getName() + " wasn't in BT's player list.");
		}

	}
} 
