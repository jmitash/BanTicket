package com.knoxcorner.banticket.user;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import com.knoxcorner.banticket.BanTicket;
import com.knoxcorner.banticket.ban.Ban;
import com.knoxcorner.banticket.ban.Expirable;
import com.knoxcorner.banticket.ban.HistoryEvent;
import com.knoxcorner.banticket.ban.HistoryEvent.BanType;
import com.knoxcorner.banticket.ban.TemporaryBanRequest;
import com.knoxcorner.banticket.util.Util;

public class BTConsole implements Reviewer
{

	private List<Expirable> banReviews;

	public void sendFormattedReviews(CommandSender cs)
	{
		this.banReviews = new ArrayList<Expirable>(BanTicket.banTicket.getRequestBanManager().getBans()); //Grab a copy, so our numbers dont change
		if(this.banReviews.size() == 0)
		{
			cs.sendMessage(ChatColor.RED + "Nothing to review at the moment.");
		}
		for(int i = 0; i < this.banReviews.size(); i++)
		{
			Expirable e = this.banReviews.get(i);
			Ban b = (Ban) e;
			String msg = (i + 1) + ". ";
			if(b.isPermanent())
				msg += "permanent ";
			else
			{
				TemporaryBanRequest tbr = (TemporaryBanRequest) e;
				msg += (Util.msToTime(tbr.getEndTime() - tbr.getStartTime()) + " ");
			}
			
			String bannedName = null;
			for(OfflinePlayer op : BanTicket.banTicket.getServer().getOfflinePlayers()) //Find name in offline players
			{
				if(op.getUniqueId().equals(b.getUUID()))
				{
					bannedName = op.getName();
					break;
				}
			}
			
			if(bannedName == null) //Didnt find him, check our files
			{
				BTPlayer btpl = BanTicket.banTicket.getPlayerSaveManager().loadPlayer(b.getUUID());
				if(btpl != null)
					bannedName = btpl.getMostRecentName();
			}
			
			if(bannedName != null)
			{
				msg += (bannedName + " ");
			}
			else
			{
				msg += ("ERR ");
			}
			
			msg += b.getReason();
			
			cs.sendMessage(ChatColor.GREEN + msg);
		}
	}
	
	public void handleBanReview(int num, boolean accept, CommandSender cs)
	{
		if(this.banReviews == null)
		{
			cs.sendMessage(ChatColor.RED + "You must first enter /btr to see the list!");
			return;
		}
		
		if(num > this.banReviews.size())
		{
			cs.sendMessage(ChatColor.RED + "That ID number isn't on the list! Enter /btr to see the list.");
			return;
		}
		
		num--;
		
		Expirable e = this.banReviews.get(num);
		Ban b = (Ban) e;
		
		if(b.isOver())
		{
			cs.sendMessage(ChatColor.RED + "That ban has already ended. Enter /btr to refresh the list.");
			return;
		}
		
		if(e.isExpired())
		{
			cs.sendMessage(ChatColor.RED + "That ban has already expired. Enter /btr to refresh the list.");
			return;
		}
		
		if(!BanTicket.banTicket.getRequestBanManager().getBans().contains(e))
		{
			cs.sendMessage(ChatColor.RED + "That ban is no longer in the list. Enter /btr to refresh the list");
			return;
		}
		
		BTPlayer btpl = BanTicket.banTicket.getPlayerSaveManager().loadPlayer(b.getUUID());
		if(btpl == null)
		{
			cs.sendMessage(ChatColor.RED + "There was an issue loading the save file for the banned player.");
			return;
		}
		
		if(accept)
		{
			Ban pBan = btpl.getBans().getActiveBan();
			if(pBan == null)
			{
				cs.sendMessage(ChatColor.RED + "Player was missing ban request in save file. Please ban manually.");
				return;
			}
			Expirable pEBan = (Expirable) pBan;
			Ban renewBan = pEBan.accept();
			btpl.getBans().remove(pBan);
			btpl.getBans().add(renewBan);
			btpl.addHistory(new HistoryEvent(BanType.INFO, "Ban Request accepted by CONSOLE"));
			btpl.save();
			renewBan.setOnServerBanList(true, btpl.getCommonIps());
			BanTicket.banTicket.getRequestBanManager().removeBan(e);
			cs.sendMessage(ChatColor.GREEN + "Ban approved. Use /btr to refresh the list");
		    BanTicket.banTicket.notify("banticket.notify.ban",
		    		ChatColor.DARK_GREEN + "CONSOLE has approved " + btpl.getMostRecentName() + "'s ban",
		    		false);
		}
		else
		{
			Ban pBan = btpl.getBans().getActiveBan();
			if(pBan == null)
			{
				cs.sendMessage(ChatColor.RED + "Player was missing ban request in save file.");
				return;
			}
			btpl.getBans().remove(pBan);
			btpl.addHistory(new HistoryEvent(BanType.INFO, "Ban Request denied by CONSOLE"));
			btpl.save();
			pBan.setOnServerBanList(false, btpl.getCommonIps());
			BanTicket.banTicket.getRequestBanManager().removeBan(e);
			
			cs.sendMessage(ChatColor.GREEN + "Ban denied. Use /btr to refresh the list");
		    BanTicket.banTicket.notify("banticket.notify.ban",
		    		ChatColor.DARK_GREEN + "CONSOLE has denied " + btpl.getMostRecentName() + "'s ban",
		    		false);
		}
	}
	

}
