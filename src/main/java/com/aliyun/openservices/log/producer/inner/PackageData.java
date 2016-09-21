package com.aliyun.openservices.log.producer.inner;

import java.util.LinkedList;
import java.util.List;

import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.producer.ILogCallback;
import com.aliyun.openservices.log.response.PutLogsResponse;

class PackageData 
{
	public String project;
	public String logstore;
	public String topic;
	public String shardHash;
	public String source;
	public LinkedList<LogItem> items = new LinkedList<LogItem>();
	public LinkedList<ILogCallback> callbacks = new LinkedList<ILogCallback>();
	public PackageData(String project, String logstore, String topic,
			String shardHash, String source) {
		super();
		this.project = project;
		this.logstore = logstore;
		this.topic = topic;
		this.shardHash = shardHash;
		this.source = source;
	}
	
	public void addItems(List<LogItem> logItems, ILogCallback callabck)
	{
		items.addAll(logItems);
		if(callabck != null)
		{
			callbacks.add(callabck);
		}
	}
	public void clear()
	{
		items.clear();
		callbacks.clear();
	}
	public void markAddToIOBeginTime()
	{
		long curr = System.currentTimeMillis();
		for(ILogCallback cb: callbacks){
			cb.addToIOQueueBeginTimeInMillis = curr;
		}
	}
	public void markAddToIOEndTime()
	{
		long curr = System.currentTimeMillis();
		for(ILogCallback cb: callbacks){
			cb.addToIOQueueEndTimeInMillis = curr;
		}
	}
	public void markCompleteIOBeginTimeInMillis(final int queueSize)
	{
		long curr = System.currentTimeMillis();
		for(ILogCallback cb: callbacks){
			cb.completeIOBeginTimeInMillis = curr;
			cb.ioQueueSize = queueSize;
		}
	}
	public void callback(PutLogsResponse response, LogException e, float srcOutFlow)
	{
		long curr = System.currentTimeMillis();
		for(ILogCallback cb: callbacks)
		{
			cb.completeIOEndTimeInMillis = curr;
			cb.sendBytesPerSecond = srcOutFlow;
			cb.onCompletion(response, e);
		}
	}
}
