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
import static org.mockito.Mockito.*;

/**
 * Created by brucewu on 2018/1/12.
 */
public class LogProducerPerformanceTest {

    private static final String MOCK_IP = "192.168.0.25";

    private LogProducer producer;

    @Before
    public void setUp() {
        producer = new LogProducer(new ProducerConfig());
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
    public void testInvokeSendMultiTimes() {
        producer.setProjectConfig(buildProjectConfig());

        TestCallback testCallback = mock(TestCallback.class);
        for (int i = 0; i < 50000; ++i) {
            producer.send(System.getenv("project1"), "store_1s", "topic1", MOCK_IP, getLogItems(),
                    testCallback);
        }
        producer.flush();
        producer.close();

        verify(testCallback, times(50000)).onCompletion(ArgumentMatchers.any(PutLogsResponse.class),
                (LogException) isNull());
    }

}
