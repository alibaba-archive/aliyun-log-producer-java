# Aliyun LOG Java Producer

[![Build Status](https://travis-ci.org/aliyun/aliyun-log-producer-java.svg?branch=master)](https://travis-ci.org/aliyun/aliyun-log-producer-java)
[![License](https://img.shields.io/badge/license-Apache2.0-blue.svg)](/LICENSE)

[中文版README](/README_CN.md)

The `Aliyun LOG Java Producer` is a high concurrency writing library written for Java applications. The Producer Library and [Consumer Library](https://yq.aliyun.com/articles/6817) is a reading and writing encapsulation for Loghub. It aims to reduce the threshold for data collection and consumption.

## Feature

* Provide an asynchronous sending interface, thread safety.
* You can configure more than one project.
* You can control the thread number for sending data.
* You can control the number and size of logs being merged into one package.
* You can control the memory usage. When the memory usage reach the threshold configured by user, the producer's send interface will be blocked until the free memory is available.

## Advantage

* `Disk Free`: the generation data will be send to AliCloud Log Service in real time through network.
* `High Throughput`: support more than 100 write operations per second.
* `The Computing and IO is separated`: recoding logs do not affect computing time.

In the above scenarios, the `Aliyun LOG Java Producer` will alleviate the burden of your development. You don't need to care about log collection implementation details. And there is no need to worry about that log collection will impact on the normal operation of your business. Reducing the threshold of data acquisition greatly.

The comparison of all kinds of access methods
<table>
<thead>
<tr>
<th>Access Mode</th>
<th>Pros/Cons</th>
<th>Use Case</th>
</tr>
</thead>
<tbody>
<tr>
<td>log file + Logtail</td>
<td>Log writing
 and log collection decoupling, there is no need to modify your code.</td>
<td>common case</td>
</tr>
<tr>
<td><a href="https://yq.aliyun.com/articles/17129">syslog</a> + Logtail</td>
<td>Good performance (80MB/S), disk free, the syslog protocol needs to be supported.</td>
<td>syslog</td>
</tr>
<tr>
<td><a href="https://yq.aliyun.com/articles/17129">Aliyun Log Java SDK</a></td>
<td>Disk free, the data will be send to AliCloud Log Service directly, you need to care about the switch of network IO and program IO.</td>
<td>The log will not be written to a file</td>
</tr>
<tr>
<td>Aliyun LOG Java Producer</td>
<td>Disk free, asynchronous and high throughput.</td>
<td>The log will not be written to a file, high QPS.</td>
</tr>
</tbody>
</table>

## Configuration Steps

### 1. Adding the Dependencies in pom.xml

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

### 2. Configure ProducerConfig

The configuration format is as follows. Please refer to the parameter description part for parameter value.
```
public class ProducerConfig {
    // Specify the timeout for sending package, in milliseconds, default is 3000
    public int packageTimeoutInMS = 3000;
    // Specify the maximum log count per package, the upper limit is 4096
    public int logsCountPerPackage = 4096;
    // Specify the maximum cache size per package, the upper limit is 3MB, in bytes
    public int logsBytesPerPackage = 3 * 1024 * 1024;
    // The upper limit of the memory that can be used by each producer instance, in bytes, default is 100MB
    public int memPoolSizeInByte = 100 * 1024 * 1024;
    // If shardHash is specified when you send data, you should care about this parameter, otherwise there is no need to care about it.
    // The backend thread will merge the data being sent to the same shard together, and shard is associated with a hash interval.
    // The producer will pull the hash interval information for each shard from AliCloud Log Service regularly and update the local value, this parameter stands for the time interval.
    public int shardHashUpdateIntervalInMS = 10 * 60 * 1000;
    // Specify the retry times when failing to send data, if exceeds this value, the exception will be used as a parameter for the callback, default is 3
    public int retryTimes = 3;
    // protobuf
    public String logsFormat = "protobuf";
    // Specify the I/O thread pool's maximum pool size, the main function of the I/O thread pool is to send data, default is 8
    public int maxIOThreadSizeInPool = 8;
    // userAgent
    public String userAgent = "loghub-producer-java";
}
```

### 3. Extends ILogCallback

The main function of callback is to handle the results of sending data. The results include successful status or exception. If you don't care about the result, there is no need to extends ILogCallback.

## Parameter Description

<table>
<thead>
<tr>
<th>Parameter</th>
<th>Description</th>
<th>Value</th>
</tr>
</thead>
<tbody>
<tr>
<td>packageTimeoutInMS</td>
<td>Specify the timeout for sending package.</td>
<td>Integer, in milliseconds</td>
</tr>
<tr>
<td>logsCountPerPackage</td>
<td>Specify the maximum log count per package.</td>
<td>Integer, 1~4096</td>
</tr>
<tr>
<td>logsBytesPerPackage</td>
<td>Specify the maximum cache size per package.</td>
<td>Integer, 1~3145728(3M),in bytes</td>
</tr>
<tr>
<td>memPoolSizeInByte</td>
<td>The upper limit of the memory that can be used by each producer instance.</td>
<td>Integer, in bytes</td>
</tr>
<tr>
<td>maxIOThreadSizeInPool</td>
<td>Specify the I/O thread pool's maximum pool size, the main function of the I/O thread pool is to send data.</td>
<td>Integer, default is 8</td>
</tr>
<tr>
<td>shardHashUpdateIntervalInMS</td>
<td>If shardHash is specified when you send data, you should care about this parameter, otherwise there is no need to care about it. The backend thread will merge the data being sent to the same shard together, and shard is associated with a hash interval. The producer will pull the hash interval information for each shard from AliCloud Log Service regularly and update the local value, this parameter stands for the time interval.</td>
<td>Integer, in milliseconds</td>
</tr>
<tr>
<td>retryTimes</td>
<td>Specify the retry times when failing to send data, if exceeds this value, the exception will be used as a parameter for the callback.</td>
<td>Integer, default is 3</td>
</tr>
</tbody>
</table>

## Sample Code

[ProducerSample.java](/src/main/java/com/aliyun/openservices/log/producer/sample/ProducerSample.java)

[CallbackSample.java](/src/main/java/com/aliyun/openservices/log/producer/sample/CallbackSample.java)

## RAM

If you want to write data to logstore through Aliyun LOG Java Producer, you should configure the following permissions for the account you use. [RAM doc](https://www.alibabacloud.com/help/doc-detail/57445.htm)
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

## Aliyun Log Java SDK
If the interface provided by producer can't meet your log collection requirements, you can develop your own log collection API based on [Aliyun Log Java SDK](https://github.com/aliyun/aliyun-log-java-sdk).

## Contact Us
- [Alicloud Log Service homepage](https://www.alibabacloud.com/product/log-service)
- [Alicloud Log Service doc](https://www.alibabacloud.com/help/product/28958.htm)
- [Alicloud Log Servic official forum](https://yq.aliyun.com/groups/50)
- Alicloud Log Servic official technical support: [submit tickets](https://workorder.console.aliyun.com/#/ticket/createIndex)

## Contributors
[@zzboy](https://github.com/zzboy) made a great contribution to this project.

Thanks for the excellent work by [@zzboy](https://github.com/zzboy)
