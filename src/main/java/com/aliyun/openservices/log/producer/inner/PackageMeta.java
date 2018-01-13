package com.aliyun.openservices.log.producer.inner;

import java.util.concurrent.locks.ReentrantLock;

class PackageMeta {
    public long arriveTimeInMS;
    public int logLinesCount = 0;
    public int packageBytes = 0;
    public ReentrantLock lock = new ReentrantLock();

    public PackageMeta(int logLinesCount, int packageBytes) {
        super();
        this.arriveTimeInMS = System.currentTimeMillis();
        this.logLinesCount = logLinesCount;
        this.packageBytes = packageBytes;
    }

    public void clear() {
        this.arriveTimeInMS = System.currentTimeMillis();
        this.logLinesCount = 0;
        this.packageBytes = 0;
    }
};