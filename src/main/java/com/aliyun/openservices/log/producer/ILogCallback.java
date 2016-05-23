package com.aliyun.openservices.log.producer;

import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.response.PutLogsResponse;

public interface ILogCallback
{
	public void onCompletion(PutLogsResponse response, LogException e);
}
