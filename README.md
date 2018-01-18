# Aliyun LOG Java Producer

日志服务 Java Producer Library 是针对 Java 应用程序编写的高并发写LogHub类库，Producer Library 和 [Consumer Library](https://yq.aliyun.com/articles/6817) 是对LogHub的读写包装，降低数据收集与消费的门槛。


## 功能特点

* 提供异步的发送接口，线程安全。
* 可以添加多个Project的配置。
* 用于发送的网络 I/O 线程数量可以配置。
* merge成的包的日志数量以及大小都可以配置。
* 内存使用可控，当内存使用达到用户配置的阈值时，Producer 的 send 接口会阻塞，直到有空闲的内存可用。

## 功能优势

* 客户端日志不落盘：既数据产生后直接通过网络发往服务端。
* 客户端高并发写入：例如一秒钟会有百次以上写操作。
* 客户端计算与 I/O 逻辑分离：打印日志不影响计算耗时。

在以上场景中，Java Producer Library 会简化您程序开发的步骤，您无需关心日志采集细节实现、也不用担心日志采集会影响您的业务正常运行，大大降低数据采集门槛。

![image.png](/pics/producer.png)

以上各种接入方式的对比：
<table>
<thead>
<tr>
<th>接入方式</th>
<th>优点/缺点</th>
<th>针对场景</th>
</tr>
</thead>
<tbody>
<tr>
<td>日志落盘 + Logtail</td>
<td>日志收集与打日志解耦，无需修改代码</td>
<td>常用场景</td>
</tr>
<tr>
<td><a href="https://yq.aliyun.com/articles/17129">syslog</a> + Logtail</td>
<td>性能较好（80MB/S），日志不落盘，需支持 syslog 协议</td>
<td>syslog 场景</td>
</tr>
<tr>
<td>SDK 直发</td>
<td>不落盘，直接发往服务端，需要处理好网络 IO 与程序 IO 之间的切换</td>
<td>日志不落盘</td>
</tr>
<tr>
<td>Producer Library</td>
<td>不落盘，异步合并发送服务端，吞吐量较好</td>
<td>日志不落盘，客户端 QPS 高</td>
</tr>
</tbody>
</table>

## 配置步骤

Java Producer Library 配置分为以下几个步骤：


### 1. maven 工程中引入依赖

```
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
<dependency>
	<groupId>com.aliyun.openservices</groupId>
	<artifactId>log-loghub-producer</artifactId>
	<version>0.1.9</version>
</dependency>
```

### 2. 程序中配置 ProducerConfig

配置格式如下，参数取值见本文档中参数取值部分。

```
public class ProducerConfig {
    //被缓存起来的日志的发送超时时间，如果缓存超时，则会被立即发送，单位是毫秒
    public int packageTimeoutInMS = 3000;
    //每个缓存的日志包中包含日志数量的最大值，不能超过4096
    public int logsCountPerPackage = 4096;
    //每个缓存的日志包的大小的上限，不能超过5MB，单位是字节
    public int logsBytesPerPackage = 3 * 1024 * 1024;
    //单个producer实例可以使用的内存的上限，单位是字节
    public int memPoolSizeInByte = 100 * 1024 * 1024;
    //当使用指定shardhash的方式发送日志时，这个参数需要被设置，否则不需要关心。后端merge线程会将映射到同一个shard的数据merge在一起，而shard关联的是一个hash区间，
    //producer在处理时会将用户传入的hash映射成shard关联hash区间的最小值。每一个shard关联的hash区间，producer会定时从从loghub拉取，该参数的含义是每隔shardHashUpdateIntervalInMS毫秒，
    //更新一次shard的hash区间。
    public int shardHashUpdateIntervalInMS = 10 * 60 * 1000;
    //如果发送失败，重试的次数，如果超过该值，就会将异常作为callback的参数，交由用户处理。
    public int retryTimes = 3;
    //protobuf
    public String logsFormat = "protobuf";
    //IO线程池最大线程数量
    public int maxIOThreadSizeInPool = 8;
    //userAgent
    public String userAgent = "loghub-producer-java";
}
```

### 3. 继承 ILogCallback

使用 callback 主要为了处理日志发送的结果，结果包括发送成功和发生异常。您也可以选择不处理，这样就不需要继承 ILogCallback。

## 参数说明

<table>
<thead>
<tr>
<th>参数</th>
<th>参数说明</th>
<th>取值</th>
</tr>
</thead>
<tbody>
<tr>
<td>packageTimeoutInMS</td>
<td>指定被缓存日志的发送超时时间，如果缓存超时，则会被立即发送。</td>
<td>整数形式，单位为毫秒。</td>
</tr>
<tr>
<td>logsCountPerPackage</td>
<td>指定每个缓存的日志包中包含日志数量的最大值。</td>
<td>整数形式，取值为1~4096。</td>
</tr>
<tr>
<td>logsBytesPerPackage</td>
<td>指定每个缓存的日志包的大小上限。</td>
<td>整数形式，取值为1~3145728 ，单位为字节。</td>
</tr>
<tr>
<td>memPoolSizeInByte</td>
<td>指定单个Producer实例可以使用的内存的上限。</td>
<td>整数形式，单位为字节。</td>
</tr>
<tr>
<td>maxIOThreadSizeInPool</td>
<td>指定I/O线程池最大线程数量，主要用于发送数据到日志服务。</td>
<td>整数形式。</td>
</tr>
<tr>
<td>shardHashUpdateIntervalInMS</td>
<td>指定更新Shard的Hash区间的时间间隔，当指定shardhash的方式发送日志时，需要设置此参数。<br>后端merge线程会将映射到同一个Shard的数据merge在一起，而Shard关联的是一个Hash区间，Producer在处理时会将用户传入的Hash映射成Shard关联Hash区间的最小值。每一个Shard关联的Hash区间，Producer会定时从LogHub拉取。</td>
<td>整数形式。</td>
</tr>
<tr>
<td>retryTimes</td>
<td>指定发送失败时重试的次数，如果超过该值，就会将异常作为callback的参数，交由用户处理。</td>
<td>整数形式。</td>
</tr>
</tbody>
</table>

## 使用实例

项目中提供了名为`ProducerSample`和`CallbackSample`的实例。

[ProducerSample.java](/src/main/java/com/aliyun/openservices/log/producer/sample/ProducerSample.java)

[CallbackSample.java](/src/main/java/com/aliyun/openservices/log/producer/sample/CallbackSample.java)


## 底层日志发送接口
若 producer 提供的接口满足不了您的日志采集需求，您可以基于底层的[日志发送接口](https://github.com/aliyun/aliyun-log-java-sdk)，开发适合您的应用场景的日志采集API。

## 联系我们
- [阿里云LOG官方网站](https://www.aliyun.com/product/sls/)
- [阿里云LOG官方论坛](https://yq.aliyun.com/groups/50)
- 阿里云官方技术支持：[提交工单](https://workorder.console.aliyun.com/#/ticket/createIndex)