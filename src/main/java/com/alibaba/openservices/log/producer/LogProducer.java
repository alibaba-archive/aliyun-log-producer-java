package com.alibaba.openservices.log.producer;

import java.util.List;

import com.alibaba.openservices.log.producer.inner.ClientPool;
import com.alibaba.openservices.log.producer.inner.PackageManager;
import com.aliyun.openservices.log.common.LogItem;

public class LogProducer 
{
	private ClientPool clientPool = new ClientPool();
	private ProducerConfig producerConfig;
	private PackageManager manager;
	
	public LogProducer(ProducerConfig producerConfig) {
		super();
		this.producerConfig = producerConfig;
		this.manager = new PackageManager(this.producerConfig, clientPool);
	}
	public void setProjectConfig(ProjectConfig config)
	{
		clientPool.updateClient(config);
	}
	public void removeProjectConfig(String project)
	{
		clientPool.removeClient(project);
	}
	public void send(String project, String logStore, String topic, String shardHash, String source, List<LogItem> logItems, ILogCallback callabck)
	{
		manager.add(project, logStore, topic, shardHash, source, logItems, callabck);
	}
	public void send(String project, String logStore, String topic, String shardHash, String source, List<LogItem> logItems)
	{
		manager.add(project, logStore, topic, shardHash, source, logItems, null);
	}
	public void send(String project, String logStore, String topic, String source, List<LogItem> logItems, ILogCallback callabck)
	{
		manager.add(project, logStore, topic, null, source, logItems, callabck);
	}
	public void send(String project, String logStore, String topic, String source, List<LogItem> logItems)
	{
		manager.add(project, logStore, topic, null, source, logItems, null);
	}
	public void flush()
	{
		manager.flush();
	}
	public void close()
	{
		manager.close();
	}
}
