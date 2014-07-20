package com.knoxcorner.banticket;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.knoxcorner.banticket.ban.PermanentBan;
import com.knoxcorner.banticket.ban.TemporaryBanRequest;
import com.knoxcorner.banticket.io.ConfigManager;
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
	
	public volatile List<BTPlayer> players;

	@Override
	public void onEnable()
	{
		//TODO: Check enable while running
		
		banTicket = this;
		cm = new ConfigManager(this);
		playerSaveManager = new PlayerSaveManager(this);
		cm.loadConfig();
		listener = new PlayerListener(this);
		players = new ArrayList<BTPlayer>();
	}
	
	@Override
	public void onDisable()
	{
		cm.saveConfig();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(cmd.getName().equalsIgnoreCase("bt"))
		{
			players.get(0).addBan(new PermanentBan(players.get(0).getUUID(), "Because I said so.", "Nothing to see.", null, null));
			players.get(0).save();
		}
		return true;
	}
	
	
	public PlayerSaveManager getPlayerSaveManager()
	{
		return playerSaveManager;
	}
	
	public ConfigManager getConfigManager()
	{
		return this.cm;
	}
	
	
	
}
