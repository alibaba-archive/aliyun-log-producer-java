package com.aliyun.openservices.log.producer.inner;

import java.util.HashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.producer.ProducerConfig;
import com.aliyun.openservices.log.producer.ProjectConfig;

public class ClientPool {
    class Pair {
        public Client client;
        public int ref;

        public Pair(Client client, int ref) {
            super();
            this.client = client;
            this.ref = ref;
        }
    }

    private HashMap<String, String> projectEndpointMap = new HashMap<String, String>();
    private HashMap<String, Pair> clientPool = new HashMap<String, Pair>();
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private ProducerConfig producerConfig;

    public ClientPool(ProducerConfig producerConfig) {
        this.producerConfig = producerConfig;
    }

    public Client updateClient(final ProjectConfig config) {
        rwLock.writeLock().lock();
        try {
            String ep = projectEndpointMap.get(config.projectName);
            int ref = 0;
            if (ep == null) {
                ref = 1;
                projectEndpointMap.put(config.projectName, config.endpoint);
                ep = config.endpoint;
            }
            Pair p = clientPool.get(ep);
            Client client;
            if (p != null) {
                client = p.client;
                p.ref += ref;
            } else {
                client = buildClient(config);
                clientPool.put(ep, new Pair(client, ref));
            }
            return client;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void removeClient(final String project) {
        rwLock.writeLock().lock();
        try {
            String ep = projectEndpointMap.get(project);
            if (ep != null) {
                projectEndpointMap.remove(project);
                Pair p = clientPool.get(ep);
                if (--p.ref == 0) {
                    clientPool.remove(ep);
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Client getClient(final String project) {
        rwLock.readLock().lock();
        try {
            String ep = projectEndpointMap.get(project);
            Client c = null;
            if (ep != null) {
                c = clientPool.get(ep).client;
            }
            return c;
        } finally {
            rwLock.readLock().unlock();
        }
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
