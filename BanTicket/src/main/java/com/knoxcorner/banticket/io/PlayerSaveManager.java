package com.knoxcorner.banticket.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.Ban;
import com.knoxcorner.banticket.ban.Expirable;
import com.knoxcorner.banticket.ban.HistoryEvent;
import com.knoxcorner.banticket.ban.HistoryEvent.BanType;
import com.knoxcorner.banticket.ban.PermanentBan;
import com.knoxcorner.banticket.ban.PermanentBanRequest;
import com.knoxcorner.banticket.ban.TemporaryBan;
import com.knoxcorner.banticket.ban.TemporaryBanRequest;
import com.knoxcorner.banticket.listener.BTPlayer;
import com.knoxcorner.banticket.util.BanList;
import com.knoxcorner.banticket.util.Util;

public class PlayerSaveManager
{
	private BanTicket pl;
	private File saveFolder;
	
	private final static byte MAX_BAN_LINES = 7;
	private final static byte MAX_HISTORY_LINES = 4;
	
	public PlayerSaveManager(BanTicket plugin)
	{
		this.pl = plugin;
		saveFolder = new File(pl.getDataFolder(), "players");
		if(!saveFolder.exists())
			if(!saveFolder.mkdirs())
			{
				pl.getLogger().severe("WARNING: Could not create directory " + saveFolder.getPath() + "; bans will not be saved");
			}
	}
	
	public boolean playerExists(UUID uuid)
	{
		return new File(saveFolder, uuid.toString() + ".dat").exists();
	}
	
	public BTPlayer loadPlayer(UUID uuid)
	{
		return loadPlayer(uuid, null);
	}
	
	public BTPlayer loadPlayer(UUID uuid, String ip)
	{
		File file = new File(saveFolder, uuid.toString() + ".dat");
		if(!file.exists())
		{
			return null;
		}
		
		BufferedReader fIn;
		try
		{
			fIn = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e)
		{
			pl.getLogger().warning("File exists, but not found on reader: " + file.getPath());
			pl.getLogger().throwing(getClass().getName(), "loadPlayer", e);
			return null;
		}
		
		List<String> buffer = new ArrayList<String>(15); //15 lines is minimal size
		
		String lineString;
		
		try
		{
			while((lineString = fIn.readLine()) != null)
			{
				buffer.add(lineString);
			}
			fIn.close();
		} catch (IOException e)
		{
			pl.getLogger().warning("Error while reading file: " + file.getPath());
			pl.getLogger().throwing(getClass().getName(), "loadPlayer", e);
			return null;
		}
		
		LinkedHashMap<String, Integer> ips = null;
		LinkedHashMap<Long, String> prevNames = null;
		BanList banList = null;
		LinkedList<HistoryEvent> history = null;
		for(int line = 0; line < buffer.size(); line++) //TODO: versioning
		{
			if(buffer.get(line).equals("IP TABLE:"))
			{
				ips = this.loadIps(line + 1, buffer, file);
			}
			else if(buffer.get(line).equals("PREVIOUS NAMES:"))
			{
				prevNames = this.loadPrevNames(line + 1, buffer, file);
			}
			else if(buffer.get(line).equals("BAN LIST:"))
			{
				banList = this.loadBans(line + 1, buffer, file, uuid);
			}
			else if(buffer.get(line).equals("HISTORY:"))
			{
				history = this.loadHistory(line + 1, buffer, file);
			}	
		}
		
		if(ips == null)
		{
			pl.getLogger().severe("IPs failed to load in file " + file.getPath());
			return null;
		}
		if(prevNames == null)
		{
			pl.getLogger().severe("Previous names failed to load in file " + file.getPath());
			return null;
		}
		if(banList == null)
		{
			pl.getLogger().severe("Bans failed to load in file " + file.getPath());
			return null;
		}
		if(history == null)
		{
			pl.getLogger().severe("History failed to load in file " + file.getPath());
			return null;
		}

		
		return new BTPlayer(uuid, ips, prevNames, banList, history);
	}

