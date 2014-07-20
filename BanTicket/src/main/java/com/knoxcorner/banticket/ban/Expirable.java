package com.knoxcorner.banticket.ban;

public interface Expirable
{
	public boolean isExpired();
	
	public Ban expire();
	
	public long getStartTime();
	
	public long getExpireTime();
	
	public boolean getApproveOnExpire();

}
