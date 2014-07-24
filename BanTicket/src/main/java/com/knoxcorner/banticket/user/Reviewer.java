package com.knoxcorner.banticket.user;

import org.bukkit.command.CommandSender;

public interface Reviewer
{

	public void sendFormattedReviews(CommandSender cs);
	
	public void handleBanReview(int num, boolean accept, CommandSender cs);
}
