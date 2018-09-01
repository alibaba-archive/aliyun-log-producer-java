package com.aliyun.openservices.log.producer.inner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.producer.ProducerConfig;
import com.aliyun.openservices.log.producer.ProjectConfig;

public class ClientPool {

    private Map<String, Client> clientPool = new ConcurrentHashMap<String, Client>();
    private ProducerConfig producerConfig;

    public ClientPool(ProducerConfig producerConfig) {
        this.producerConfig = producerConfig;
    }

    public Client updateClient(final ProjectConfig config) {
        Client client = buildClient(config);
        clientPool.put(config.projectName, client);
        return client;
    }

    public void removeClient(final String project) {
        clientPool.remove(project);
    }

    public Client getClient(final String project) {
        return clientPool.get(project);
    }

    private Client buildClient(final ProjectConfig config) {
        Client client = new Client(config.endpoint, config.accessKeyId, config.accessKey);
        client.setUserAgent(producerConfig.userAgent);
        if (config.stsToken != null) {
            client.SetSecurityToken(config.stsToken);
        }
        return client;
    }
}
