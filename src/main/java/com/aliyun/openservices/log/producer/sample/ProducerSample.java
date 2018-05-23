package com.aliyun.openservices.log.producer.sample;

import java.util.Date;
import java.util.Random;
import java.util.Vector;

import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.producer.LogProducer;
import com.aliyun.openservices.log.producer.ProducerConfig;
import com.aliyun.openservices.log.producer.ProjectConfig;

public class ProducerSample {

	private static final String MOCK_IP = "192.168.0.25";

	private static final int ThreadsCount = 4;

	private static ProjectConfig buildProjectConfig1() {
		String projectName = System.getenv("project1");
		String endpoint = System.getenv("endpoint1");
		String accessKeyId = System.getenv("accessKeyId");
		String accessKey = System.getenv("accessKey");
		return new ProjectConfig(projectName, endpoint, accessKeyId, accessKey);
	}

	private static ProjectConfig buildProjectConfig2() {
		String projectName = System.getenv("project2");
		String endpoint = System.getenv("endpoint2");
		String accessKeyId = System.getenv("accessKeyId");
		String accessKey = System.getenv("accessKey");
		return new ProjectConfig(projectName, endpoint, accessKeyId, accessKey);
	}

	public static String RandomString(int length) {
		String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random random = new Random();
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < length; i++) {
			int num = random.nextInt(62);
			buf.append(str.charAt(num));
		}
		return buf.toString();
	}

	public static void main(String args[]) throws InterruptedException {
		System.out.println(System.currentTimeMillis());
		ProducerConfig producerConfig = new ProducerConfig();
		// 使用默认producer配置
		// 应该使用那种方式来自定义配置
		final LogProducer producer = new LogProducer(producerConfig);
		// 添加多个project配置
		producer.setProjectConfig(buildProjectConfig1());
		producer.setProjectConfig(buildProjectConfig2());
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
						producer.send(System.getenv("project1"), "store_1s", "topic1", MOCK_IP,
								logGroups.get(rand),
								new CallbackSample(System.getenv("project1"), "store_1s",
										"topic1", MOCK_IP, null, logGroups.get(rand), producer));
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
