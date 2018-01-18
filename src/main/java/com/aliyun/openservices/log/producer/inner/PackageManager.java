package com.aliyun.openservices.log.producer.inner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.aliyun.openservices.log.common.LogContent;
import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.producer.ILogCallback;
import com.aliyun.openservices.log.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageManager.class);

    private ReadWriteLock metaRWLock = new ReentrantReadWriteLock();
    private HashMap<String, PackageMeta> metaMap = new HashMap<String, PackageMeta>();
    private ConcurrentHashMap<String, PackageData> dataMap = new ConcurrentHashMap<String, PackageData>();
    private ProducerConfig config;
    private Semaphore semaphore;
    private IOThread ioThread;
    private ControlThreadPool controlThreadPool;
    private ShardHashManager shardHashManager;

    public PackageManager(ProducerConfig config, ClientPool pool) {
        super();
        this.config = config;
        semaphore = new Semaphore(config.memPoolSizeInByte);
        ioThread = IOThread.launch(pool, this, config);
        shardHashManager = new ShardHashManager(pool, config);
        controlThreadPool = ControlThreadPool.launch(shardHashManager, this, config);
    }

    private static int LogItemListBytes(List<LogItem> logItems) {
        int b = 0;
        for (LogItem it : logItems) {
            b += 4;
            for (LogContent con : it.GetLogContents()) {
                b += con.mKey.length() + con.mValue.length();
            }
        }
        return b;
    }

    void acquireBytes(final int b) {
        semaphore.acquireUninterruptibly(b);
    }

    void releaseBytes(final int b) {
        semaphore.release(b);
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    void filterTimeoutPackage() {
        ArrayList<String> timeoutList = new ArrayList<String>();
        metaRWLock.writeLock().lock();
        for (Entry<String, PackageMeta> entry : metaMap.entrySet()) {
            PackageMeta meta = entry.getValue();
            meta.lock.lock();
            long currTime = System.currentTimeMillis();
            if ((currTime - meta.arriveTimeInMS) >= config.packageTimeoutInMS) {
                PackageData data = dataMap.remove(entry.getKey());
                if (meta.logLinesCount > 0) {
                    ioThread.addPackage(data, meta.packageBytes);
                }
                timeoutList.add(entry.getKey());
            }
            meta.lock.unlock();
        }
        for (String key : timeoutList) {
            metaMap.remove(key);
        }
        metaRWLock.writeLock().unlock();
    }

    public void flush() {
        LOGGER.debug("Try to flush PackageManager.");
        ArrayList<String> timeoutList = new ArrayList<String>();
        metaRWLock.writeLock().lock();
        for (Entry<String, PackageMeta> entry : metaMap.entrySet()) {
            PackageMeta meta = entry.getValue();
            meta.lock.lock();
            PackageData data = dataMap.remove(entry.getKey());
            ioThread.addPackage(data, meta.packageBytes);
            meta.lock.unlock();
            timeoutList.add(entry.getKey());
        }
        for (String key : timeoutList) {
            metaMap.remove(key);
        }
        metaRWLock.writeLock().unlock();
    }

    public void close() {
        LOGGER.info("Try to close PackageManager.");
        controlThreadPool.shutdown();
        ioThread.shutdown();
    }

    public void closeNow() {
        LOGGER.info("Try to close PackageManager immediately.");
        controlThreadPool.shutdownNow();
        ioThread.shutdownNow();
    }

    public void add(String project, String logStore, String topic,
                    String shardHash, String source, List<LogItem> logItems,
                    ILogCallback callback) {
        if (callback != null) {
            callback.callSendBeginTimeInMillis = System.currentTimeMillis();
        }
        if (shardHash != null) {
            shardHash = shardHashManager.getBeginHash(project, logStore,
                    shardHash);
        }
        String key = project + "|" + logStore + "|" + topic + "|" + shardHash + "|" + source;
        int linesCount = logItems.size();
        int logBytes = LogItemListBytes(logItems);

        acquireBytes(logBytes);

        metaRWLock.readLock().lock();
        PackageMeta meta = metaMap.get(key);
        if (meta == null) {
            metaRWLock.readLock().unlock();
            metaRWLock.writeLock().lock();
            meta = metaMap.get(key);
            if (meta == null) {
                meta = new PackageMeta(0, 0);
                metaMap.put(key, meta);
            }
            meta.lock.lock();
            metaRWLock.writeLock().unlock();
        } else {
            meta.lock.lock();
            metaRWLock.readLock().unlock();
        }
        PackageData data = dataMap.get(key);
        if (meta.logLinesCount > 0
                && (meta.logLinesCount + linesCount >= config.logsCountPerPackage
                || meta.packageBytes + logBytes >= config.logsBytesPerPackage || System
                .currentTimeMillis() - meta.arriveTimeInMS >= config.packageTimeoutInMS)) {
            ioThread.addPackage(data, meta.packageBytes);
            dataMap.remove(key);
            data = null;
            meta.clear();
        }

        if (data == null) {
            data = new PackageData(project, logStore, topic, shardHash, source);
            dataMap.put(key, data);
        }
        data.addItems(logItems, callback);
        meta.logLinesCount += linesCount;
        meta.packageBytes += logBytes;
        meta.lock.unlock();
        if (callback != null) {
            callback.callSendEndTimeInMillis = System.currentTimeMillis();
        }
    }
}
