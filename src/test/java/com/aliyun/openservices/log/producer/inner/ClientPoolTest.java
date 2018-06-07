package com.aliyun.openservices.log.producer.inner;

import com.aliyun.openservices.log.producer.ProducerConfig;
import com.aliyun.openservices.log.producer.ProjectConfig;
import org.junit.Test;

public class ClientPoolTest {

    private ProjectConfig buildProjectConfig1() {
        String projectName = "project1";
        String endpoint = "endpoint";
        String accessKeyId = "accessKeyId";
        String accessKey = "accessKey";
        return new ProjectConfig(projectName, endpoint, accessKeyId, accessKey);
    }

    private ProjectConfig buildProjectConfig2() {
        String projectName = "project2";
        String endpoint = "endpoint";
        String accessKeyId = "accessKeyId";
        String accessKey = "accessKey";
        return new ProjectConfig(projectName, endpoint, accessKeyId, accessKey);
    }

    private ProjectConfig buildProjectConfig3() {
        String projectName = "project3";
        String endpoint = "endpoint";
        String accessKeyId = "accessKeyId";
        String accessKey = "accessKey";
        return new ProjectConfig(projectName, endpoint, accessKeyId, accessKey);
    }

    @Test
    public void testUpdateClient() {
        ClientPool clientPool = new ClientPool(new ProducerConfig());
        clientPool.updateClient(buildProjectConfig1());
        clientPool.updateClient(buildProjectConfig2());
        clientPool.updateClient(buildProjectConfig3());
    }

    @Test
    public void testRemoveClient() {
        ClientPool clientPool = new ClientPool(new ProducerConfig());
        clientPool.removeClient("project1");
    }

}
