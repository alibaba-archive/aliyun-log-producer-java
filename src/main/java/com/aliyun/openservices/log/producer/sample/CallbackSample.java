package com.aliyun.openservices.log.producer.sample;

import java.util.Vector;

import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.producer.ILogCallback;
import com.aliyun.openservices.log.producer.LogProducer;
import com.aliyun.openservices.log.response.PutLogsResponse;

public class CallbackSample extends ILogCallback {
	public String project;
	public String logstore;
	public String topic;
	public String shardHash;
	public String source;
	public Vector<LogItem> items;
	public LogProducer producer;

	public CallbackSample(String project, String logstore, String topic,
			String shardHash, String source, Vector<LogItem> items,
			LogProducer producer) {
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
			System.out.println(e.GetErrorCode() + ", " + e.GetErrorMessage()
					+ ", " + e.GetRequestId() + toString());
		} else {
			
			if ((completeIOEndTimeInMillis - callSendBeginTimeInMillis) > (producer.getProducerConfig().packageTimeoutInMS * 10)) {
				System.out.println("send success, request id: "
						+ response.GetRequestId() + toString());
			}
		}
	}

}
