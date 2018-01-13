package com.aliyun.openservices.log.producer;

import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.response.PutLogsResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by brucewu on 2018/1/12.
 */
public class LogProducerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogProducerTest.class);

    private static final String MOCK_IP = "192.168.0.25";

    private LogProducer producer;

    @Before
    public void setUp() {
        producer = new LogProducer(new ProducerConfig());
    }

    @After
    public void cleanUp() {
        System.out.println("here");
        producer.close();
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
            LOGGER.info("onCompletion, e=" + e);
            assertNull(e);
        }
    }

    @Test
    public void testSendToMultiProjects() {
        producer.setProjectConfig(buildProjectConfig1());
        producer.setProjectConfig(buildProjectConfig2());
        producer.send(System.getenv("project1"), "store_1s", "topic1", MOCK_IP, getLogItems(),
                new TestCallback());
        producer.send(System.getenv("project2"), "store_1s", "topic2", MOCK_IP, getLogItems(),
                new TestCallback());
        producer.flush();
    }

    @Test
    public void testSendToMultiShardLogStore() {
        producer.setProjectConfig(buildProjectConfig1());
        producer.send(System.getenv("project1"), "store_3s", "topic1", MOCK_IP, getLogItems(),
                new TestCallback());
        producer.flush();
    }

}
