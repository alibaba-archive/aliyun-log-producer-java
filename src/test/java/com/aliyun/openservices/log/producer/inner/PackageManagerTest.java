package com.aliyun.openservices.log.producer.inner;

import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.producer.ProducerConfig;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

public class PackageManagerTest {

    @Test
    public void testSplitLogItems_10_21() {
        ProducerConfig producerConfig = new ProducerConfig();
        producerConfig.logsCountPerPackage = 10;
        PackageManager packageManager = new PackageManager(producerConfig, new ClientPool(producerConfig));
        List<LogItem> srcLogItems = new ArrayList<LogItem>();
        for (int i = 0; i < 21; ++i) {
            LogItem logItem = new LogItem();
            srcLogItems.add(logItem);
        }
        List<List<LogItem>> sepLogItems = packageManager.splitLogItems(srcLogItems);
        assertEquals(3, sepLogItems.size());
        assertEquals(producerConfig.logsCountPerPackage, sepLogItems.get(0).size());
        assertEquals(producerConfig.logsCountPerPackage, sepLogItems.get(1).size());
        assertEquals(1, sepLogItems.get(2).size());
    }

    @Test
    public void testSplitLogItems_10_30() {
        ProducerConfig producerConfig = new ProducerConfig();
        producerConfig.logsCountPerPackage = 10;
        PackageManager packageManager = new PackageManager(producerConfig, new ClientPool(producerConfig));
        List<LogItem> srcLogItems = new ArrayList<LogItem>();
        for (int i = 0; i < 30; ++i) {
            LogItem logItem = new LogItem();
            srcLogItems.add(logItem);
        }
        List<List<LogItem>> sepLogItems = packageManager.splitLogItems(srcLogItems);
        assertEquals(3, sepLogItems.size());
        assertEquals(producerConfig.logsCountPerPackage, sepLogItems.get(0).size());
        assertEquals(producerConfig.logsCountPerPackage, sepLogItems.get(1).size());
        assertEquals(producerConfig.logsCountPerPackage, sepLogItems.get(2).size());
    }

    @Test
    public void testSplitLogItems_10_0() {
        ProducerConfig producerConfig = new ProducerConfig();
        producerConfig.logsCountPerPackage = 10;
        PackageManager packageManager = new PackageManager(producerConfig, new ClientPool(producerConfig));
        List<LogItem> srcLogItems = new ArrayList<LogItem>();
        List<List<LogItem>> sepLogItems = packageManager.splitLogItems(srcLogItems);
        assertEquals(0, sepLogItems.size());
    }

    @Test
    public void testSplitLogItems_default_4096() {
        ProducerConfig producerConfig = new ProducerConfig();
        PackageManager packageManager = new PackageManager(producerConfig, new ClientPool(producerConfig));
        List<LogItem> srcLogItems = new ArrayList<LogItem>();
        for (int i = 0; i < 4096; ++i) {
            LogItem logItem = new LogItem();
            srcLogItems.add(logItem);
        }
        List<List<LogItem>> sepLogItems = packageManager.splitLogItems(srcLogItems);
        assertEquals(1, sepLogItems.size());
        assertEquals(producerConfig.logsCountPerPackage, sepLogItems.get(0).size());
    }
}
