package com.aliyun.openservices.log.producer.inner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.Consts;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.producer.ProducerConfig;
import com.aliyun.openservices.log.request.PutLogsRequest;
import com.aliyun.openservices.log.response.PutLogsResponse;

class BlockedData {
	public PackageData data;
	public int bytes;

	public BlockedData(PackageData data, int bytes) {
		super();
		this.data = data;
		this.bytes = bytes;
	}
};

class IOThread implements Runnable {
	private Thread[] ioThreads;
	private String threadNamePrefix = "log_producer_io_thread_";
	private BlockingQueue<BlockedData> dataQueue = new LinkedBlockingQueue<BlockedData>();
	private ClientPool clientPool;
	private PackageManager manager;
	private ProducerConfig config;
	private boolean stop = false;

	public IOThread(ClientPool cltPool, PackageManager man, ProducerConfig conf) {
		super();
		this.clientPool = cltPool;
		this.manager = man;
		this.config = conf;
		this.ioThreads = new Thread[config.ioThreadsCount];
		for (int i = 0; i < config.ioThreadsCount; ++i) {
			this.ioThreads[i] = new Thread(null, this, threadNamePrefix + i);
			this.ioThreads[i].start();
		}
	}

	public void addPackage(PackageData data, int bytes, int logLineCount) {
		try {
			dataQueue.put(new BlockedData(data, bytes));
		} catch (InterruptedException e) {
		}
	}

	public void stop() {
		stop = true;
		for (int i = 0; i < config.ioThreadsCount; ++i) {
			ioThreads[i].interrupt();
		}
		while (!dataQueue.isEmpty()) {
			try {
				BlockedData bd = dataQueue.poll(config.packageTimeoutInMS / 2,
						TimeUnit.MILLISECONDS);
				sendData(bd);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	protected void sendData(BlockedData bd) {
		Client clt = clientPool.getClient(bd.data.project);
		if (clt == null) {
			bd.data.callback(null, new LogException("ProjectConfigNotExist",
					"the config of project " + bd.data.project
							+ " is not exist", ""));
		} else {
			int retry = 0;
			LogException excep = null;
			PutLogsResponse response = null;
			while (retry++ <= config.retryTimes) {
				try {
					if (bd.data.shardHash != null
							&& !bd.data.shardHash.isEmpty()) {
						PutLogsRequest request = new PutLogsRequest(bd.data.project, bd.data.logstore, bd.data.topic, bd.data.source, bd.data.items, bd.data.shardHash);
						request.setContentType(config.logsFormat == "protobuf" ? Consts.CONST_PROTO_BUF : Consts.CONST_SLS_JSON);
						response = clt.PutLogs(request);

					} else {
						PutLogsRequest request = new PutLogsRequest(bd.data.project, bd.data.logstore, bd.data.topic, bd.data.source, bd.data.items);
						request.setContentType(config.logsFormat == "protobuf" ? Consts.CONST_PROTO_BUF : Consts.CONST_SLS_JSON);
						response = clt.PutLogs(request);
					}
					break;
				} catch (LogException e) {
					excep = new LogException(e.GetErrorCode(),
							e.GetErrorMessage() + ", itemscount: "
									+ bd.data.items.size(), e.GetRequestId());
				}
			}
			bd.data.callback(response, excep);
		}
		manager.releaseBytes(bd.bytes);
	}

	public void run() {
		while (!stop) {
			BlockedData bd = null;
			try {
				bd = dataQueue.poll(config.packageTimeoutInMS / 2,
						TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
			}
			if (bd == null) {
				continue;
			} else {
				sendData(bd);
			}
		}
	}
}
