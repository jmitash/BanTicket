package com.knoxcorner.banticket;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.knoxcorner.banticket.ban.PermanentBan;
import com.knoxcorner.banticket.listener.BTPlayer;
import com.knoxcorner.banticket.util.BanList;
import com.knoxcorner.banticket.util.Util;

public class BanTicket extends JavaPlugin
{
	public static BanTicket banTicket;
	public BanList bans;

	public void onEnable()
	{
		banTicket = this;
		bans = new BanList();
	}
	
	public void onDisable()
	{
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if(!cmd.getName().equalsIgnoreCase("bt"))
			return false;
		
		if(args.length == 0)
		{
			sender.sendMessage(ChatColor.BLUE + "No help yet :(");
			return true;
		}
		
		String user = args[0];
		OfflinePlayer player = Util.findOnlinePlayer(user);
		boolean online = player != null;
		
		if(!online)
		{
			//Load file
			//Check online
			sender.sendMessage(ChatColor.RED + "Player not online");
			return true;
		}
		if(player != null)
		{
			PermanentBan pb = new PermanentBan(player.getUniqueId(),
					"LOL WHY NOT",
					"Beep", 
					(sender instanceof Player) ? ((Player) sender).getUniqueId() : null,
					false);
			
			BTPlayer btpl = new BTPlayer(player.getUniqueId(), player.getPlayer().getAddress().toString(), player.getName());
			btpl.addBan(pb);
		}
		return true;
	}
	
	
	
}
