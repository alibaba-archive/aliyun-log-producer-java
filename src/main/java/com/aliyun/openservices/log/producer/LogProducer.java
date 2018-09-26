package com.aliyun.openservices.log.producer;

import java.util.List;

import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.producer.inner.ClientPool;
import com.aliyun.openservices.log.producer.inner.PackageManager;

public class LogProducer {
    private ClientPool clientPool;
    private ProducerConfig producerConfig;
    private PackageManager manager;

    public LogProducer(ProducerConfig producerConfig) {
        super();
        this.producerConfig = producerConfig;
        this.clientPool = new ClientPool(producerConfig);
        this.manager = new PackageManager(this.producerConfig, clientPool);
    }

    public void setProjectConfig(ProjectConfig config) {
        clientPool.updateClient(config);
    }

    public void removeProjectConfig(String project) {
        clientPool.removeClient(project);
    }

    public void send(String project, String logStore, String topic, String shardHash, String source, List<LogItem> logItems, ILogCallback callabck) {
        manager.add(project, logStore, topic, shardHash, source, logItems, callabck, false);
    }

    public void send(String project, String logStore, String topic, String shardHash, String source, List<LogItem> logItems) {
        manager.add(project, logStore, topic, shardHash, source, logItems, null, false);
    }

    public void send(String project, String logStore, String topic, String source, List<LogItem> logItems, ILogCallback callabck) {
        manager.add(project, logStore, topic, null, source, logItems, callabck, false);
    }

    public void send(String project, String logStore, String topic, String source, List<LogItem> logItems) {
        manager.add(project, logStore, topic, null, source, logItems, null, false);
    }

    public boolean trySend(String project, String logStore, String topic, String shardHash, String source, List<LogItem> logItems, ILogCallback callabck) {
        return manager.add(project, logStore, topic, shardHash, source, logItems, callabck, true);
    }

    public boolean trySend(String project, String logStore, String topic, String shardHash, String source, List<LogItem> logItems) {
        return manager.add(project, logStore, topic, shardHash, source, logItems, null, true);
    }

    public boolean trySend(String project, String logStore, String topic, String source, List<LogItem> logItems, ILogCallback callabck) {
        return manager.add(project, logStore, topic, null, source, logItems, callabck, true);
    }

    public boolean trySend(String project, String logStore, String topic, String source, List<LogItem> logItems) {
        return manager.add(project, logStore, topic, null, source, logItems, null, true);
    }

    public void flush() {
        manager.flush();
    }

    public void close() {
        manager.close();
    }

    public void closeNow() {
        manager.closeNow();
    }

    public int availablePermits() {
        return manager.availablePermits();
    }

    public ProducerConfig getProducerConfig() {
        return producerConfig;
    }

    public void setProducerConfig(ProducerConfig producerConfig) {
        this.producerConfig = producerConfig;
    }

}
