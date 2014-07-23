package com.knoxcorner.banticket.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.IpBan;

public class IpBanManager
{
	
	private final static byte IP_BANS_VERSION = 1;
	
	private BanTicket pl;
	private File file;
	private List<IpBan> ipBans;

	public IpBanManager(BanTicket bt)
	{
		this.pl = bt;
		file = new File(pl.getDataFolder(), "ipbans.dat");
		if(!file.exists())
		{
			try
			{
				file.createNewFile();
				FileWriter fOut = new FileWriter(file);
				fOut.write("VERSION: " + IP_BANS_VERSION + "\n");
				fOut.close();
			} catch (IOException e)
			{
				pl.getLogger().severe("Error creating/writing " + file.getName()); 
				e.printStackTrace();
			}
		}
		ipBans = new ArrayList<IpBan>();
	}
	
	public synchronized IpBan getBan(String ip) throws Exception
	{
		for(IpBan ipban : ipBans)
		{
			if(ipban.getIps().contains(ip))
			{
				
				if(ipban.isOver())
				{
					this.removeBan(ipban);
					throw new Exception("Ban over");
				}
				
				if(ipban.isExpired())
				{
					this.removeBan(ipban);
					ipban = ipban.expire();
					if(ipban != null)
					{
						this.addBan(ipban);
						return ipban;
					}
					else
					{
						throw new Exception("Ban expired");
					}
				}
				
				return ipban;
			}
		}
		return null;
	}
	
	public synchronized IpBan getBan(List<String> ips) throws Exception
	{
		IpBan temp;
		for(String ip : ips)
		{
			if((temp = this.getBan(ip)) != null)
				return temp;
		}
		return null;
	}
	
	public synchronized byte removeBan(IpBan ban)
	{
		boolean foundBan = false;
		for(int i = 0; i < this.ipBans.size(); i++)
		{
			if(ipBans.get(i).getIps().containsAll(ban.getIps()))
			{
				ipBans.remove(i);
				foundBan = true;
				break;
			}
		}
		if(!foundBan)
			return 1;
		
		Scanner in;
		try
		{
			in = new Scanner(file);
		} catch (FileNotFoundException e)
		{
			pl.getLogger().severe("ipbans.dat appears to exist, but Scanner threw exception.");
			pl.getLogger().throwing(this.getClass().getName(), "load", e);
			return 2;
		}
		
		List<String> buffer = new ArrayList<String>(10);
		while(in.hasNextLine())
		{
			buffer.add(in.nextLine());
		}
		in.close();
		
		for(int i = 0; i < buffer.size(); i++)
		{
			String line = buffer.get(i);
			if(!line.startsWith("IP: "))
			{
				continue;
			}
			
			String[] parts = line.split(" ");
			if(parts.length >= 2)
			{
				String[] ips = parts[1].split(",");
				if(ban.getIps().containsAll(Arrays.asList(ips)))
				{
					buffer.remove(i);
					break;
				}
			}
		}
		
		FileWriter fOut;
		try
		{
			fOut = new FileWriter(file, false);
		} catch (IOException e)
		{
			pl.getLogger().severe("Error while trying to overwrite " + file.getName()); 
			e.printStackTrace();
			return 2;
		}
		
		try
		{
			for(int i = 0; i < buffer.size(); i++)
			{
				
					fOut.write(buffer.get(i) + "\n");
			}
			
			fOut.close();
		} catch (IOException e)
		{
			pl.getLogger().severe("Error while trying to write to " + file.getName());
			e.printStackTrace();
			return 2;
		}
		
		return 0;
	}
	
