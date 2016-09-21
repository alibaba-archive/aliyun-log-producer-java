package com.aliyun.openservices.log.producer;

import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.response.PutLogsResponse;

public abstract class ILogCallback
{
	public long callSendBeginTimeInMillis = 0;
	public long callSendEndTimeInMillis = 0;
	public long addToIOQueueBeginTimeInMillis = 0;
	public long addToIOQueueEndTimeInMillis = 0;
	public long completeIOBeginTimeInMillis = 0;
	public long completeIOEndTimeInMillis = 0;
	public int ioQueueSize = 0;
	public float sendBytesPerSecond = 0;
	public abstract void onCompletion(PutLogsResponse response, LogException e);
	@Override
	public String toString() {
		return "ILogCallback [callSendBeginTimeInMillis="
				+ callSendBeginTimeInMillis + ", callSendEndTimeInMillis="
				+ callSendEndTimeInMillis + ", addToIOQueueBeginTimeInMillis="
				+ addToIOQueueBeginTimeInMillis
				+ ", addToIOQueueEndTimeInMillis="
				+ addToIOQueueEndTimeInMillis
				+ ", completeIOBeginTimeInMillis="
				+ completeIOBeginTimeInMillis + ", completeIOEndTimeInMillis="
				+ completeIOEndTimeInMillis + ", ioQueueSize=" + ioQueueSize
				+ ", sendBytesPerSecond=" + sendBytesPerSecond + "]";
	}
	
	
}
