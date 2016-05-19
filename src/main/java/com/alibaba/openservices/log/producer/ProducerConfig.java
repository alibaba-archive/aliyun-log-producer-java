package com.alibaba.openservices.log.producer;

public class ProducerConfig 
{
	public int packageTimeoutInMS = 50000;
	public int logsCountPerPackage = 4096;
	public int logsBytesPerPackage = 5 * 1024 * 1024;
	public int memPoolSizeInByte = 1000 * 1024 * 1024;
	public int ioThreadsCount = 1;
	public int shardHashUpdateIntervalInMS = 10 * 60 * 1000;
	public int retryTimes = 3;
}
