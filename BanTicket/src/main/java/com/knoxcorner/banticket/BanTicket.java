package com.knoxcorner.banticket;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bukkit.BanList;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import com.knoxcorner.banticket.ban.Ban;
import com.knoxcorner.banticket.ban.Expirable;
import com.knoxcorner.banticket.ban.HistoryEvent;
import com.knoxcorner.banticket.ban.HistoryEvent.BanType;
import com.knoxcorner.banticket.ban.IpBan;
import com.knoxcorner.banticket.ban.PermanentBan;
import com.knoxcorner.banticket.ban.PermanentBanRequest;
import com.knoxcorner.banticket.ban.TemporaryBan;
import com.knoxcorner.banticket.ban.TemporaryBanRequest;
import com.knoxcorner.banticket.io.ConfigManager;
import com.knoxcorner.banticket.io.IpBanManager;
import com.knoxcorner.banticket.io.PlayerSaveManager;
import com.knoxcorner.banticket.io.RequestBanManager;
import com.knoxcorner.banticket.listener.PlayerListener;
import com.knoxcorner.banticket.user.BTConsole;
import com.knoxcorner.banticket.user.BTPlayer;
import com.knoxcorner.banticket.util.Util;

public class BanTicket extends JavaPlugin
{
	public static BanTicket banTicket;
	
	private ConfigManager cm;
	private PlayerSaveManager playerSaveManager;
	private PlayerListener listener;
	private IpBanManager ipBanManager;
	private RequestBanManager reqBanMgr;
	private BTConsole console;
	
	private volatile List<BTPlayer> players;
	private volatile List<BTPlayer> bannedPlayersCache;

	@Override
	public void onEnable()
	{
		//TODO: Check enable while running
		
		banTicket = this;
		console = new BTConsole();
		cm = new ConfigManager(this);
		playerSaveManager = new PlayerSaveManager(this);
		this.ipBanManager = new IpBanManager(this);
		cm.loadConfig();
		listener = new PlayerListener(this);
		players = new ArrayList<BTPlayer>();
		bannedPlayersCache = new ArrayList<BTPlayer>();
		ipBanManager.load();
		reqBanMgr = new RequestBanManager(this);
		reqBanMgr.load();
		
		for(Player pl : this.getServer().getOnlinePlayers())
		{
			BTPlayer btpl = playerSaveManager.loadPlayer(pl.getUniqueId());
			if(btpl == null)
			{
				btpl = new BTPlayer(pl.getUniqueId(), pl.getAddress().getAddress().getHostAddress(), pl.getName());
			}
			else
			{
				btpl.addIP(pl.getAddress().getAddress().getHostAddress());
			}
			btpl.save();
			
			btpl.getBans().update(btpl.getCommonIps());
			if(btpl.getBans().getActiveBan() != null)
			{
				pl.kickPlayer(btpl.getBans().getActiveBan().getBanMessage());
				continue;
			}
			
			IpBan ipban = null;
			
			try
			{
				ipban = this.ipBanManager.getBan(pl.getAddress().getAddress().getHostAddress());
			} catch (Exception e) {
				//No longer banned
			}
			
			if(ipban != null)
			{
				pl.kickPlayer(ipban.getBanMessage());
				continue;
			}
			
			this.players.add(btpl);
		}
	}
	
	@Override
	public void onDisable()
	{
		cm.saveConfig();
		playerSaveManager = null;
		HandlerList.unregisterAll(this);
		players.clear();
		players = null;
		bannedPlayersCache.clear();
		bannedPlayersCache = null;
		reqBanMgr.save();
		reqBanMgr = null;
		console = null;
	}
	
	public PlayerSaveManager getPlayerSaveManager()
	{
		return playerSaveManager;
	}
	
	public ConfigManager getConfigManager()
	{
		return this.cm;
	}
	
	public synchronized void addPlayer(BTPlayer player)
	{
		this.players.add(player);
	}
	
	public synchronized BTPlayer removePlayer(UUID uuid)
	{
		for(int i = 0; i < players.size(); i++)
		{
			if(players.get(i).getUUID().equals(uuid))
			{
				return this.players.remove(i);
			}
		}
		return null;
	}
	
