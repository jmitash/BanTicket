package com.knoxcorner.banticket.ban;

import java.util.Calendar;

import com.knoxcorner.banticket.util.Util;
import com.knoxcorner.banticket.ban.BanType;


public class HistoryEvent
{

	private Calendar calendar;
	private String event;
	private BanType eventType;
	private long banTime;
	private String extraInfo;
	
	public HistoryEvent(Ban ban)
	{
		this.event = ban.getReason();
		this.eventType = ban.getType();
		this.calendar = Calendar.getInstance();
		this.extraInfo = ban.getInfo();
		if(ban.getType() == BanType.TEMPBAN)
		{
			this.banTime = ((TemporaryBan) ban).getEndTime() - System.currentTimeMillis(); //new bans only
		}
	}
	
	/**
	 * Creates an event at the current time<br>NOTE: TEMPBANS SHOULD USE TEMPBAN CONSTRUCTOR
	 * @param type ban type
	 * @param event description of this event
	 */
	public HistoryEvent(BanType type, String event)
	{
		this.calendar = Calendar.getInstance();
		this.event = event;
		this.eventType = type;
		
	}
	
	/**
	 * Creates an event at the given time<br>NOTE: TEMPBANS SHOULD USE TEMPBAN CONSTRUCTOR
	 * @param type ban type
	 * @param event description of this event
	 * @param cal time of event
	 */
	public HistoryEvent(BanType type, String event, Calendar cal)
	{
		this.event = event;
		this.calendar = cal;
		this.eventType = type;
	}
	
	/**
	 * Creates an event at the given time<br>NOTE: ONLY FOR TEMPBAN TYPE
	 * @param type ban type
	 * @param event description of this event
	 * @param cal time of event
	 */
	public HistoryEvent(String event, Calendar cal, long banLength)
	{
		this.event = event;
		this.calendar = cal;
		this.eventType = BanType.TEMPBAN;
		this.banTime = banLength;
	}
	
	public void setExtraInfo(String info)
	{
		this.extraInfo = info;
	}
	
	public String getExtraInfo()
	{
		if(this.extraInfo == null)
			return "No extra info available.";
		else
			return this.extraInfo;
	}
	
	public String getEvent()
	{
		return event;
	}
	
	public Calendar getCalendar()
	{
		return calendar;
	}
	
	public String getFormattedLabel()
	{
		String label = String.format("Type: %s; Date: %tD %tl:%tM %tp", this.eventType, calendar, calendar, calendar, calendar);
		if(this.eventType == BanType.TEMPBAN)
		{
			label += "; Length: ";
			label += Util.msToTime(this.banTime);
		}
		
		return label;
	}
	
	public long getBanTime()
	{
		return this.banTime;
	}
	
	public BanType getEventType()
	{
		return this.eventType;
	}

}
