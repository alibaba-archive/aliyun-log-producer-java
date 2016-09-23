package com.aliyun.openservices.log.producer.sample;

import java.util.Date;
import java.util.Random;
import java.util.Vector;

import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.producer.LogProducer;
import com.aliyun.openservices.log.producer.ProducerConfig;
import com.aliyun.openservices.log.producer.ProjectConfig;

public class ProducerSample {
	private final static int ThreadsCount = 4;

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
		System.out.println(System.currentTimeMillis());
		ProducerConfig producerConfig = new ProducerConfig();
		//producerConfig.logsFormat = "json";
		// 使用默认producer配置
		final LogProducer producer = new LogProducer(producerConfig);
		// 添加多个project配置
		producer.setProjectConfig(new ProjectConfig("",
				"", "", ""));
		// 生成日志集合，用于测试
		final Vector<Vector<LogItem>> logGroups = new Vector<Vector<LogItem>>();
		for (int i = 0; i < 100000; ++i) {
			Vector<LogItem> tmpLogGroup = new Vector<LogItem>();
			LogItem logItem = new LogItem((int) (new Date().getTime() / 1000));
			logItem.PushBack("level", "info " + System.currentTimeMillis());
			logItem.PushBack("message", "mmmmmdekdekjdefjekjfek"
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
						producer.send("ali-log-beizhou", "test-producer", "topic",
								"10.101.166.178", logGroups.get(rand),
								new CallbackSample("ali-log-beizhou", "test", "topic", "10.101.166.178", null, logGroups.get(rand), producer));
					}
				}
			}, i + "");
			threads[i].start();
		}
		
		//等待发送线程退出
		Thread.sleep(30 * 1000);
		for (int i = 0; i < ThreadsCount; ++i) {
			threads[i].interrupt();
		}
		//主动刷新缓存起来的还没有被发送的日志
		producer.flush();
		//关闭后台io线程
		producer.close();
	}
}
