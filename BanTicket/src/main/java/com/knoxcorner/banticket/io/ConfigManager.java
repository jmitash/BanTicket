package com.knoxcorner.banticket.io;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;


import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.util.Util;

public class ConfigManager
{
	public final static int CONFIG_VERSION = 1; //Add actual config version handling on next version
	
	private BanTicket pl;
	private File configFile;
	
	private long timeTillExpiry;
	private boolean approveOnExpire;
	
	public ConfigManager(BanTicket plugin)
	{
		this.pl = plugin;
		this.configFile = new File(pl.getDataFolder(), "config.yml");
	}
	
	public void loadConfig()
	{
		if(!configFile.exists())
			this.saveConfig();
		FileConfiguration config = pl.getConfig();
		this.approveOnExpire = config.getBoolean("ApproveOnExpire", false);
		String timeToExpire = config.getString("ExpireTime", "2d0h0m0s");
		long msToExpire = Util.msFromTime(timeToExpire);
		if(msToExpire == -1)
		{
			pl.getLogger().severe("\"" + timeToExpire + "\" is not a valid time for ExpireTime. Tickets will expire on default of 48 hours.");
			msToExpire = Util.msFromTime("2d");
		}
		this.timeTillExpiry = msToExpire;
	}
	
	public void saveConfig()
	{
		if(!configFile.exists())
		{
			pl.saveDefaultConfig();
			return;
		}
		
		FileConfiguration config = pl.getConfig();
		config.set("ApproveOnExpire", this.approveOnExpire);
		config.set("ExpireTime", Util.msToTime(timeTillExpiry));
	}
	
	public long getExpireTime()
	{
		return timeTillExpiry;
	}
	
	public boolean getApproveOnExpire()
	{
		return approveOnExpire;
	}

}
