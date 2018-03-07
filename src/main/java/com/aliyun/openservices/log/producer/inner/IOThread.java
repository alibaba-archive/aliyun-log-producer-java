package com.aliyun.openservices.log.producer.inner;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.Consts;
import com.aliyun.openservices.log.common.TagContent;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.producer.ProducerConfig;
import com.aliyun.openservices.log.request.PutLogsRequest;
import com.aliyun.openservices.log.response.PutLogsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BlockedData {
    PackageData data;
    int bytes;

    BlockedData(PackageData data, int bytes) {
        super();
        this.data = data;
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return "BlockedData{" +
                "data=" + data +
                ", bytes=" + bytes +
                '}';
    }
};

class IOThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOThread.class);

    private static final String IO_THREAD_NAME = "log-producer-io-thread";

    private static final String IO_WORKER_BASE_NAME = "log-producer-io-worker-";

    private ExecutorService cachedThreadPool;
    private BlockingQueue<BlockedData> dataQueue = new LinkedBlockingQueue<BlockedData>();
    private ClientPool clientPool;
    private PackageManager packageManager;
    private ProducerConfig producerConfig;
    private AtomicLong sendLogBytes = new AtomicLong(0L);
    private AtomicLong sendLogTimeWindowInMillis = new AtomicLong(0L);

    public static IOThread launch(ClientPool cltPool, PackageManager packageManager,
                                  ProducerConfig producerConfig) {
        IOThread ioThread = new IOThread(cltPool, packageManager, producerConfig);
        ioThread.setName(IO_THREAD_NAME);
        ioThread.setDaemon(true);
        ioThread.start();
        return ioThread;
    }

    private IOThread(ClientPool cltPool, PackageManager packageManager,
                     ProducerConfig producerConfig) {
        this.clientPool = cltPool;
        this.packageManager = packageManager;
        this.producerConfig = producerConfig;
        cachedThreadPool = new ThreadPoolExecutor(0,
                producerConfig.maxIOThreadSizeInPool, 60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), new NamedThreadFactory(IO_WORKER_BASE_NAME));
    }

    public void addPackage(PackageData data, int bytes) {
        data.markAddToIOBeginTime();
        try {
            dataQueue.put(new BlockedData(data, bytes));
        } catch (InterruptedException e) {
            LOGGER.error("Failed to put data into dataQueue.", e);
        }
        data.markAddToIOEndTime();
    }

    public void shutdown() {
        this.interrupt();
        try {
            this.join();
        } catch (InterruptedException e) {
            LOGGER.warn("Failed to waiting for the IOThread to die. This may lead to data loss.",
                    e);
        }
        while (!dataQueue.isEmpty()) {
            BlockedData bd;
            try {
                bd = dataQueue.poll(producerConfig.packageTimeoutInMS / 2, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Failed to poll data from dataQueue.", e);
                break;
            }
            if (bd != null) {
                sendData(bd);
            }
        }
        cachedThreadPool.shutdown();
        try {
            if (cachedThreadPool.awaitTermination(
                    2 * producerConfig.packageTimeoutInMS, TimeUnit.MILLISECONDS)) {
                LOGGER.info("All submitted tasks in cachedThreadPool are executed.");
            } else {
                LOGGER.warn("The cachedThreadPool is not terminated. This may lead to data loss.");
            }
        } catch (InterruptedException e) {
            LOGGER.warn(
                    "The thread has been interrupted during shutdown. This may lead to data loss.",
                    e);
        }
    }

    public void shutdownNow() {
        this.interrupt();
        cachedThreadPool.shutdownNow();
    }

    private void sendData(BlockedData bd) {
        try {
            LOGGER.debug("Before execute doSendData(), blockedData={}", bd);
            doSendData(bd);
            LOGGER.debug("After execute doSendData(), blockedData={}", bd);
        } catch (Exception e) {
            LOGGER.error("Failed to send data.", e);
        } catch (Error e) {
            LOGGER.error("Failed to send data.", e);
        }
    }

    private void doSendData(BlockedData bd) {
        Client clt = clientPool.getClient(bd.data.project);
        if (clt == null) {
            bd.data.callback(null, new LogException("ProjectConfigNotExist",
                    "the config of project " + bd.data.project + " is not exist", ""), 0);
        } else {
            int retry = 0;
            LogException excep = null;
            PutLogsResponse response = null;
            while (retry++ <= producerConfig.retryTimes) {
                try {
                    if (bd.data.shardHash != null
                            && !bd.data.shardHash.isEmpty()) {
                        PutLogsRequest request = new PutLogsRequest(
                                bd.data.project, bd.data.logstore,
                                bd.data.topic, bd.data.source, bd.data.items,
                                bd.data.shardHash);
                        List<TagContent> tags = new ArrayList<TagContent>();
                        tags.add(new TagContent("__pack_id__", bd.data.getPackageId()));
                        request.SetTags(tags);
                        request.setContentType(producerConfig.logsFormat.equals("protobuf") ?
                                Consts.CONST_PROTO_BUF
                                : Consts.CONST_SLS_JSON);
                        response = clt.PutLogs(request);

                    } else {
                        PutLogsRequest request = new PutLogsRequest(
                                bd.data.project, bd.data.logstore,
                                bd.data.topic, bd.data.source, bd.data.items);
                        List<TagContent> tags = new ArrayList<TagContent>();
                        tags.add(new TagContent("__pack_id__", bd.data.getPackageId()));
                        request.SetTags(tags);
                        request.setContentType(producerConfig.logsFormat.equals("protobuf") ?
                                Consts.CONST_PROTO_BUF
                                : Consts.CONST_SLS_JSON);
                        response = clt.PutLogs(request);
                    }
                    long tmpBytes = sendLogBytes.get();
                    sendLogBytes.set(tmpBytes + bd.bytes);
                    break;
                } catch (LogException e) {
                    excep = new LogException(e.GetErrorCode(),
                            e.GetErrorMessage() + ", itemscount: "
                                    + bd.data.items.size(), e.GetRequestId());
                }
            }
            long currTime = System.currentTimeMillis();
            float sec = (currTime - sendLogTimeWindowInMillis.get()) / 1000.0f;
            float outflow = 0;
            if (sec > 0)
                outflow = sendLogBytes.get() / sec;
            bd.data.callback(response, excep, outflow);
        }
        packageManager.releaseBytes(bd.bytes);
    }

    @Override
    public void run() {
        try {
            LOGGER.info("The IOThread is going to work.");
            handleBlockedData();
            LOGGER.info("The IOThread terminated.");
        } catch (Exception e) {
            LOGGER.error("Failed to handle BlockedData.", e);
        }
    }

    private void handleBlockedData() {
        while (!isInterrupted()) {
            long currTime = System.currentTimeMillis();
            if ((currTime - sendLogTimeWindowInMillis.get()) > 60 * 1000) {
                sendLogBytes.set(0L);
                sendLogTimeWindowInMillis.set(currTime);
            }

            final BlockedData bd;
            try {
                bd = dataQueue.poll(
                        producerConfig.packageTimeoutInMS / 2, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOGGER.info("The IOThread has been interrupted when poll data from dataQueue.");
                break;
            }

            if (bd != null) {
                bd.data.markCompleteIOBeginTimeInMillis(dataQueue.size());
                try {
                    cachedThreadPool.submit(new Runnable() {
                        public void run() {
                            sendData(bd);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    try {
                        dataQueue.put(bd);
                    } catch (InterruptedException e1) {
                        LOGGER.info("Failed to put blockedData into data Queue. Try to send it in IOThread.");
                        sendData(bd);
                        break;
                    }
                }
            }
        }
    }
}
