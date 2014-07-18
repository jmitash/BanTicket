package com.knoxcorner.banticket.listener;

import org.bukkit.event.Listener;

import com.knoxcorner.banticket.BanTicket;

public class PlayerListener implements Listener
{
	private BanTicket banTicket;
	
	public PlayerListener(BanTicket banTicket)
	{
		this.banTicket = banTicket;
	}
}
