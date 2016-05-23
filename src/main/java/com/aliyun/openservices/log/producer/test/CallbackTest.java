package com.aliyun.openservices.log.producer.test;

import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.producer.ILogCallback;
import com.aliyun.openservices.log.response.PutLogsResponse;

public class CallbackTest implements ILogCallback {

	public void onCompletion(PutLogsResponse response, LogException e) {
		if (e != null) {
			System.out.println(e.GetErrorCode() + ", " + e.GetErrorMessage());
		}
	}

}
