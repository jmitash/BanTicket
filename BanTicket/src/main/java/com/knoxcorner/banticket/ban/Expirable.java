package com.knoxcorner.banticket.ban;

import java.util.List;

public interface Expirable
{
	public boolean isExpired();
	
	public Ban expire(List<String> ips);
	
	public long getStartTime();
	
	public long getExpireTime();
	
	public boolean getApproveOnExpire();

}