	private LinkedList<HistoryEvent> loadHistory(int lineOffset, List<String> buffer, File file)
	{
		int lines = 0;
		LinkedList<HistoryEvent> history = new LinkedList<HistoryEvent>();
		while(lineOffset + lines < buffer.size() && buffer.get(lineOffset + lines).startsWith("\t")) //History subcategory
		{
			if(buffer.get(lineOffset + lines).startsWith("\t\t"))//Possible previous error
			{
				lines++;
				continue;
			}
			String line = buffer.get(lineOffset + lines).substring(1); //Cut out first tab
			BanType banType = null;
			for(BanType type : BanType.values())
			{
				if(line.equals(type.toString()))
				{
					banType = type;
					break;
				}
			}
			if(banType == null)
			{
				pl.getLogger().warning("\"" + line + "\": illegal type on line " + (lineOffset + lines) + " of " + file.getPath());
				lines++;
				continue;
			}
			
			lines++; //Next line down
			
			long date = -1;
			long length = -1;
			
			String dateString = null;
			String event = null;
			String info = null;
			String lengthString = null;
			
			for(int i = lineOffset + lines; i < lineOffset + lines + MAX_HISTORY_LINES && i < buffer.size(); i++)
			{
				String curLine = buffer.get(i);
				if(!curLine.startsWith("\t\t")) //Too far
					break;
				curLine = curLine.substring(2);
				if(curLine.startsWith("DATE: "))
				{
					dateString = curLine.replaceFirst("DATE: ", "");
				}
				else if(curLine.startsWith("EVENT: "))
				{
					event = curLine.replaceFirst("EVENT: ", "");
				}
				else if(curLine.startsWith("INFO: "))
				{
					info = curLine.replaceFirst("INFO: ", "");
				}
				else if(curLine.startsWith("LENGTH: ") && (banType == BanType.TEMPBAN || banType == BanType.TEMPBANREQ))
				{
					lengthString = curLine.replaceFirst("LENGTH: ", "");
				}
				else
				{
					pl.getLogger().warning("\"" + curLine + "\": unexpected subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
				}
			}
			
			if(info == null)
			{
				pl.getLogger().warning("Missing INFO subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
			}
			else if(info.equals("null"))
			{
				info = null;
			}
			
			if(event == null)
			{
				pl.getLogger().warning("Missing EVENT subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
				pl.getLogger().warning("This history event will be removed on next plugin save");
				continue;
			}
			
			if(dateString == null)
			{
				pl.getLogger().warning("Missing DATE subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
				pl.getLogger().warning("This history event will be removed on next plugin save");
				continue;
			}
			
			if(lengthString == null && (banType == BanType.TEMPBAN || banType == BanType.TEMPBANREQ))
			{
				pl.getLogger().warning("Missing LENGTH subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
				pl.getLogger().warning("The ban length will be replace with 0");
				lengthString = "0";
			}
				
			try
			{
				if((banType == BanType.TEMPBAN || banType == BanType.TEMPBANREQ))
					length = Long.parseLong(lengthString);
			} catch (NumberFormatException nfe)
			{
				pl.getLogger().warning("\"" + lengthString + "\": not a valid long under line " + (lineOffset + lines) + " of " + file.getPath());
				pl.getLogger().warning("The ban length will be replace with 0");
				length = 0;
			}
			
			try
			{
				date = Long.parseLong(dateString);
			} catch (NumberFormatException nfe)
			{
				pl.getLogger().warning("\"" + dateString + "\": not a valid long under line " + (lineOffset + lines) + " of " + file.getPath());
				pl.getLogger().warning("This history event will be removed on next plugin save");
				continue;
			}
			HistoryEvent he = null;
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(date);
			if(banType == BanType.TEMPBAN || banType == BanType.TEMPBANREQ)
			{
				he = new HistoryEvent(banType, event, cal, length);
			}
			else
			{
				he = new HistoryEvent(banType, event, cal);
			}
			
			he.setExtraInfo(info);
			history.add(he);
			lines++;
		}
		return history;
	}
	
	private BanList loadBans(int lineOffset, List<String> buffer, File file, UUID playersUuid)
	{
		int lines = 0;
		BanList bans = new BanList();
		while(lineOffset + lines < buffer.size() && buffer.get(lineOffset + lines).startsWith("\t")) //Ban subcategory only
		{
			if(buffer.get(lineOffset + lines).startsWith("\t\t"))//Possible previous error
			{
				lines++;
				continue;
			}
			String line = buffer.get(lineOffset + lines).substring(1); //Cut out first tab
			String[] parts = line.split(" ");
			if(parts.length != 3 && parts.length != 4) //Bad line
			{
				pl.getLogger().severe("\"" + line + "\": bad formatting on line " + (lineOffset + lines) + " of " + file.getPath());
				pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
			}
			else
			{
				boolean isFull = false;
				boolean isIp = false;
				boolean isPerm = false;
				long banEnd = -1;
				long startTime = -1;
				long expTime = -1;
				boolean aoe = false;
				
				String ticketType = parts[0];
				String idType = parts[1];
				String lengthType = parts[2];
				String length = null;
				
				if(parts.length == 4)
					length = parts[3];
				
				//Check data
				if(!ticketType.equals("FULL") && !ticketType.equals("REQUEST"))
				{
					pl.getLogger().severe("\"" + ticketType + "\": bad formatting on line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				if(!idType.equals("IP") && !idType.equals("UUID"))
				{
					pl.getLogger().severe("\"" + idType + "\": bad formatting on line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				if(!lengthType.equals("PERMANENT") && !lengthType.equals("TEMPORARY"))
				{
					pl.getLogger().severe("\"" + lengthType + "\": bad formatting on line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				else if(parts.length != 4 && lengthType.equals("TEMPORARY"))
				{
					pl.getLogger().severe("\"" + line + "\": missing ban length on line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				
				isFull = ticketType.equals("FULL");
				isIp = idType.equals("IP");
				isPerm = lengthType.equals("PERMANENT");
				
				if(parts.length == 4)
				{
					try
					{
						banEnd = Long.parseLong(length);
					} catch (NumberFormatException nfe)
					{
						pl.getLogger().severe("\"" + length + "\": illegal ban length on line " + (lineOffset + lines) + " of " + file.getPath());
						pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
						lines++;
						continue;
					}
				}
				
				String info = null;
				String reason = null;
				String buuid = null;
				
				String startString = null;
				String expString = null;
				String aoeString = null;
				
				for(int i = lineOffset + lines + 1; i < buffer.size() && i < MAX_BAN_LINES + lineOffset + lines + 1; i++)
				{
					if(!buffer.get(i).startsWith("\t\t")) //too far
						break;
					if(buffer.get(i).startsWith("\t\tINFO: "))
					{
						info = buffer.get(i).replaceFirst("\t\tINFO: ", "");
					}
					else if(buffer.get(i).startsWith("\t\tBANNER UUID: "))
					{
						buuid = buffer.get(i).replaceFirst("\t\tBANNER UUID: ", "");
					}
					else if(buffer.get(i).startsWith("\t\tREASON: "))
					{
						reason = buffer.get(i).replaceFirst("\t\tREASON: ", "");
					}
					else if(!isFull && buffer.get(i).startsWith("\t\tSTART: "))
					{
						startString = buffer.get(i).replaceFirst("\t\tSTART: ", "");
					}
					else if(!isFull && buffer.get(i).startsWith("\t\tEXPIRE: "))
					{
						expString = buffer.get(i).replaceFirst("\t\tEXPIRE: ", "");
					}
					else if(!isFull && buffer.get(i).startsWith("\t\tAOE: "))
					{
						aoeString = buffer.get(i).replaceFirst("\t\tAOE: ", "");
					}
					else
					{
						//TODO: Error logging, copy errored files
						//TODO: Warn privileged
						pl.getLogger().severe("\"" + buffer.get(i) + "\": illegal subcategory on line " + (lineOffset + lines) + " of " + file.getPath());
						pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
						lines++;
						continue;
					}
				}
				
				if(info == null)
				{
					pl.getLogger().warning("Missing INFO subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
				}
				
				if(reason == null)
				{
					pl.getLogger().severe("Missing REASON subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				
				if(buuid == null)
				{
					pl.getLogger().warning("Missing BANNER UUID subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().warning("Will replace with console for banner UUID");
					buuid = "CONSOLE";
				}
				
				if(!isFull && startString == null)
				{
					pl.getLogger().severe("Missing START subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				
				if(!isFull && expString == null)
				{
					pl.getLogger().severe("Missing EXPIRE subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				
				if(!isFull && aoeString == null)
				{
					pl.getLogger().severe("Missing AOE subcatagory under line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				
				try
				{
					if(!isFull)
					{
						startTime = Long.parseLong(startString);
					}
				} catch (NumberFormatException nfe)
				{
					pl.getLogger().severe("\"" + startTime + "\": invalid start time under line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				
				try
				{
					if(!isFull)
					{
						expTime = Long.parseLong(expString);
					}
				} catch (NumberFormatException nfe)
				{
					pl.getLogger().severe("\"" + startTime + "\": invalid expire time under line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().severe("WARNING: This player's ban will be removed from the file when saving, but will remain on Minecraft's ban list");
					lines++;
					continue;
				}
				
				aoe = Boolean.parseBoolean(aoeString);
					
				
				
				UUID bannersUuid = null;
				try
				{
					if(buuid != null && !buuid.equals("CONSOLE"))
						bannersUuid = UUID.fromString(buuid);
				} catch (IllegalArgumentException iae)
				{
					pl.getLogger().severe("\"" + buuid + "\": invalid UUID under line " + (lineOffset + lines) + " of " + file.getPath());
					pl.getLogger().warning("Will replace with console for banner UUID");
				}
				
				
				
				Ban ban = null;
				if(isFull)
				{
					if(isPerm)
					{
						ban = new PermanentBan(playersUuid, reason, info, bannersUuid, isIp);
					}
					else
					{
						ban = new TemporaryBan(playersUuid, reason, info, bannersUuid, isIp, banEnd);
					}
				}
				else
				{
					if(isPerm)
					{
						ban = new PermanentBanRequest(playersUuid, reason, info, bannersUuid, isIp, startTime, expTime, aoe);
					}
					else
					{
						ban = new TemporaryBanRequest(playersUuid, reason, info, bannersUuid, isIp, banEnd, startTime, expTime, aoe);
					}
				}
				bans.add(ban);
				
				
			}
			lines++;
		}
		return bans;
	}

	private LinkedHashMap<Long, String> loadPrevNames(int lineOffset, List<String> buffer, File file)
	{
		int lines = 0;
		LinkedHashMap<Long, String> nameMap = new LinkedHashMap<Long, String>();
		while(lineOffset + lines < buffer.size() && buffer.get(lineOffset + lines).startsWith("\t"))
		{
			String line = buffer.get(lineOffset + lines).substring(1); //Cut out first tab
			String[] parts = line.split(" ");
			if(parts.length != 2) //Bad line
			{
				pl.getLogger().warning("\"" + line + "\": bad formatting on line " + (lineOffset + lines) + " of " + file.getPath());
			}
			else
			{
				long ms = -1;
				try
				{
					ms = Long.parseLong(parts[0]);
				} catch (NumberFormatException nfe)  //Bad count
				{
					pl.getLogger().warning("\"" + parts[0] + "\": invalid number on line " + (lineOffset + lines) + " of " + file.getPath());
					lines++;
					continue;
				}
				
				nameMap.put(ms, parts[1]);
			}
			lines++;
		}
		return nameMap;
	}

	private LinkedHashMap<String, Integer> loadIps(int lineOffset, List<String> buffer, File file)
	{
		int lines = 0;
		LinkedHashMap<String, Integer> ipMap = new LinkedHashMap<String, Integer>();
		while(lineOffset + lines < buffer.size() && buffer.get(lineOffset + lines).startsWith("\t"))
		{
			String line = buffer.get(lineOffset + lines).substring(1); //Cut out first tab
			String[] parts = line.split(" ");
			if(parts.length != 2) //Bad line
			{
				pl.getLogger().warning("\"" + line + "\": bad formatting on line " + (lineOffset + lines) + " of " + file.getPath());
			}
			else
			{
				int count = -1;
				try
				{
					count = Integer.parseInt(parts[1]);
				} catch (NumberFormatException nfe)  //Bad count
				{
					pl.getLogger().warning("\"" + parts[1] + "\": invalid number on line " + (lineOffset + lines) + " of " + file.getPath());
					lines++;
					continue;
				}
				
				ipMap.put(parts[0], count);
			}
			lines++;
		}
		
		return ipMap;
	}
	
	public void savePlayer(BTPlayer player) //YamlConfiguration ain't cooperating damn it
	{
		File file = new File(saveFolder, player.getUUID().toString() + ".dat");
		if(!file.exists())
		{
			try
			{
				file.createNewFile();
			} catch (IOException e)
			{
				pl.getLogger().severe("Could not create: " + file.getPath());
				pl.getLogger().throwing(getClass().getName(), "savePlayer", e); 
				return;
			}
		}
		
		BufferedWriter fOut;
		try
		{
			fOut = new BufferedWriter(new FileWriter(file, false));
		} catch (IOException ioe)
		{
			pl.getLogger().severe("Failed to open writer to: " + file.getPath());
			pl.getLogger().severe("For player: " + player.getMostRecentName());
			pl.getLogger().throwing(getClass().getName(), "savePlayer", ioe);
			return;
		}
		
		
		LinkedList<String> buffer = new LinkedList<String>();
		buffer.add("IP TABLE:");

		for(Map.Entry<String, Integer> entry : player.getIpMap().entrySet())
		{
			buffer.add("\t" + entry.getKey() + " " + entry.getValue());
		}
		
		buffer.add("PREVIOUS NAMES:");
		
		for(Map.Entry<Long, String> entry : player.getNameMap().entrySet())
		{
			buffer.add("\t" + entry.getKey() + " " + entry.getValue());
		}
		
		buffer.add("BAN LIST:");
		
		//Format: PENDING IP TEMPORARY 123456789
		//		  FULL UUID TEMPORARY 123456789
		//		  PENDING IP PERMANENT
		//		  FULL UUID PERMANENT
		for(Ban ban : player.getBans()) 
		{
			String label = "\t";
			if(ban instanceof Expirable)
				label += "REQUEST";
			else
				label += "FULL";
			
			if(ban.isIpBan())
				label += " IP";
			else
				label += " UUID";
			
			if(ban.isPermanent())
				label += " PERMANENT";
			else
			{
				TemporaryBan tempBan = (TemporaryBan) ban;
				label += (" TEMPORARY " + tempBan.getEndTime());
			}
			
			buffer.add(label);
			
			buffer.add("\t\tREASON: " + ban.getReason());
			buffer.add("\t\tINFO: " + ban.getInfo());
			if(ban.getBannerUUID() != null)
				buffer.add("\t\tBANNER UUID: " + ban.getBannerUUID().toString());
			else
				buffer.add("\t\tBANNER UUID: " + "CONSOLE");
			if(ban instanceof Expirable)
			{
				Expirable exp = ((Expirable) ban);
				buffer.add("\t\tSTART: " + exp.getStartTime());
				buffer.add("\t\tEXPIRE: " + exp.getExpireTime());
				buffer.add("\t\tAOE: " + exp.getApproveOnExpire());
			}
			
			
		}
		
		buffer.add("HISTORY:");
		for(HistoryEvent he : player.getHistory())
		{
			buffer.add("\t" + he.getEventType());
			if(he.getEventType() == BanType.TEMPBAN || he.getEventType() == BanType.TEMPBANREQ)
				buffer.add("\t\tLENGTH: " + he.getBanTime());
			buffer.add("\t\tDATE: " + he.getCalendar().getTimeInMillis());
			buffer.add("\t\tEVENT: " + he.getEvent());
			buffer.add("\t\tINFO: " + he.getExtraInfo());
		}
		
		
		
		try
		{
			for(int i = 0; i < buffer.size(); i++)
			{
				fOut.write(buffer.get(i));
				fOut.newLine();
			}
		}
		catch (IOException ioe)
		{
			pl.getLogger().severe("Failed to write to: " + file.getPath());
			pl.getLogger().severe("For player: " + player.getMostRecentName());
			pl.getLogger().throwing(getClass().getName(), "savePlayer", ioe);
		}
		
		try
		{
			fOut.close();
		}
		catch (IOException ioe)
		{
			pl.getLogger().severe("Failed to close writer on: " + file.getPath());
			pl.getLogger().severe("For player: " + player.getMostRecentName());
			pl.getLogger().throwing(getClass().getName(), "savePlayer", ioe);
		}
		
		
		
	}
	
	


}
