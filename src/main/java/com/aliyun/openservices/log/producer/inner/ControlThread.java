package com.aliyun.openservices.log.producer.inner;

import com.aliyun.openservices.log.producer.ProducerConfig;

public class ControlThread implements Runnable {
	private ShardHashManager shardHashManager;
	private PackageManager manager;
	private ProducerConfig producerConfig;
	private boolean stop = false;
	private Thread thread;

	public ControlThread(ShardHashManager shardHashManager,
			PackageManager manager, ProducerConfig producerConfig) {
		super();
		this.shardHashManager = shardHashManager;
		this.manager = manager;
		this.producerConfig = producerConfig;
		thread = new Thread(null, this, "log_producer_control_thread");
		thread.setDaemon(true);
		thread.start();
	}

	public void stop() {
		stop = true;
		thread.interrupt();
	}

	public void run() {
		long min = producerConfig.packageTimeoutInMS / 2, max = producerConfig.shardHashUpdateIntervalInMS * 2;
		if (min > max) {
			long tmp = min;
			min = max;
			max = tmp;
		}
		long sleepacc = 0;
		while (!stop) {
			try {
				Thread.sleep(min);
			} catch (InterruptedException e) {
			}
			if (!stop) {
				sleepacc += min;
				manager.filterTimeoutPackage();
				if (sleepacc > max) {
					sleepacc = 0L;
					shardHashManager.filterExpired();
				}
			}
		}
	}
}
