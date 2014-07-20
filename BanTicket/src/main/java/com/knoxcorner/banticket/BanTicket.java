package com.knoxcorner.banticket;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.knoxcorner.banticket.ban.TemporaryBanRequest;
import com.knoxcorner.banticket.io.ConfigManager;
import com.knoxcorner.banticket.io.PlayerSaveManager;
import com.knoxcorner.banticket.listener.BTPlayer;
import com.knoxcorner.banticket.util.Util;

public class BanTicket extends JavaPlugin
{
	public static BanTicket banTicket;
	
	private ConfigManager cm;
	private PlayerSaveManager playerSaveManager;

	@Override
	public void onEnable()
	{
		banTicket = this;
		cm = new ConfigManager(this);
		playerSaveManager = new PlayerSaveManager(this);
		cm.loadConfig();
		
	}
	
	@Override
	public void onDisable()
	{
		cm.saveConfig();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		
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
