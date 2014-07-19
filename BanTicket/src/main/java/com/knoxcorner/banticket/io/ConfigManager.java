package com.knoxcorner.banticket.io;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.util.Util;

public class ConfigManager
{
	public final static int CONFIG_VERSION = 1; //Add actual config version handling on next version
	
	private BanTicket pl;
	private File configFile;
	
	private String prevExpireTime;
	
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
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		this.approveOnExpire = config.getBoolean("ApproveOnExpire", false);
		String timeToExpire = config.getString("ExpireTime", "2d0h0m0s");
		long msToExpire = Util.msFromTime(timeToExpire);
		if(msToExpire == -1)
		{
			pl.getLogger().severe("\"" + timeToExpire + "\" is not a valid time for ExpireTime. Tickets will not expire this run.");
			msToExpire = Long.MAX_VALUE;
		}
		this.timeTillExpiry = msToExpire;
		
		this.prevExpireTime = timeToExpire;
	}
	
	public void saveConfig()
	{
		if(!configFile.exists())
		{
			pl.saveDefaultConfig();
			return;
		}
		
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		config.set("ApproveOnExpire", this.approveOnExpire);
		config.set("ExpireTime", this.prevExpireTime);
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
