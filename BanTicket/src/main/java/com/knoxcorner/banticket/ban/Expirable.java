package com.knoxcorner.banticket.ban;

import java.util.List;
import java.util.UUID;

public interface Expirable
{
	public boolean isExpired();
	
	public Ban expire(List<String> ips);
	
	public long getStartTime();
	
	public long getExpireTime();
	
	public boolean getApproveOnExpire();
	
	public long getSoonestEndTime();
	
	public UUID getUUID();
	
	public Ban accept();

}
