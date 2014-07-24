package com.knoxcorner.banticket.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.Ban;
import com.knoxcorner.banticket.ban.Expirable;
import com.knoxcorner.banticket.user.BTPlayer;

public class RequestBanManager
{

	private final static byte REQ_BAN_MANAGER_VERSION = 1;
	private BanTicket pl;
	private List<Expirable> bans;
	private File file;
	
	public RequestBanManager(BanTicket plugin)
	{
		pl = plugin;
		bans = new ArrayList<Expirable>();
		file = new File(pl.getDataFolder(), "banreqs.dat");
		
		try
		{
			
			if(!file.exists())
			{
				file.createNewFile();
				FileWriter fOut = new FileWriter(file);
				fOut.write("VERSION: " + REQ_BAN_MANAGER_VERSION + "\n");
				fOut.close();
			}
		} catch (IOException e)
		{
			pl.getLogger().severe("Could not create " + file.getName()); 
			e.printStackTrace();
		}
	}
	
	public List<Expirable> getBans()
	{
		update();
		return this.bans;
	}
	
	public void addBan(Expirable ban)
	{
		this.bans.add(ban);
		this.save();
	}
	
	public void removeBan(Expirable ban)
	{
		for(int i = 0; i < bans.size(); i++)
		{
			if(bans.get(i).getUUID().equals(ban.getUUID()))
			{
				bans.remove(i);
				this.save();
				return;
			}
		}
	}
	
	private void update()
	{
		boolean changed = false;
		for(int i = 0; i < bans.size(); i++)
		{
			if(((Ban)bans.get(i)).isOver() || bans.get(i).getSoonestEndTime() < System.currentTimeMillis())
			{
				bans.remove(i);
				changed = true;
			}
		}
		if(changed)
			this.save();
	}
	
	public void load()
	{
		
		Scanner sc = null;
		try
		{
			sc = new Scanner(file);
		} catch (FileNotFoundException e)
		{
			pl.getLogger().severe(file.getName() + " does not exist although it should have been created.");
			e.printStackTrace();
			return;
		}
		
		
		byte version = -1;
		if(sc.hasNextLine())//Version line
		{
			String line = sc.nextLine();
			String[] parts = line.split(" ");
			if(parts.length < 2)
			{
				pl.getLogger().severe("Expected version number in " + file.getName() + ". Assuming " + REQ_BAN_MANAGER_VERSION);
				version = REQ_BAN_MANAGER_VERSION;
			}
			else
			{
				try
				{
					version = Byte.parseByte(parts[1]);
					if(version < REQ_BAN_MANAGER_VERSION)
					{
						//
					}
				} catch (NumberFormatException nfe)
				{
					pl.getLogger().severe("Expected version number in " + file.getName() + ". Assuming " + REQ_BAN_MANAGER_VERSION);
					version = REQ_BAN_MANAGER_VERSION;
				}
			}
		}
		else
		{
			pl.getLogger().severe("Expected version number in " + file.getName() + ". Assuming " + REQ_BAN_MANAGER_VERSION);
			pl.getLogger().severe("Nothing left to read :(");
			version = REQ_BAN_MANAGER_VERSION;
		}
		
		List<UUID> uuids = new ArrayList<UUID>();
		
		while(sc.hasNextLine())
		{
			try
			{
				uuids.add(UUID.fromString(sc.nextLine()));
			} catch (IllegalArgumentException iae)
			{
				pl.getLogger().warning("Invalid UUID in " + file.getName());
			}
		}
		
		sc.close();
		
		for(UUID uuid : uuids)
		{
			BTPlayer btpl = pl.getPlayerSaveManager().loadPlayer(uuid);
			if(btpl == null)
				continue;
			Ban ban = btpl.getBans().getActiveBan();
			if(ban == null)
				continue;
			if(!(ban instanceof Expirable))
				continue;
			if(ban.isOver())
				continue;
			Expirable expBan = (Expirable) ban;
			if(expBan.getSoonestEndTime() > System.currentTimeMillis())
				this.bans.add(expBan);
		}
		
		
	}
	
	public void save()
	{
		update();
		List<String> buffer = new ArrayList<String>(this.bans.size() + 1);
		
		buffer.add("VERSION: " + REQ_BAN_MANAGER_VERSION);
		
		for(Expirable exp : bans)
		{
			buffer.add(exp.getUUID().toString());
		}
		
		FileWriter fOut;
		try
		{
			fOut = new FileWriter(file);
		} catch (IOException e)
		{
			pl.getLogger().severe("Failed to save " + file.getName());
			e.printStackTrace();
			return;
		}
		
		try
		{
			for(int i = 0; i < buffer.size(); i++)
			{
				fOut.write(buffer.get(i) + "\n");
			}
		} catch (IOException ioe)
		{
			pl.getLogger().severe("Faile to write to " + file.getName());
			ioe.printStackTrace();
			return;
		}
		
		try
		{
			fOut.close();
		} catch (IOException e)
		{
			pl.getLogger().severe("Failed to close " + file.getName());
			e.printStackTrace();
		}
	}

}
