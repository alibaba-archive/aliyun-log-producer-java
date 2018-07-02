package com.aliyun.openservices.log.producer;

import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.response.PutLogsResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LogProducerDeadlockTest {

    private static final String MOCK_IP = "192.168.0.25";

    private LogProducer producer;

    @Before
    public void setUp() {
        ProducerConfig producerConfig = new ProducerConfig();
        producerConfig.memPoolSizeInByte = 1024;
        producer = new LogProducer(producerConfig);
    }

    @After
    public void cleanUp() {
    }

    private ProjectConfig buildProjectConfig() {
        String projectName = System.getenv("project1");
        String endpoint = System.getenv("endpoint1");
        String accessKeyId = System.getenv("accessKeyId");
        String accessKey = System.getenv("accessKey");
        return new ProjectConfig(projectName, endpoint, accessKeyId, accessKey);
    }

    private List<LogItem> getLogItems() {
        List<LogItem> logItems = new ArrayList<LogItem>();

        LogItem logItem = new LogItem((int) (new Date().getTime() / 1000));
        logItem.PushBack("", "val");
        logItems.add(logItem);
        return logItems;
    }

    static private class TestCallback extends ILogCallback {

        @Override
        public void onCompletion(PutLogsResponse response, LogException e) {
        }
    }

    @Test
    public void testSend() {
        producer.setProjectConfig(buildProjectConfig());

        TestCallback testCallback = mock(TestCallback.class);
        for (int i = 0; i < 1000; ++i) {
            producer.send(System.getenv("project1"), "store_1s", "topic1", MOCK_IP, getLogItems(),
                    testCallback);
        }
        producer.flush();
        producer.close();

        verify(testCallback, times(0)).onCompletion(ArgumentMatchers.any(PutLogsResponse.class),
                (LogException) isNull());
    }

}
