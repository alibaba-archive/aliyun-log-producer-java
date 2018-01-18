package com.aliyun.openservices.log.producer.inner;

import com.aliyun.openservices.log.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by brucewu on 2018/1/12.
 */
public class ControlThreadPool {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlThreadPool.class);

    private static final String CONTROL_WORKER_BASE_NAME = "log-producer-control-worker-";

    private ShardHashManager shardHashManager;
    private PackageManager packageManager;
    private ProducerConfig producerConfig;
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool
            (2, new NamedThreadFactory(CONTROL_WORKER_BASE_NAME));

    public static ControlThreadPool launch(ShardHashManager shardHashManager,
                                           PackageManager packageManager,
                                           ProducerConfig producerConfig) {
        ControlThreadPool pool = new ControlThreadPool(shardHashManager, packageManager,
                producerConfig);
        pool.scheduleFilterTimeoutPackageTask();
        pool.scheduleFilterExpiredTask();
        return pool;
    }

    private ControlThreadPool(ShardHashManager shardHashManager, PackageManager packageManager,
                             ProducerConfig producerConfig) {
        this.shardHashManager = shardHashManager;
        this.packageManager = packageManager;
        this.producerConfig = producerConfig;
    }

    public void shutdown() {
        scheduledExecutorService.shutdown();
    }

    public void shutdownNow() {
        scheduledExecutorService.shutdownNow();
    }

    private void scheduleFilterTimeoutPackageTask() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    LOGGER.debug("Begin to filter timeout package.");
                    packageManager.filterTimeoutPackage();
                } catch (Exception e) {
                    LOGGER.error("Failed to filter timeout package.", e);
                }
            }
        }, 0, producerConfig.packageTimeoutInMS / 2, TimeUnit.MILLISECONDS);
    }

    private void scheduleFilterExpiredTask() {
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    LOGGER.debug("Begin to filter expired.");
                    shardHashManager.filterExpired();
                } catch (Exception e) {
                    LOGGER.error("Failed to filter expired.", e);
                }
            }
        }, 0, producerConfig.shardHashUpdateIntervalInMS * 2, TimeUnit.MILLISECONDS);
    }

}
