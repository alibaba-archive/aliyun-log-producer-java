## 要解决的问题
1. 系统每次只产生一条日志，如果每条日志都调用log sdk发送，对网络IO的使用效率很低。
2. 少量日志每次都发送，这样就会有大量的tcp短链接，很容易耗尽系统端口。

log producer解决上面问题的方法是会将日志merge到一定数量才真正发送，具体数量由根据用户配置的参数决定。
## log producer功能
1. 提供异步的发送接口，线程安全。
2. 可以添加多个project的配置。
3. 用于发送的网络IO线程数量可以配置。
4. merge成的包的日志数量以及大小都可以配置。
5. 内存使用可控，当内存使用达到用户配置的阈值时，producer的send接口会阻塞，直到有空闲的内存可用。

## 使用方法
producer使用分为以下几个步骤：

step 1:
maven工程中添加依赖：
```
<dependency>
    <groupId>com.aliyun.openservices</groupId>
    <artifactId>log-loghub-producer</artifactId>
    <version>0.1.9</version>
</dependency>
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>2.5.0</version>
</dependency>
<dependency>
    <groupId>com.aliyun.openservices</groupId>
    <artifactId>aliyun-log</artifactId>
    <version>0.6.10</version>
</dependency>
```
step 2：程序中配置ProducerConfig，其中各个参数说明如下。
```java
public class ProducerConfig 
{
	//被缓存起来的日志的发送超时时间，如果缓存超时，则会被立即发送，单位是毫秒
	public int packageTimeoutInMS = 50000;
	//每个缓存的日志包中包含日志数量的最大值，不能超过4096
	public int logsCountPerPackage = 4096;
	//每个缓存的日志包的大小的上限，不能超过5MB，单位是字节
	public int logsBytesPerPackage = 5 * 1024 * 1024;
	//单个producer实例可以使用的内存的上限，单位是字节
	public int memPoolSizeInByte = 1000 * 1024 * 1024;
	//后台用于发送日志包的IO线程的数量
	public int ioThreadsCount = 1;
	//当使用指定shardhash的方式发送日志时，这个参数需要被设置，否则不需要关心。后端merge线程会将映射到同一个shard的数据merge在一起，而shard关联的是一个hash区间，
	//producer在处理时会将用户传入的hash映射成shard关联hash区间的最小值。每一个shard关联的hash区间，producer会定时从从loghub拉取，该参数的含义是每隔shardHashUpdateIntervalInMS毫秒，
	//更新一次shard的hash区间。
	public int shardHashUpdateIntervalInMS = 10 * 60 * 1000;
	//如果发送失败，重试的次数，如果超过该值，就会将异常作为callback的参数，交由用户处理。
	public int retryTimes = 3;
}
```
step 3：继承ILogCallback，callback主要用于日志发送结果的处理，结果包括发送成功和发生异常。用户也可以选择不处理，这样就不需要继承ILogCallback。

step 4：创建producer实例，调用send接口发数据。

下面是一个完整的示例。
### 示例
main:
```java
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
		producer.setProjectConfig(new ProjectConfig("your project 1",
				"endpoint", "your accesskey id", "your accesskey"));
		producer.setProjectConfig(new ProjectConfig("your project 2",
				"endpoint", "your accesskey id", "your accesskey",
				"your sts token"));
		// 更新project 1的配置
		producer.setProjectConfig(new ProjectConfig("your project 1",
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
		//等待发送线程退出
		Thread.sleep(1 * 60 * 60 * 1000);
		//主动刷新缓存起来的还没有被发送的日志
		producer.flush();
		//关闭后台io线程
		producer.close();
	}
}
```
callback:
```java
public class CallbackSample implements ILogCallback {
    //保存要发送的数据，当时发生异常时，进行重试
	public String project;
	public String logstore;
	public String topic;
	public String shardHash;
	public String source;
	public Vector<LogItem> items;
	public LogProducer producer;
	public int retryTimes = 0;
	public CallbackSample(String project, String logstore, String topic,
			String shardHash, String source, Vector<LogItem> items, LogProducer producer) {
		super();
		this.project = project;
		this.logstore = logstore;
		this.topic = topic;
		this.shardHash = shardHash;
		this.source = source;
		this.items = items;
		this.producer = producer;
	}

	public void onCompletion(PutLogsResponse response, LogException e) {
		if (e != null) {
		    // 打印异常
			System.out.println(e.GetErrorCode() + ", " + e.GetErrorMessage() + ", " + e.GetRequestId());
			//最多重试三次
			if(retryTimes++ < 3)
			{
				producer.send(project, logstore, topic, source, shardHash, items, this);
			}
		}
		else{
			System.out.println("send success, request id: " + response.GetRequestId());
		}
	}

}
```