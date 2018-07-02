package com.aliyun.openservices.log.producer;

import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.response.PutLogsResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProducerTest {

    private static final String MOCK_IP = "192.168.0.25";

    private static ProjectConfig buildProjectConfig() {
        String projectName = System.getenv("project");
        String endpoint = System.getenv("endpoint");
        String accessKeyId = System.getenv("accessKeyId");
        String accessKey = System.getenv("accessKey");
        return new ProjectConfig(projectName, endpoint, accessKeyId, accessKey);
    }

    private static List<LogItem> getLogItems() {
        List<LogItem> logItems = new ArrayList<LogItem>();

        LogItem logItem1 = new LogItem((int) (new Date().getTime() / 1000));
        logItem1.PushBack("", "val1");
        LogItem logItem2 = new LogItem((int) (new Date().getTime() / 1000));
        logItem2.PushBack("key2", "val2");

        logItems.add(logItem1);
        logItems.add(logItem2);
        return logItems;
    }

    public static void main(String[] args) {
        ProducerConfig producerConfig = new ProducerConfig();
        producerConfig.memPoolSizeInByte = 1024;
        LogProducer producer = new LogProducer(producerConfig);
        producer.setProjectConfig(buildProjectConfig());
        for (int i = 0;i< 1000; ++i) {
            producer.send(System.getenv("project"), "store_1s", "topic1", MOCK_IP, getLogItems(), new TestCallback());
            System.out.println(i);
        }
        producer.flush();
        producer.close();
    }

    static private class TestCallback extends ILogCallback {

        @Override
        public void onCompletion(PutLogsResponse response, LogException e) {
            if (e != null) {
                System.out.println(e.GetErrorCode() + ", " + e.GetErrorMessage()
                        + ", " + e.GetRequestId() + toString());
            }
        }
    }

}
