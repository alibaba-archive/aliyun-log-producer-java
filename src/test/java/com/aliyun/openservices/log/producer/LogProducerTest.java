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

/**
 * Created by brucewu on 2018/1/12.
 */
public class LogProducerTest {

    private static final String MOCK_IP = "192.168.0.25";

    private LogProducer producer;

    @Before
    public void setUp() {
        producer = new LogProducer(new ProducerConfig());
    }

    @After
    public void cleanUp() {
    }

    private ProjectConfig buildProjectConfig1() {
        String projectName = System.getenv("project1");
        String endpoint = System.getenv("endpoint1");
        String accessKeyId = System.getenv("accessKeyId");
        String accessKey = System.getenv("accessKey");
        return new ProjectConfig(projectName, endpoint, accessKeyId, accessKey);
    }

    private ProjectConfig buildProjectConfig2() {
        String projectName = System.getenv("project2");
        String endpoint = System.getenv("endpoint2");
        String accessKeyId = System.getenv("accessKeyId");
        String accessKey = System.getenv("accessKey");
        return new ProjectConfig(projectName, endpoint, accessKeyId, accessKey);
    }

    private List<LogItem> getLogItems() {
        List<LogItem> logItems = new ArrayList<LogItem>();

        LogItem logItem1 = new LogItem((int) (new Date().getTime() / 1000));
        logItem1.PushBack("key1", "val1");
        LogItem logItem2 = new LogItem((int) (new Date().getTime() / 1000));
        logItem2.PushBack("key2", "val2");

        logItems.add(logItem1);
        logItems.add(logItem2);
        return logItems;
    }

    static private class TestCallback extends ILogCallback {

        @Override
        public void onCompletion(PutLogsResponse response, LogException e) {
        }
    }

    @Test
    public void testSendToMultiProjects() {
        producer.setProjectConfig(buildProjectConfig1());
        producer.setProjectConfig(buildProjectConfig2());

        TestCallback testCallback1 = mock(TestCallback.class);
        TestCallback testCallback2 = mock(TestCallback.class);
        producer.send(System.getenv("project1"), "store_1s", "topic1", MOCK_IP, getLogItems(),
                testCallback1);
        producer.send(System.getenv("project2"), "store_1s", "topic2", MOCK_IP, getLogItems(),
                testCallback2);
        producer.flush();
        producer.close();

        verify(testCallback1, times(1)).onCompletion(ArgumentMatchers.any(PutLogsResponse.class),
                (LogException) isNull());
        verify(testCallback2, times(1)).onCompletion(ArgumentMatchers.any(PutLogsResponse.class),
                (LogException) isNull());
    }

    @Test
    public void testSendToMultiShardLogStore() {
        producer.setProjectConfig(buildProjectConfig1());

        TestCallback testCallback = mock(TestCallback.class);
        producer.send(
                System.getenv("project1"),
                "store_3s",
                "topic1",
                "55000000000000000000000000000000",
                MOCK_IP,
                getLogItems(),
                testCallback);
        producer.send(
                System.getenv("project1"),
                "store_3s",
                "topic2",
                "55000000000000000000000000000001",
                MOCK_IP,
                getLogItems(),
                testCallback);
        producer.send(
                System.getenv("project1"),
                "store_3s",
                "topic3",
                "aa000000000000000000000000000000",
                MOCK_IP,
                getLogItems(),
                testCallback);
        producer.send(
                System.getenv("project1"),
                "store_3s",
                "topic4",
                "aa000000000000000000000000000001",
                MOCK_IP,
                getLogItems(),
                testCallback);
        producer.send(
                System.getenv("project1"),
                "store_3s",
                "topic5",
                "fffffffffffffffffffffffffffffffe",
                MOCK_IP,
                getLogItems(),
                testCallback);

        producer.flush();
        producer.close();

        verify(testCallback, times(5)).onCompletion(ArgumentMatchers.any(PutLogsResponse.class),
                (LogException) isNull());
    }

}
