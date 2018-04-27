# Aliyun LOG Java Producer

[![Build Status](https://travis-ci.org/aliyun/aliyun-log-producer-java.svg?branch=master)](https://travis-ci.org/aliyun/aliyun-log-producer-java)
[![License](https://img.shields.io/badge/license-Apache2.0-blue.svg)](/LICENSE)

[README in English](/README.md)

Aliyun LOG Java Producer 是针对 Java 应用程序编写的高并发写 LogHub 类库，Producer Library 和 [Consumer Library](https://yq.aliyun.com/articles/6817) 是对 LogHub 的读写包装，降低数据收集与消费的门槛。


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

在以上场景中，Aliyun LOG Java Producer 会简化您程序开发的步骤，您无需关心日志采集细节实现、也不用担心日志采集会影响您的业务正常运行，大大降低数据采集门槛。

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
<td>Aliyun LOG Java Producer</td>
<td>不落盘，异步合并发送服务端，吞吐量较好</td>
<td>日志不落盘，客户端 QPS 高</td>
</tr>
</tbody>
</table>

## 配置步骤

Aliyun LOG Java Producer 配置分为以下几个步骤：


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
	<version>0.1.11</version>
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
    //每个缓存的日志包的大小的上限，不能超过3MB，单位是字节
    public int logsBytesPerPackage = 3 * 1024 * 1024;
    //单个producer实例可以使用的内存的上限，单位是字节
    public int memPoolSizeInByte = 100 * 1024 * 1024;
    //当指定以shardhash的方式发送日志时，这个参数需要被设置，否则不需要关心。后端merge线程会将映射到同一个shard的数据merge在一起，而shard关联的是一个hash区间，
    //producer在处理时会将用户传入的hash映射成shard关联hash区间的最小值。每一个shard关联的hash区间，producer会定时从loghub拉取，该参数的含义是每隔shardHashUpdateIntervalInMS毫秒，
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
<td>整数形式，取值为1~3145728（3M），单位为字节。</td>
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

## 权限控制

想要通过 Aliyun LOG Java Producer 向 logstore 中写入数据，需要为使用的账号设置如下权限，设置方法请参考[RAM文档](https://help.aliyun.com/document_detail/57445.html)：
<table>
<thead>
<tr>
<th>Action</th>
<th>Resource</th>
</tr>
</thead>
<tbody>
<tr>
<td>log:PostLogStoreLogs</td>
<td>acs:log:${regionName}:${projectOwnerAliUid}:project/${projectName}/logstore/${logstoreName}</td>
</tr>
</tbody>
</table>

## 错误诊断

如果您发现数据没有写入日志服务，可通过如下步骤进行错误诊断。
* 检查您项目中引入的 protobuf-java，aliyun-log，log-loghub-producer 这三个 jar 包的版本是否和文档中`maven 工程中引入依赖`部分列出的 jar 包版本一致。
* 继承 ILogCallback 类，查看 onCompletion 方法的参数：PutLogsResponse respone，LogException e。后台线程在尝试发送数据后，不论成功或失败，都会回调该方法，通过查看上述参数您可以获得数据发送失败的原因。
```
static private class TestCallback extends ILogCallback {

    @Override
    public void onCompletion(PutLogsResponse response, LogException e) {
        System.out.println(response);
        System.out.println(e);
    }
}
```
* 如果您发现并没有回调 ILogCallback 的 onCompletion 方法，请检查在您的程序退出之前是否有调用 producer.flush() 和 producer.close() 方法。因为数据发送是由后台线程异步完成的，为了防止缓存在内存里的少量数据丢失，建议您在程序退出之前务必调用 producer.flush() 和 producer.close() 方法。
* log-loghub-producer 会把运行过程中的关键行为日志通过 slf4j 进行输出，您可以在程序中配置好相应的日志实现框架，打开 DEBUG 级别的日志，观察 log-loghub-producer。
* 如果通过上述步骤仍然没有解决您的问题请联系我们。

## 常见问题

**Q**: Aliyun LOG Java Producer 依赖 Aliyun LOG Java SDK，而 Aliyun LOG Java SDK 依赖了 2.5.0 版本的 protobuf 库，如果该版本的 protobuf 与用户应用程序中自身带的 protobuf 库冲突怎么办？

**A**: 可以使用 Aliyun LOG Java SDK 提供的一个特殊版本，maven 配置如下：

```
<dependency>
    <groupId>com.aliyun.openservices</groupId>
    <artifactId>aliyun-log</artifactId>
    <version>0.6.12</version>
    <classifier>jar-with-dependencies</classifier>
    <exclusions>
        <exclusion>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.aliyun.openservices</groupId>
    <artifactId>log-loghub-producer</artifactId>
    <version>0.1.10</version>
    <exclusions>
        <exclusion>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
        </exclusion>
        <exclusion>
            <groupId>com.aliyun.openservices</groupId>
            <artifactId>aliyun-log</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**Q**: 使用 producer 向 log service 写数据，PutLogsResponse 返回如下错误信息：error:PostBodyInvalid, upload data time must greater than 0 。

**A**: 某个被发送的 LogItem 的 mLogTime 属性被设置成了0。

**Q**: 如何运行 unit test？

**A**: 编辑文件 `.test_env_rc`，填写 project1、endpoint1 等值，然后运行下列命令

```
source .test_env_rc
mvn clean test
```
**Q**: 如果写入数据的时候不想指定 topic，调用 `send()` 方法时 topic 该如何指？

**A**: 不要将 topic 设成 null，而是设置成 ""。

**Q**: `send()` 接口中 shardHash 的作用？

**A**: 一个 logstore 包含多个 shard。shardHash 决定了这个 logItem 应该位于哪个 shard 上。

https://help.aliyun.com/document_detail/28976.html

**Q**: 一个 producer 实例，会额外创建哪些线程？

**A**: 一个 producer 实例所创建的线程情况如下
<table>
<thead>
<tr>
<th>线程类型</th>
<th>线程名格式</th>
<th>数量</th>
</tr>
</thead>
<tbody>
<tr>
<td>ControlThread</td>
<td>log-producer-control-worker-{N}</td>
<td>2</td>
</tr>
<tr>
<td>IOThread</td>
<td>log-producer-io-thread</td>
<td>1</td>
</tr>
<tr>
<td>IOWorkerThread</td>
<td>log-producer-io-worker-{N}</td>
<td>最多 ProducerConfig.maxIOThreadSizeInPool 个</td>
</tr>
</tbody>
</table>

**Q**: 一个进程需要创建多少个 producer？

**A**: producer 的方法是线程安全的，一般情况一个进程的所有线程共用一个 producer。

**Q**：写数据时，服务端返回如下错误 aliyun.openservices.log.exception.LogException: Write quota exceed: qps: 517？

**A**：超过了 project 每秒限制的写入次数，说明您的一个 package 所包含的 logItem 过少，检查 packageTimeoutInMS、 logsCountPerPackage 等参数是否设置过低。

**Q**：手动分裂 shard 后，原来 shard 变成只读了，需要重启写入程序吗？

**A**：不需要。服务端会自动将待写入的数据路由到新分裂出来的两个可写的 shard 上。

**Q**：producer 会缓存待发送的数据，并将数据合并成 package 后批量发往服务端。什么样的数据有机会合并在相同的 package 里？

**A**：有相同 project，logStore，topic，shardHash，source 的数据会被合并在一起。

## Aliyun LOG Java SDK
若 producer 提供的接口满足不了您的日志采集需求，您可以基于 [Aliyun Log Java SDK](https://github.com/aliyun/aliyun-log-java-sdk)，开发适合您的应用场景的日志采集API。

## 联系我们
- [阿里云LOG官方网站](https://www.aliyun.com/product/sls/)
- [阿里云LOG官方论坛](https://yq.aliyun.com/groups/50)
- 阿里云官方技术支持：[提交工单](https://workorder.console.aliyun.com/#/ticket/createIndex)

## 贡献者
[@zzboy](https://github.com/zzboy)对项目作了很大贡献。

感谢[@zzboy](https://github.com/zzboy)的杰出工作。
