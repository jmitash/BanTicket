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

import com.knoxcorner.banticket.ban.IpBan;
import com.knoxcorner.banticket.ban.PermanentBan;
import com.knoxcorner.banticket.ban.PermanentBanRequest;
import com.knoxcorner.banticket.ban.TemporaryBan;
import com.knoxcorner.banticket.ban.TemporaryBanRequest;
import com.knoxcorner.banticket.io.ConfigManager;
import com.knoxcorner.banticket.io.IpBanManager;
import com.knoxcorner.banticket.io.PlayerSaveManager;
import com.knoxcorner.banticket.listener.BTPlayer;
import com.knoxcorner.banticket.listener.PlayerListener;
import com.knoxcorner.banticket.util.Util;

public class BanTicket extends JavaPlugin
{
	public static BanTicket banTicket;
	
	private ConfigManager cm;
	private PlayerSaveManager playerSaveManager;
	private PlayerListener listener;
	private IpBanManager ipBanManager;
	
	private volatile List<BTPlayer> players;
	private volatile List<BTPlayer> bannedPlayersCache;

	@Override
	public void onEnable()
	{
		//TODO: Check enable while running
		
		banTicket = this;
		cm = new ConfigManager(this);
		playerSaveManager = new PlayerSaveManager(this);
		this.ipBanManager = new IpBanManager(this);
		cm.loadConfig();
		listener = new PlayerListener(this);
		players = new ArrayList<BTPlayer>();
		bannedPlayersCache = new ArrayList<BTPlayer>();
		getIpBanManager().load();
		
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
				page = parseInt(args[0], s) - 1;
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
				reason = compoundString(args, 1);
			}
			
			if(isIp(args[0]))
			{
				for(BTPlayer player : players)
				{
					if(player.getLastIp().equals(args[0]))
					{
						PermanentBan pb = new PermanentBan(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								true);
						boolean success = handleBanError(s, player.addBan(pb), player);
						player.save();
						if(success)
						{
							s.sendMessage(ChatColor.GREEN + "Banning IP(s): " + player.getCommonIps().toString());
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now IP banned");
						}
						return true;
					}
				}
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
						boolean success = handleBanError(s, player.addBan(pb), player);
						player.save();
						if(success)
						{
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
						boolean success = handleBanError(s, btpl.addBan(pb), btpl);
						btpl.save();
						if(success)
						{
							s.sendMessage(ChatColor.GREEN + btpl.getMostRecentName() + " (offline) is now banned.");
						}
						return true;
					}
					else //DNE on our side
					{
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
				reason = compoundString(args, 2);
			}
			
			if(isIp(args[0]))
			{
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
						boolean success = handleBanError(s, player.addBan(tb), player);
						player.save();
						if(success)
						{
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
						boolean success = handleBanError(s, player.addBan(tb), player);
						player.save();
						if(success)
						{
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
						boolean success = handleBanError(s, btpl.addBan(tb), btpl);
						btpl.save();
						if(success)
						{
							s.sendMessage(ChatColor.GREEN + btpl.getMostRecentName() + " (offline) is now banned.");
						}
						return true;
					}
					else //DNE on our side
					{
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
				reason = compoundString(args, 1);
			}
			
			if(isIp(args[0]))
			{
				for(BTPlayer player : players)
				{
					if(player.getLastIp().equals(args[0]))
					{
						PermanentBanRequest pbr = new PermanentBanRequest(player.getUUID(),
								reason,
								null,
								s instanceof Player ? ((Player) s).getUniqueId() : null,
								true);
						boolean success = handleBanError(s, player.addBan(pbr), player);
						player.save();
						if(success)
						{
							s.sendMessage(ChatColor.GREEN + "Banning IP(s): " + player.getCommonIps().toString());
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now IP banned.");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
				} //Not online
				IpBan ipban = new IpBan(Util.newList(args[0]), reason, null, -1,
						null, sPlayer == null ? null : sPlayer.getUniqueId(), true,
						this.getConfigManager().getExpireTime() + System.currentTimeMillis(),
						this.getConfigManager().getApproveOnExpire());
				this.getIpBanManager().addBan(ipban);
				s.sendMessage(ChatColor.GREEN + args[0] + " is now IP banned.");
				s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
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
						boolean success = handleBanError(s, player.addBan(pbr), player);
						player.save();
						if(success)
						{
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
						boolean success = handleBanError(s, btpl.addBan(pbr), btpl);
						btpl.save();
						if(success)
						{
							s.sendMessage(ChatColor.GREEN + btpl.getMostRecentName() + " (offline) is now banned.");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
					else //DNE on our side
					{
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
				reason = compoundString(args, 2);
			}
			
			if(isIp(args[0]))
			{
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
						boolean success = handleBanError(s, player.addBan(tbr), player);
						player.save();
						if(success)
						{
							s.sendMessage(ChatColor.GREEN + "Banning IP(s): " + player.getCommonIps().toString());
							s.sendMessage(ChatColor.GREEN + player.getMostRecentName() + " is now IP banned for " + args[1] + ".");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
				} //Not online
				IpBan ipban = new IpBan(Util.newList(args[0]), reason, null, System.currentTimeMillis() + length,
						null, sPlayer == null ? null : sPlayer.getUniqueId(), true,
						this.getConfigManager().getExpireTime() + System.currentTimeMillis(),
						this.getConfigManager().getApproveOnExpire());
				this.getIpBanManager().addBan(ipban);
				s.sendMessage(ChatColor.GREEN + args[0] + " is now IP banned for " + args[1] + ".");
				s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
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
						boolean success = handleBanError(s, player.addBan(tbr), player);
						player.save();
						if(success)
						{
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
						boolean success = handleBanError(s, btpl.addBan(tbr), btpl);
						btpl.save();
						if(success)
						{
							s.sendMessage(ChatColor.GREEN + btpl.getMostRecentName() + " (offline) was banned.");
							s.sendMessage(ChatColor.GREEN + "This ban will expire in " + Util.msToTime(this.getConfigManager().getExpireTime()) + " without approval.");
						}
						return true;
					}
					else //DNE on our side
					{
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
		
		return true;
	}
	
	private static int parseInt(String is, CommandSender s)
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
	
	private static String compoundString(String[] args, int offset)
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
	
	private static boolean isIp(String s)
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
	
	private static boolean handleBanError(CommandSender cs, byte err, BTPlayer player)
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
	
	
	
	
	public IpBanManager getIpBanManager()
	{
		return ipBanManager;
	}






	private final static String[][] HELP = 
		{
			{
				ChatColor.GREEN + "/bt <page #>" + ChatColor.DARK_GREEN + ": Open help page",
				ChatColor.GREEN + "/btb <player/IP> <reason>" + ChatColor.DARK_GREEN + ": Permaban player/IP",
				ChatColor.GREEN + "/bttb <player/IP> <time> <reason>" + ChatColor.DARK_GREEN + ": Tempban for (0d0h0m0s)",
				ChatColor.GREEN + "/btbr <player/IP> <reason>" + ChatColor.DARK_GREEN + ": Request for player/IP",
				ChatColor.GREEN + "/bttbr <player/IP> <time> <reason>" + ChatColor.DARK_GREEN + ": Request tempban for (0d0h0m0s)",
				ChatColor.GREEN + "/btw <player> <warning>" + ChatColor.DARK_GREEN + ": Warn player"
			},
			{
				ChatColor.GREEN + "/btinfo <Info>" + ChatColor.DARK_GREEN + ": Attach info to previous warn/ban"
			}
			
		};
	
}
