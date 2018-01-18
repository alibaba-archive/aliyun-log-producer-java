package com.aliyun.openservices.log.producer.inner;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by brucewu on 2018/1/12.
 */
class NamedThreadFactory implements ThreadFactory {

    private final String baseName;
    private final AtomicInteger threadNum = new AtomicInteger(0);

    NamedThreadFactory(String baseName) {
        this.baseName = baseName;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(baseName + threadNum.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }

}
