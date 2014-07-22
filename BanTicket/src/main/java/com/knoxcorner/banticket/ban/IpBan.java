package com.knoxcorner.banticket.ban;

import java.util.List;
import java.util.UUID;

import com.knoxcorner.banticket.util.Util;

public class IpBan
{

	private List<String> ips;
	private String reason;
	private String info;
	private long endTime;
	private UUID uuid;
	private UUID bannerUuid;
	private boolean isPermanent;
	private boolean isRequest;
	private long expireTime;
	private boolean approveOnExpire;
	
	/**
	 * Holds information about IP ban
	 * @param ips list of IPs to ban
	 * @param reason reason for this IP ban
	 * @param info extra info on this ban
	 * @param endtime time this ban should end; if it is permanent, pass any negative value to indicate so
	 * @param uuid the UUID of the person to be banned, or null if no associated UUID
	 * @param bannerUuid the UUID of the person who placed the ban, or null if banned from console
	 * @param isReq true if ban is a request, false otherwise
	 * @param expireTime unix time this ban expires. If the ban is not a request, variable is ignored
	 * @param approveOnExpire true if this ban should approve on expire. If not a request, variable is ignored
	 */
	public IpBan(List<String> ips, String reason, String info, long endtime, UUID uuid, UUID bannerUuid, 
			boolean isReq, long expireTime, boolean approveOnExpire)
	{
		this.ips = ips;
		this.reason = reason;
		this.info = info;
		this.endTime = endtime;
		this.uuid = uuid;
		this.bannerUuid = bannerUuid;
		this.isPermanent = endTime < 0;
		this.isRequest = isReq;
		this.expireTime = expireTime;
		this.approveOnExpire = approveOnExpire;
	}
	
	public IpBan expire()
	{
		if(!this.isRequest)
			return null;
		if(!this.isExpired())
			return null;
		if(!this.approveOnExpire)
			return null;
		
		return new IpBan(ips, reason, "Auto Renewal " + Util.getDate() + ": " + info, endTime, uuid, bannerUuid, false, -1, this.approveOnExpire);
	}
	
	public List<String> getIps()
	{
		return this.ips;
	}
	
	public String getReason()
	{
		return this.reason;
	}
	
	public String getInfo()
	{
		return this.info;
	}
	
	public long getEndTime()
	{
		return this.endTime;
	}
	
	public UUID getUUID()
	{
		return this.uuid;
	}
	
	public UUID getBannerUUID()
	{
		return this.bannerUuid;
	}
	
	public boolean isOver()
	{
		return System.currentTimeMillis() > this.endTime
				|| (this.isRequest && !this.approveOnExpire && System.currentTimeMillis() > this.expireTime);
	}
	
	public boolean isRequest()
	{
		return this.isRequest;
	}
	
	public boolean isPermanent()
	{
		return this.isPermanent;
	}

	public boolean isApproveOnExpire()
	{
		return approveOnExpire;
	}
	
	public long getExpireTime()
	{
		return this.expireTime;
	}
	
	public boolean isExpired()
	{
		return this.isRequest && System.currentTimeMillis() > this.expireTime;
	}
	
	public String getBanMessage()
	{

		String msg;
		
		if(this.isPermanent)
		{
			msg = "Permanently IP Banned for:\n"
					+ (this.reason == null || this.reason.equals("null") ? "No reason given." : this.reason)
					+ "\nExtra info:\n"
					+ (this.info == null || this.info.equals("null") ? "No extra info given." : this.info);
		}
		else
		{
			msg = Util.msToTime(endTime - System.currentTimeMillis()) + 
			" remaining; You have been IP banned for\n"
			+ this.getReason()
			+ "\nExtra info:\n"
			+ this.getInfo();
		}
		if(this.isRequest)
		{
				msg = "You have been banned by a low-ranking staff member until a\n"
				+ "higher ranked staff member can approve/deny this ban.\n"
				+ "Ban will auto-" + (this.approveOnExpire ? "approved " : "denied ")
				+ "after " + Util.msToTime(this.expireTime - System.currentTimeMillis())
				+ ".\n\n"
				+ msg;
		}
		return msg;
	}
	

}