	/**
	 * Adds ban to ban list and saves
	 * @param ban the IpBan to add
	 * @return 0 - Success<br>1 - Already banned<br>2 - Error saving
	 */
	public synchronized byte addBan(IpBan ban)
	{
		for(IpBan ipban : ipBans)
		{
			if(!Collections.disjoint(ipban.getIps(), ban.getIps())) //Any in common
			{
				if(ipban.isOver())
				{
					this.removeBan(ipban);
					break;
				}
				if(ipban.isExpired())
				{
					this.removeBan(ipban);
					ipban = ipban.expire();
					if(ipban == null) 
						break; //Ban is over, continue on
					else
					{
						this.addBan(ipban);
						return 1;
					}
				}
			}
		}
		
		this.ipBans.add(ban);
		try
		{
			FileWriter fOut = new FileWriter(file, true);
			String line = "IP: ";
			for(int i = 0; i < ban.getIps().size() - 1; i++)
			{
				line += (ban.getIps().get(i) + ",");
			}
			line += ban.getIps().get(ban.getIps().size() - 1); //No comma for last
			line += " ";
			line += ban.getReason();
			line += "\u00C4";
			line += ban.getInfo();
			line += "\u00C4";
			line += ban.getEndTime();
			line += " ";
			line += (ban.getUUID() == null) ? "null" : ban.getUUID().toString();
			line += " ";
			line += (ban.getBannerUUID() == null) ? "null" : ban.getBannerUUID().toString();
			line += " ";
			line += ban.isRequest();
			if(ban.isRequest())
			{
				line += " ";
				line += ban.getExpireTime();
				line += " ";
				line += ban.isApproveOnExpire();
			}
			
			line += "\n";
			fOut.write(line);
			fOut.close();
		} catch (IOException e)
		{
			pl.getLogger().severe("Could not write to " + file.getName());
			pl.getLogger().throwing(this.getClass().getName(), "add", e);
			return 2; //TODO: 
		}
		
		return 0;
	}
	
	public boolean load()
	{
		if(!file.exists())
			return true;
		
		Scanner in;
		try
		{
			in = new Scanner(file);
		} catch (FileNotFoundException e)
		{
			pl.getLogger().severe("ipbans.dat appears to exist, but Scanner threw exception.");
			pl.getLogger().throwing(this.getClass().getName(), "load", e);
			return false;
		}
		
		Scanner tempScan;
		byte version = -1;
		List<String> buffer = new ArrayList<String>(10);
		while(in.hasNextLine())
		{
			buffer.add(in.nextLine());
		}
		in.close();
		
		for(int i = 0; i < buffer.size(); i++)
		{
			String line = buffer.get(i);
			tempScan = new Scanner(line);
			{
				if(tempScan.hasNext())
				{
					String identifier = tempScan.next();
					if(identifier.equals("VERSION:"))
					{
						if(tempScan.hasNextByte())
						{
							version = tempScan.nextByte();
						}
						else
						{
							pl.getLogger().warning("Missing version number in " + file.getName());
							pl.getLogger().warning("Assuming " + IP_BANS_VERSION);
							version = IP_BANS_VERSION;
						}
					}
					else//IP listing
					{
						try
						{
							
							String ips = tempScan.next();
							
							tempScan.useDelimiter("\u00C4");
							String reason = tempScan.next();
							String info = tempScan.next();
							tempScan.reset();
							tempScan.skip("\u00C4");
							long endTime = tempScan.nextLong();
							String uuidStr = tempScan.next();
							String bannerUuidStr = tempScan.next();
							boolean isReq = tempScan.nextBoolean();
							long expireTime = -1;
							boolean aoe = false;
							if(isReq)
							{
								expireTime = tempScan.nextLong();
								aoe = tempScan.nextBoolean();
							}
							
							
							Scanner ipsScan = new Scanner(ips);
							ipsScan.useDelimiter(",");
							
							List<String> ipsList = new ArrayList<String>();
							
							while(ipsScan.hasNext())
								ipsList.add(ipsScan.next());
							
							ipsScan.close();
							
							UUID banned =  uuidStr.equals("null") ? null : UUID.fromString(uuidStr);
							UUID banner = bannerUuidStr.equals("null") ? null : UUID.fromString(bannerUuidStr);
							
							IpBan ipban = new IpBan(ipsList, reason, info, endTime, banned, 
									banner, isReq, expireTime, aoe);
							
							if(ipban.isOver())
							{
								//it's over- dont add it back
								this.removeBan(ipban);
							}
							else if(ipban.isExpired())
							{
								//its expired, fetch new ban if needed
								IpBan newBan = ipban.expire();
								if(newBan != null)
								{
									this.addBan(newBan);
								}
								this.removeBan(ipban);
							}
							else
							{
								this.ipBans.add(ipban);
							}
							
							
							
						} catch(InputMismatchException ime)
						{
							pl.getLogger().severe("Input mismatch in " + file.getName() + ". Ban will be lost.");
							pl.getLogger().throwing(this.getClass().getName(), "load", ime);
							continue;
						}
						catch(NoSuchElementException nsee)
						{
							pl.getLogger().severe("Missing element in " + file.getName() + ". Ban will be lost.");
							pl.getLogger().throwing(this.getClass().getName(), "load", nsee);
							continue;
						}
					}
				}
				else
				{
					pl.getLogger().warning("Found unexpected empty line in " + file.getName()); 
				}
			}
			tempScan.close();
			
		}
	
		
		
		return true;
	}

}
