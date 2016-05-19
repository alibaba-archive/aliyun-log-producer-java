package com.alibaba.openservices.log.producer.sample;

import java.util.Date;
import java.util.Random;
import java.util.Vector;

import com.alibaba.openservices.log.producer.LogProducer;
import com.alibaba.openservices.log.producer.ProducerConfig;
import com.alibaba.openservices.log.producer.ProjectConfig;
import com.aliyun.openservices.log.common.LogItem;

public class ProducerSample {
	private final static int ThreadsCount = 25;

	public static String RandomString(int length) {
		String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int num = random.nextInt(62);
			buf.append(str.charAt(num));
		}
		return buf.toString();
	}

	public static void main(String args[]) throws InterruptedException {
		ProducerConfig producerConfig = new ProducerConfig();
		// 使用默认producer配置
		final LogProducer producer = new LogProducer(producerConfig);
		// 添加多个project配置
		producer.updateProjectConfig(new ProjectConfig("your project 1",
				"endpoint", "your accesskey id", "your accesskey"));
		producer.updateProjectConfig(new ProjectConfig("your project 2",
				"endpoint", "your accesskey id", "your accesskey",
				"your sts token"));
		// 更新project 1的配置
		producer.updateProjectConfig(new ProjectConfig("your project 1",
				"endpoint", "your new accesskey id", "your new accesskey"));
		// 删除project 2的配置
		producer.removeProjectConfig("your project 2");
		// 生成日志集合，用于测试
		final Vector<Vector<LogItem>> logGroups = new Vector<Vector<LogItem>>();
		for (int i = 0; i < 100000; ++i) {
			Vector<LogItem> tmpLogGroup = new Vector<LogItem>();
			LogItem logItem = new LogItem((int) (new Date().getTime() / 1000));
			logItem.PushBack("level", "info" + System.currentTimeMillis());
			logItem.PushBack("message", "test producer send perf "
					+ RandomString(50));
			logItem.PushBack("method", "SenderToServer " + RandomString(10));
			tmpLogGroup.add(logItem);
			logGroups.add(tmpLogGroup);
		}
		// 并发调用send发送日志
		Thread[] threads = new Thread[ThreadsCount];
		for (int i = 0; i < ThreadsCount; ++i) {
			threads[i] = new Thread(null, new Runnable() {
				Random random = new Random();

				public void run() {
					int j = 0, rand = random.nextInt(99999);
					while (++j < Integer.MAX_VALUE) {
						producer.send("project 1", "logstore 1", "topic",
								"source ip", logGroups.get(rand),
								new CallbackSample("project 1", "logstore 1", "topic", "source ip", null, logGroups.get(rand), producer));
					}
				}
			}, i + "");
			threads[i].start();
		}
		//主动刷新缓存起来的还没有被发送的日志
		producer.flush();
		//关闭后台io线程
		producer.close();
	}
}