	public synchronized BTPlayer findPlayer(String name)
	{
		for(BTPlayer player : this.players)
		{
			if(player.getMostRecentName().equalsIgnoreCase(name))
				return player;
		}
		return null;
	}

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args)
	{
		Player sPlayer = null;
		if(s instanceof Player)
			sPlayer = (Player) s;
		
		/////////////\\\\\\\\\\\
		////////////BT\\\\\\\\\\\
		
		if(cmd.getName().equalsIgnoreCase("bt"))
		{
			int page = 0;
			if(args.length > 0)
			{
				page = Util.parseInt(args[0], s) - 1;
				if(page == -1)
				{
					//More command checking
					return true;
				}
				else if(page >= HELP.length)
				{
					s.sendMessage(ChatColor.RED + "That page doesn't exist! Pages 1-" + HELP.length + ".");
					return true;
				}
			}
			
			for(int i = 0; i < HELP[page].length; i++)
			{
				s.sendMessage(HELP[page][i]);
			}
			
			return true;
			
		}
		
		///////////// \\\\\\\\\\\
		////////////BTB\\\\\\\\\\\
		
		if(cmd.getName().equalsIgnoreCase("btb"))
		{
			if(args.length == 0)
			{
				s.sendMessage(HELP[0][1]);
				return true;
			}
			
			String reason = "No reason given.";
			if(args.length > 1)
			{
				reason = Util.compoundString(args, 1);
			}
			
			if(Util.isIp(args[0]))
			{
				if(!s.hasPermission("banticket.btb.ip"))
				{
					s.sendMessage(ChatColor.RED + "You don't have permission to permaban IPs!");
					return true;
				}
				for(BTPlayer player : players)
				{
					if(player.getLastIp().equals(args[0]))
					{
						PermanentBan pb = new PermanentBan(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								true);
						boolean success = Util.handleBanError(s, player.addBan(pb), player);
						player.save();
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has IP permabanned " + player.getCommonIps().toString() + " for: " + reason,
									this.getConfigManager().broadcastBans());
							s.sendMessage(ChatColor.GREEN + "Banning IP(s): " + player.getCommonIps().toString());
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now IP banned");
						}
						return true;
					}
				}
				notify("banticket.notify.ban", 
						ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has permabanned " + args[0] + " for: " + reason,
						this.getConfigManager().broadcastBans());
				IpBan ipban = new IpBan(Util.newList(args[0]), reason, null, -1, null, sPlayer == null ? null : sPlayer.getUniqueId(), false, -1, false);
				this.getIpBanManager().addBan(ipban);
				s.sendMessage(ChatColor.GREEN + args[0] + " is now IP banned");
				return true;
			}
			else //Player name
			{
				BTPlayer player;
				if((player = this.findPlayer(args[0])) != null)
				{
					if(player.getMostRecentName().equalsIgnoreCase(args[0]))
					{
						PermanentBan pb = new PermanentBan(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								false);
						boolean success = Util.handleBanError(s, player.addBan(pb), player);
						player.save();
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has permabanned " + args[0] + " for: " + reason,
									this.getConfigManager().broadcastBans());
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now banned.");
						}
						return true;
					}
				}
				OfflinePlayer[] posPlay = Util.findPossibleOfflinePlayers(args[0]);
				if(posPlay.length == 0)
				{
					s.sendMessage(ChatColor.GREEN + args[0] + " hasn't played here before; Would you like to ban his UUID? (Not finished)");
					return true;
				}
				else if(posPlay.length > 1)
				{
					s.sendMessage(ChatColor.GREEN + "Found " + posPlay.length + " possible players for that name. (Not finished)");
					return true;
				}
				else //1 option
				{
					BTPlayer btpl = this.playerSaveManager.loadPlayer(posPlay[0].getUniqueId());
					if(btpl != null)
					{
						PermanentBan pb = new PermanentBan(btpl.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								false);
						boolean success = Util.handleBanError(s, btpl.addBan(pb), btpl);
						btpl.save();
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has permabanned " + args[0] + " for: " + reason,
									this.getConfigManager().broadcastBans());
							s.sendMessage(ChatColor.GREEN + btpl.getMostRecentName() + " (offline) is now banned.");
						}
						return true;
					}
					else //DNE on our side
					{
						notify("banticket.notify.ban", 
								ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has permabanned " + args[0] + " for: " + reason,
								this.getConfigManager().broadcastBans());
						s.sendMessage(ChatColor.GREEN + args[0] + " has no history on BanTicket; banning on Bukkit only");
						this.getServer().getBanList(BanList.Type.NAME).addBan(args[0], reason, null, s.getName());
						if(posPlay[0].getPlayer() != null)
						{
							posPlay[0].getPlayer().kickPlayer(reason);
						}
					}
				}
			}
			
		}

		/////////////  \\\\\\\\\\\
		////////////BTTB\\\\\\\\\\\
		
		if(cmd.getName().equalsIgnoreCase("bttb"))
		{
			if(args.length == 0 || args.length == 1)
			{
				s.sendMessage(HELP[0][2]);
				return true;
			}
			
			long length = Util.msFromTime(args[1]);
			
			if(length == -1)
			{
				s.sendMessage(ChatColor.RED + args[1] + " is not a valid time. Use format 0d0h0m0s");
				return true;
			}
			
			String reason = "No reason given.";
			if(args.length > 2)
			{
				reason = Util.compoundString(args, 2);
			}
			
			if(Util.isIp(args[0]))
			{
				if(!s.hasPermission("banticket.bttb.ip"))
				{
					s.sendMessage(ChatColor.RED + "You don't have permission to permaban IPs!");
					return true;
				}
				for(BTPlayer player : players)
				{
					if(player.getLastIp().equals(args[0]))
					{
						TemporaryBan tb = new TemporaryBan(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								true,
								length + System.currentTimeMillis());
						boolean success = Util.handleBanError(s, player.addBan(tb), player);
						player.save();
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has IP tempbanned " + player.getCommonIps().toString() + " for " + args[1] + ": " + reason,
									this.getConfigManager().broadcastBans());
							s.sendMessage(ChatColor.GREEN + "Banning IP(s): " + player.getCommonIps().toString());
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now IP banned for " + args[1] + ".");
						}
						return true;
					}
				} //Not online
				IpBan ipban = new IpBan(Util.newList(args[0]), reason, null, System.currentTimeMillis() + length,
						null, sPlayer == null ? null : sPlayer.getUniqueId(), false, -1, false);
				this.getIpBanManager().addBan(ipban);
				s.sendMessage(ChatColor.GREEN + args[0] + " is now IP banned for " + args[1] + ".");
				notify("banticket.notify.ban", 
						ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has tempbanned " + ipban.getIps().toString() + " for " + args[1] + ": " + reason,
						this.getConfigManager().broadcastBans());
				return true;
			}
			else //Player name
			{
				BTPlayer player;
				if((player = this.findPlayer(args[0])) != null)
				{
					if(player.getMostRecentName().equalsIgnoreCase(args[0]))
					{
						TemporaryBan tb = new TemporaryBan(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								false,
								length + System.currentTimeMillis());
						boolean success = Util.handleBanError(s, player.addBan(tb), player);
						player.save();
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has tempbanned " + args[0] + " for " + args[1] + ": " + reason,
									this.getConfigManager().broadcastBans());
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now banned for " + args[1] + ".");
						}
						return true;
					}
				}
				OfflinePlayer[] posPlay = Util.findPossibleOfflinePlayers(args[0]);
				if(posPlay.length == 0)
				{
					s.sendMessage(ChatColor.GREEN + args[0] + " hasn't played here before; Would you like to ban his/her UUID? (Not finished)");
					return true;
				}
				else if(posPlay.length > 1)
				{
					s.sendMessage(ChatColor.GREEN + "Found " + posPlay.length + " possible players for that name. (Not finished)");
					return true;
				}
				else //1 option
				{
					BTPlayer btpl = this.playerSaveManager.loadPlayer(posPlay[0].getUniqueId());
					if(btpl != null)
					{
						TemporaryBan tb = new TemporaryBan(btpl.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								true,
								length + System.currentTimeMillis());
						boolean success = Util.handleBanError(s, btpl.addBan(tb), btpl);
						btpl.save();
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has tempbanned " + args[0] + " for " + args[1] + ": " + reason,
									this.getConfigManager().broadcastBans());
							s.sendMessage(ChatColor.GREEN + btpl.getMostRecentName() + " (offline) is now banned.");
						}
						return true;
					}
					else //DNE on our side
					{
						notify("banticket.notify.ban", 
								ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has tempbanned " + args[0] + " for " + args[1] + ": " + reason,
								this.getConfigManager().broadcastBans());
						s.sendMessage(ChatColor.GREEN + args[0] + " has no history on BanTicket; banning on Bukkit only");
						this.getServer().getBanList(BanList.Type.NAME).addBan(args[0], reason, null, s.getName());
						if(posPlay[0].getPlayer() != null)
						{
							posPlay[0].getPlayer().kickPlayer(reason);
						}
					}
				}
			}
		}
		
		/////////////  \\\\\\\\\\\
		////////////BTBR\\\\\\\\\\\
		
		if(cmd.getName().equalsIgnoreCase("btbr"))
		{
			if(args.length == 0)
			{
				s.sendMessage(HELP[0][3]);
				return true;
			}
			
			String reason = "No reason given.";
			if(args.length > 1)
			{
				reason = Util.compoundString(args, 1);
			}
			
			if(Util.isIp(args[0]))
			{
				/*for(BTPlayer player : players)
				{
					if(player.getLastIp().equals(args[0]))
					{
						PermanentBanRequest pbr = new PermanentBanRequest(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								true);
						boolean success = Util.handleBanError(s, player.addBan(pbr), player);
						player.save();
						
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has requested a permaban on " + args[0] + " for: " + reason,
									this.getConfigManager().broadcastBans());
							reqBanMgr.addBan(pbr);
							s.sendMessage(ChatColor.GREEN + "Banning IP(s): " + player.getCommonIps().toString());
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now IP banned.");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
				} //Not online
				/*
				IpBan ipban = new IpBan(Util.newList(args[0]), reason, null, -1,
						null, sPlayer == null ? null : sPlayer.getUniqueId(), true,
						this.getConfigManager().getExpireTime() + System.currentTimeMillis(),
						this.getConfigManager().getApproveOnExpire());
				this.getIpBanManager().addBan(ipban);
				s.sendMessage(ChatColor.GREEN + args[0] + " is now IP banned.");
				s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
				return true;*/
				s.sendMessage(ChatColor.RED + "Cannot ban IPs only in ban requests yet.");
				return true;
			}
			else //Player name
			{
				BTPlayer player;
				if((player = this.findPlayer(args[0])) != null)
				{
					if(player.getMostRecentName().equalsIgnoreCase(args[0]))
					{
						PermanentBanRequest pbr = new PermanentBanRequest(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								false);
						boolean success = Util.handleBanError(s, player.addBan(pbr), player);
						player.save();
						
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has requested a permaban on " + args[0] + " for: " + reason,
									this.getConfigManager().broadcastBans());
							reqBanMgr.addBan(pbr);
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now banned.");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
				}
				OfflinePlayer[] posPlay = Util.findPossibleOfflinePlayers(args[0]);
				if(posPlay.length == 0)
				{
					s.sendMessage(ChatColor.GREEN + args[0] + " hasn't played here before; Would you like to ban his/her UUID? (Not finished)");
					return true;
				}
				else if(posPlay.length > 1)
				{
					s.sendMessage(ChatColor.GREEN + "Found " + posPlay.length + " possible players for that name. (Not finished)");
					return true;
				}
				else //1 option
				{
					BTPlayer btpl = this.playerSaveManager.loadPlayer(posPlay[0].getUniqueId());
					if(btpl != null)
					{
						PermanentBanRequest pbr = new PermanentBanRequest(btpl.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								false);
						
						boolean success = Util.handleBanError(s, btpl.addBan(pbr), btpl);
						btpl.save();
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has requested a permaban on " + args[0] + " for: " + reason,
									this.getConfigManager().broadcastBans());
							reqBanMgr.addBan(pbr);
							s.sendMessage(ChatColor.GREEN + btpl.getMostRecentName() + " (offline) is now banned.");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
					else //DNE on our side
					{
						notify("banticket.notify.ban", 
								ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has requested a permaban on " + args[0] + " for: " + reason,
								this.getConfigManager().broadcastBans());
						s.sendMessage(ChatColor.GREEN + args[0] + " has no history on BanTicket; banning on Bukkit only");
						s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						this.getServer().getBanList(BanList.Type.NAME).addBan(args[0], reason, null, s.getName());
						if(posPlay[0].getPlayer() != null)
						{
							posPlay[0].getPlayer().kickPlayer(reason);
						}
					}
				}
			}
		}
		
		
		/////////////   \\\\\\\\\\\
		////////////BTTBR\\\\\\\\\\\
		
		if(cmd.getName().equalsIgnoreCase("bttbr"))
		{
			if(args.length == 0 || args.length == 1)
			{
				s.sendMessage(HELP[0][4]);
				return true;
			}
			
			long length = Util.msFromTime(args[1]);
			
			if(length == -1)
			{
				s.sendMessage(ChatColor.RED + args[1] + " is not a valid time. Use format 0d0h0m0s");
				return true;
			}
			
			String reason = "No reason given.";
			if(args.length > 2)
			{
				reason = Util.compoundString(args, 2);
			}
			
			if(Util.isIp(args[0]))
			{
				/*
				for(BTPlayer player : players)
				{
					if(player.getLastIp().equals(args[0]))
					{
						TemporaryBanRequest tbr = new TemporaryBanRequest(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								true,
								System.currentTimeMillis() + length);
						boolean success = Util.handleBanError(s, player.addBan(tbr), player);
						player.save();
						
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has requested a tempban on " + args[0] + " for " + args[1] + " with reason: " + reason,
									this.getConfigManager().broadcastBans());
							reqBanMgr.addBan(tbr);
							s.sendMessage(ChatColor.GREEN + "Banning IP(s): " + player.getCommonIps().toString());
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now IP banned for " + args[1] + ".");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
				} //Not online
				/*IpBan ipban = new IpBan(Util.newList(args[0]), reason, null, System.currentTimeMillis() + length,
						null, sPlayer == null ? null : sPlayer.getUniqueId(), true,
						this.getConfigManager().getExpireTime() + System.currentTimeMillis(),
						this.getConfigManager().getApproveOnExpire());
				this.getIpBanManager().addBan(ipban);
				s.sendMessage(ChatColor.GREEN + args[0] + " is now IP banned for " + args[1] + ".");
				s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
				return true;*/
				s.sendMessage(ChatColor.RED + "Cannot ban IPs only in ban requests yet.");
				return true;
			}
			else //Player name
			{
				BTPlayer player;
				if((player = this.findPlayer(args[0])) != null)
				{
					if(player.getMostRecentName().equalsIgnoreCase(args[0]))
					{
						TemporaryBanRequest tbr = new TemporaryBanRequest(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								false,
								System.currentTimeMillis() + length);
						boolean success = Util.handleBanError(s, player.addBan(tbr), player);
						player.save();
						
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has requested a tempban on " + args[0] + " for " + args[1] + " with reason: " + reason,
									this.getConfigManager().broadcastBans());
							reqBanMgr.addBan(tbr);
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now banned for " + args[1] + ".");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
				}
				OfflinePlayer[] posPlay = Util.findPossibleOfflinePlayers(args[0]);
				if(posPlay.length == 0)
				{
					s.sendMessage(ChatColor.GREEN + args[0] + " hasn't played here before; Would you like to ban his UUID? (Not finished)");
					return true;
				}
				else if(posPlay.length > 1)
				{
					s.sendMessage(ChatColor.GREEN + "Found " + posPlay.length + " possible players for that name. (Not finished)");
					return true;
				}
				else //1 option
				{
					BTPlayer btpl = this.playerSaveManager.loadPlayer(posPlay[0].getUniqueId());
					if(btpl != null)
					{
						TemporaryBanRequest tbr = new TemporaryBanRequest(btpl.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								false,
								System.currentTimeMillis() + length);
						boolean success = Util.handleBanError(s, btpl.addBan(tbr), btpl);
						btpl.save();
						if(success)
						{
							notify("banticket.notify.ban", 
									ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has requested a tempban on " + args[0] + " for " + args[1] + " with reason: " + reason,
									this.getConfigManager().broadcastBans());
							reqBanMgr.addBan(tbr);
							s.sendMessage(ChatColor.GREEN + btpl.getMostRecentName() + " (offline) was banned.");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
					else //DNE on our side
					{
						notify("banticket.notify.ban", 
								ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has requested a tempban on " + args[0] + " for " + args[1] + " with reason: " + reason,
								this.getConfigManager().broadcastBans());
						s.sendMessage(ChatColor.GREEN + args[0] + " has no history on BanTicket; banning on Bukkit only");
						s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						this.getServer().getBanList(BanList.Type.NAME).addBan(args[0], reason, null, s.getName());
						if(posPlay[0].getPlayer() != null)
						{
							posPlay[0].getPlayer().kickPlayer(reason);
						}
					}
				}
			}
		}
			
		/////////////   \\\\\\\\\\\
		//////////// BTP \\\\\\\\\\\
		
		if(cmd.getName().equalsIgnoreCase("btp"))
		{
			if(args.length == 0)
			{
				s.sendMessage(HELP[1][1]);
				return true;
			}
			
			if(Util.isIp(args[0]))
			{
				IpBan ipban = null;
				try
				{
					ipban = this.getIpBanManager().getBan(args[0]);
				} catch (Exception e)
				{
					if(e.getMessage().equals("Ban expired"))
					{
						s.sendMessage(ChatColor.RED + "That ban has already expired!");
						return true;
					}
					if(e.getMessage().equals("Ban over"))
					{
						s.sendMessage(ChatColor.RED + "That ban has already ended!");
						return true;
					}
					s.sendMessage(ChatColor.RED + "Unexpected exception");
					return true;
				}
				if(ipban == null)
				{
					s.sendMessage(ChatColor.RED + "No banned IPs found for " + args[0]);
					return true;
				}
				
				this.getIpBanManager().removeBan(ipban);
				s.sendMessage(ChatColor.GREEN + "IPs unbanned: " + ipban.getIps().toString());
				notify("banticket.notify.ban", 
						ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has pardoned IPs: " + ipban.getIps().toString(),
						false);
				
				if(ipban.getUUID() != null)
				{
					BTPlayer banned = this.getPlayerSaveManager().loadPlayer(ipban.getUUID());
					if(banned == null)
					{
						s.sendMessage(ChatColor.GREEN + "A UUID was attached to that ban, but the save file is missing.");
						return true;
					}
					Ban ban = banned.getBans().getActiveBan();
					if(ban == null)
					{
						s.sendMessage(ChatColor.GREEN + "Tried to unban " + banned.getMostRecentName() + " but his/her save is not banned.");
						return true;
					}
					ban.setOnServerBanList(false, banned.getCommonIps());
					banned.getBans().remove(ban);
					HistoryEvent he = new HistoryEvent(BanType.INFO, "Unbanned");
					if(sPlayer == null)
						he.setExtraInfo("By CONSOLE");
					else
						he.setExtraInfo(sPlayer.getName() + " | " + sPlayer.getUniqueId().toString());
					banned.addHistory(he);
					banned.save();
					s.sendMessage(ChatColor.GREEN + banned.getMostRecentName() + " is now unbanned.");
					notify("banticket.notify.ban", 
							ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has pardoned " + banned.getMostRecentName(),
							false);
				}
				
				return true;
			}
			else
			{
				OfflinePlayer[] posPlayer = Util.findPossibleOfflinePlayers(args[0]);
				if(posPlayer.length == 0)
				{
					s.sendMessage(ChatColor.RED + "No players found with name " + args[0]);
					return true;
				}
				if(posPlayer.length > 1)
				{
					s.sendMessage(ChatColor.RED + "Found more than one player with name " + args[0]);
					return true;
				}
				OfflinePlayer op = posPlayer[0];
				BTPlayer player = this.getPlayerSaveManager().loadPlayer(op.getUniqueId());
				if(player == null)
				{
					notify("banticket.notify.ban", 
							ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has pardoned " + args[0],
							false);
					s.sendMessage(ChatColor.RED + "Player is missing save file. Ban will still be removed from server.");
					this.getServer().getBanList(BanList.Type.NAME).pardon(args[0]);
					return true;
				}
				else
				{
					player.getBans().update(player.getCommonIps());
					Ban ban = player.getBans().getActiveBan();
					if(ban != null)
					{
						player.getBans().remove(ban);
						ban.setOnServerBanList(false, player.getCommonIps());
						player.save();
						if(ban instanceof Expirable)
							reqBanMgr.removeBan((Expirable) ban);
						notify("banticket.notify.ban", 
								ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has pardoned " + args[0],
								false);
						s.sendMessage(ChatColor.GREEN + args[0] + " is now unbanned!");
						return true;
					}
					else
					{
						s.sendMessage(ChatColor.RED + args[0] + " doesn't appear to be banned!");
						return true;
					}
				}
			}
		}
		
		
		/////////////   \\\\\\\\\\\
		//////////// BTR \\\\\\\\\\\
		if(cmd.getName().equalsIgnoreCase("btr"))
		{
			if(args.length == 0)
			{
				if(sPlayer != null)
				{
					for(BTPlayer btpl : this.players)
					{
						if(btpl.getUUID().equals(sPlayer.getUniqueId()))
						{
							btpl.sendFormattedReviews(s);
							return true;
						}
					}
					s.sendMessage(ChatColor.RED + "You are not the console, but not in the player list either!");
					return true;
				}
				else
				{
					this.console.sendFormattedReviews(s);
					return true;
				}
			}
			
			if(args.length < 2)
			{
				s.sendMessage(HELP[1][2]);
				return true;
			}
			
			int num = -1;
			try
			{
				num = Integer.parseInt(args[0]);
			}
			catch (NumberFormatException nfe)
			{
				s.sendMessage(ChatColor.RED + args[0] + " is not a valid number");
				return true;
			}
			
			if(!args[1].startsWith("a") && !args[1].startsWith("d"))
			{
				s.sendMessage(HELP[1][2]);
				return true;
			}
			
			boolean accept = args[1].startsWith("a");
			
			if(sPlayer != null)
			{
				for(BTPlayer btpl : this.players)
				{
					if(btpl.getUUID().equals(sPlayer.getUniqueId()))
					{
						btpl.handleBanReview(num, accept, s);
						return true;
					}
				}
				s.sendMessage(ChatColor.RED + "You are not the console, but not in the player list either!");
				return true;
			}
			else
			{
				this.console.handleBanReview(num, accept, s);
				return true;
			}
			
		}
		
		////////  \\\\\\\
		////// BTW \\\\\\
		if (cmd.getName().equalsIgnoreCase("btw"))
		{
			BTPlayer btpl;
			if (args.length == 0)
			{
				s.sendMessage(HELP[0][5]);
				return true;
			}
			if (args.length == 1)
			{
				s.sendMessage(ChatColor.RED + "You must provide a warning message!");
				return true;
			}
			btpl = findPlayer(args[0]);
			if (btpl == null)
			{
				s.sendMessage(ChatColor.RED + args[0] + " must be online to receive a warning.");
				return true;
			}
			String reason = Util.compoundString(args, 1);

			HistoryEvent he = new HistoryEvent(HistoryEvent.BanType.WARN, reason);
			he.setExtraInfo("Warned by " + (sPlayer == null ? "CONSOLE" : new StringBuilder(String.valueOf(sPlayer.getName())).append(" : ").append(sPlayer.getUniqueId().toString()).toString()));
			btpl.addHistory(he);
			btpl.save();

			Player pl = Util.findOnlinePlayer(args[0]);
			if (pl == null)
			{
				s.sendMessage(ChatColor.RED + args[0] + " was in BanTicket's player list, but not the server's!");
				s.sendMessage(ChatColor.RED + "The warning will still be saved on their profile, but they won't receive the message if they're online");
				return true;
			}
			notify("banticket.notify.warn", 
					ChatColor.DARK_GREEN + (sPlayer == null ? "CONSOLE" : sPlayer.getName()) + " has warned " + pl.getName() + ": " + reason,
					this.getConfigManager().broadcastWarnings());
		}
		
		
		////////  \\\\\\\
		////// BTV \\\\\\
		if(cmd.getName().equalsIgnoreCase("btv"))
		{
			BTPlayer btpl;
			if (args.length == 0)
			{
				s.sendMessage(HELP[1][3]);
				return true;
			}
			if (args.length >= 1)
			{
				btpl = findPlayer(args[0]);
				if(btpl == null)
				{
					OfflinePlayer[] posPlay = Util.findPossibleOfflinePlayers(args[0]);
					if(posPlay.length == 0)
					{
						s.sendMessage(ChatColor.RED + "No players found with name " + args[0]);
						return true;
					}
					else if(posPlay.length == 1)
					{
						btpl = this.getPlayerSaveManager().loadPlayer(posPlay[0].getUniqueId());
						if(btpl == null)
						{
							s.sendMessage(ChatColor.RED + "That player doesn't have a BanTicket file");
							return true;
						}
					}
					else
					{
						s.sendMessage(ChatColor.RED + "There's more than one player with that name (unfinished)");
						return true;
					}
				}
				
				for(HistoryEvent he : btpl.getHistory())
				{
					s.sendMessage(ChatColor.DARK_GREEN + he.getEventType().toString() + " " + Util.getDate(he.getCalendar().getTimeInMillis()));
					s.sendMessage(ChatColor.GREEN + "Event: " + he.getEvent());
					s.sendMessage(ChatColor.GREEN + "Info: " + he.getExtraInfo());
				}
				return true;
				
			}
			
			return true;
		}
		
		////////  \\\\\\\
		////// BTRW \\\\\\
		if(cmd.getName().equalsIgnoreCase("btwr"))
		{
			if(args.length == 0)
			{
				s.sendMessage(HELP[1][4]);
				return true;
			}
			
			BTPlayer btpl = Util.findBTPlayer(args[0]);
			if(btpl == null)
			{
				s.sendMessage(ChatColor.RED + "Could not find player " + args[0]);
				return true;
			}
			
			for(int i = btpl.getHistory().size() - 1; i >= 0; i--)
			{
				if(btpl.getHistory().get(i).getEventType() == BanType.WARN)
				{
					HistoryEvent he = btpl.getHistory().remove(i);
					s.sendMessage(ChatColor.GREEN + "Removed warning: " + he.getEvent());
					return true;
				}
			}
			
			s.sendMessage(ChatColor.RED + "No warnings found for " + args[0]);
			
			return true;
			
		}
		return true;
	}

		
	
	public IpBanManager getIpBanManager()
	{
		return ipBanManager;
	}
	
	public RequestBanManager getRequestBanManager()
	{
		return this.reqBanMgr;
	}
	
	public void notify(String perm, String msg, boolean dontUsePerm)
	{
		for(Player pl : this.getServer().getOnlinePlayers())
		{
			if(dontUsePerm || pl.hasPermission(perm))
			{
				pl.sendMessage(msg);
			}
		}
	}
	
	public synchronized List<BTPlayer> getBTPlayers()
	{
		return this.players;
	}

	private final static String[][] HELP = 
		{
			{
				ChatColor.GREEN + "/bt <page #>" + ChatColor.DARK_GREEN + ": Open help page",
				ChatColor.GREEN + "/btb <player/IP> <reason>" + ChatColor.DARK_GREEN + ": Permaban player/IP",
				ChatColor.GREEN + "/bttb <player/IP> <time> <reason>" + ChatColor.DARK_GREEN + ": Tempban for (0d0h0m0s)",
				ChatColor.GREEN + "/btbr <player/IP> <reason>" + ChatColor.DARK_GREEN + ": Request permaban for player/IP",
				ChatColor.GREEN + "/bttbr <player/IP> <time> <reason>" + ChatColor.DARK_GREEN + ": Request tempban for (0d0h0m0s)",
				ChatColor.GREEN + "/btw <player> <warning>" + ChatColor.DARK_GREEN + ": Warn player"
			},
			{
				ChatColor.GREEN + "/bti <Info>" + ChatColor.DARK_GREEN + ": Attach info to previous warn/ban",
				ChatColor.GREEN + "/btp <player/IP>" + ChatColor.DARK_GREEN + ": Unban given player/IP",
				ChatColor.GREEN + "/btr <num> <a/d>" + ChatColor.DARK_GREEN + ": Review ban requests",
				ChatColor.GREEN + "/btv <player>" + ChatColor.DARK_GREEN + ": View player history",
				ChatColor.GREEN + "/btwr <player>" + ChatColor.DARK_GREEN + ": Remove last warning"
			}
			
		};
	
}
