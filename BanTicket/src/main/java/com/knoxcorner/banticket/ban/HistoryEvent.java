package com.knoxcorner.banticket.ban;

import java.util.Calendar;

import com.knoxcorner.banticket.util.Util;


public class HistoryEvent
{
	public enum BanType
	{
		INFO,
		WARN,
		TEMPBAN,
		PERMBAN,
		TEMPBANREQ,
		PERMBANREQ
	}

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
		if(ban.getType() == BanType.TEMPBAN || ban.getType() == BanType.TEMPBANREQ)
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
	public HistoryEvent(BanType type, String event, Calendar cal, long banLength)
	{
		this.event = event;
		this.calendar = cal;
		this.eventType = type;
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
	
	public long getBanTime()
	{
		return this.banTime;
	}
	
	public BanType getEventType()
	{
		return this.eventType;
	}

}
