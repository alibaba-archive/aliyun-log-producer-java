package com.aliyun.openservices.log.producer.inner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
	private ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
	private Thread thread;
	private String threadName = "log_producer_io_thread";
	private BlockingQueue<BlockedData> dataQueue = new LinkedBlockingQueue<BlockedData>();
	private ClientPool clientPool;
	private PackageManager manager;
	private ProducerConfig config;
	private boolean stop = false;
	private AtomicLong sendLogBytes = new AtomicLong(0L);
	private AtomicLong sendLogTimeWindowInMillis = new AtomicLong(0L);

	public IOThread(ClientPool cltPool, PackageManager man, ProducerConfig conf) {
		super();
		this.clientPool = cltPool;
		this.manager = man;
		this.config = conf;
		this.thread = new Thread(null, this, threadName);
		thread.start();
	}

	public void addPackage(PackageData data, int bytes, int logLineCount) {
		data.markAddToIOBeginTime();
		try {
			dataQueue.put(new BlockedData(data, bytes));
		} catch (InterruptedException e) {
		}
		data.markAddToIOEndTime();
	}

	public void stop() {
		stop = true;
		thread.interrupt();
		cachedThreadPool.shutdown();
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
							+ " is not exist", ""), 0);
		} else {
			int retry = 0;
			LogException excep = null;
			PutLogsResponse response = null;
			while (retry++ <= config.retryTimes) {
				try {
					if (bd.data.shardHash != null
							&& !bd.data.shardHash.isEmpty()) {
						PutLogsRequest request = new PutLogsRequest(
								bd.data.project, bd.data.logstore,
								bd.data.topic, bd.data.source, bd.data.items,
								bd.data.shardHash);
						request.setContentType(config.logsFormat == "protobuf" ? Consts.CONST_PROTO_BUF
								: Consts.CONST_SLS_JSON);
						response = clt.PutLogs(request);

					} else {
						PutLogsRequest request = new PutLogsRequest(
								bd.data.project, bd.data.logstore,
								bd.data.topic, bd.data.source, bd.data.items);
						request.setContentType(config.logsFormat == "protobuf" ? Consts.CONST_PROTO_BUF
								: Consts.CONST_SLS_JSON);
						response = clt.PutLogs(request);
					}
					long tmpBytes = sendLogBytes.get();
					sendLogBytes.set(tmpBytes + bd.bytes);
					break;
				} catch (LogException e) {
					excep = new LogException(e.GetErrorCode(),
							e.GetErrorMessage() + ", itemscount: "
									+ bd.data.items.size(), e.GetRequestId());
				}
			}
			long currTime = System.currentTimeMillis();
			float sec = (currTime - sendLogTimeWindowInMillis.get()) / 1000.0f;
			float outflow = 0;
			if (sec > 0)
				outflow = sendLogBytes.get() / sec;
			bd.data.callback(response, excep, outflow);
		}
		manager.releaseBytes(bd.bytes);
	}

	public void run() {
		while (!stop) {
			long currTime = System.currentTimeMillis();
			if ((currTime - sendLogTimeWindowInMillis.get()) > 60 * 1000) {
				sendLogBytes.set(0L);
				sendLogTimeWindowInMillis.set(currTime);
			}
			try {
				final BlockedData bd = dataQueue.poll(
						config.packageTimeoutInMS / 2, TimeUnit.MILLISECONDS);
				if (bd != null) {
					bd.data.markCompleteIOBeginTimeInMillis(dataQueue.size());
					cachedThreadPool.submit(new Runnable() {
						public void run() {
							sendData(bd);
						}
					});
				}
			} catch (InterruptedException e) {
			}
		}
	}
}
