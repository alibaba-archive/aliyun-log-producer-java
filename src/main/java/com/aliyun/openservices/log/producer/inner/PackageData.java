package com.aliyun.openservices.log.producer.inner;

import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.aliyun.openservices.log.common.Consts;
import com.aliyun.openservices.log.common.LogItem;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.producer.ILogCallback;
import com.aliyun.openservices.log.response.PutLogsResponse;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PackageData {

    private static final Logger LOGGER = LoggerFactory.getLogger(PackageData.class);

    public String project;
    public String logstore;
    public String topic;
    public String shardHash;
    public String source;
    public String packageId;
    public LinkedList<LogItem> items = new LinkedList<LogItem>();
    public LinkedList<ILogCallback> callbacks = new LinkedList<ILogCallback>();

    public static String contextHash;
    public static AtomicLong contextOrder = new AtomicLong(0);

    static String GetLocalMachineIp() {
        InetAddressValidator validator = new InetAddressValidator();
        String candidate = "";
        try {
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface
                    .getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = ifaces.nextElement();

                if (iface.isUp()) {
                    for (Enumeration<InetAddress> addresses = iface
                            .getInetAddresses(); addresses.hasMoreElements(); ) {

                        InetAddress address = addresses.nextElement();

                        if (!address.isLinkLocalAddress() && address.getHostAddress() != null) {
                            String ipAddress = address.getHostAddress();
                            if (ipAddress.equals(Consts.CONST_LOCAL_IP)) {
                                continue;
                            }
                            if (validator.isValidInet4Address(ipAddress)) {
                                return ipAddress;
                            }
                            if (validator.isValid(ipAddress)) {
                                candidate = ipAddress;
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            LOGGER.error("Failed to get local machine IP.", e);
        }
        return candidate;
    }

    static {
        String candidate = GetLocalMachineIp() + "-" + ManagementFactory.getRuntimeMXBean().getName();
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(candidate.getBytes());
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            contextHash = bigInt.toString(16).toUpperCase();
            if (contextHash.length() > 16) contextHash = contextHash.substring(0, 16);
            else if (contextHash.length() < 16) {
                while (contextHash.length() < 16) contextHash = "0" + contextHash;
            }
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to generate contextHash.", e);
        }

    }

    public PackageData(String project, String logstore, String topic,
                       String shardHash, String source) {
        super();
        this.project = project;
        this.logstore = logstore;
        this.topic = topic;
        this.shardHash = shardHash;
        this.source = source;
        this.packageId = contextHash + "-" + Long.toHexString(contextOrder.incrementAndGet()).toUpperCase();
    }

    public void addItems(List<LogItem> logItems, ILogCallback callabck) {
        items.addAll(logItems);
        if (callabck != null) {
            callbacks.add(callabck);
        }
    }

    public void clear() {
        items.clear();
        callbacks.clear();
    }

    public void markAddToIOBeginTime() {
        long curr = System.currentTimeMillis();
        for (ILogCallback cb : callbacks) {
            cb.addToIOQueueBeginTimeInMillis = curr;
        }
    }

    public void markAddToIOEndTime() {
        long curr = System.currentTimeMillis();
        for (ILogCallback cb : callbacks) {
            cb.addToIOQueueEndTimeInMillis = curr;
        }
    }

    public void markCompleteIOBeginTimeInMillis(final int queueSize) {
        long curr = System.currentTimeMillis();
        for (ILogCallback cb : callbacks) {
            cb.completeIOBeginTimeInMillis = curr;
            cb.ioQueueSize = queueSize;
        }
    }

    public void callback(PutLogsResponse response, LogException e, float srcOutFlow) {
        long curr = System.currentTimeMillis();
        for (ILogCallback cb : callbacks) {
            cb.completeIOEndTimeInMillis = curr;
            cb.sendBytesPerSecond = srcOutFlow;
            cb.onCompletion(response, e);
        }
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    @Override
    public String toString() {
        return "PackageData{" +
                "project='" + project + '\'' +
                ", logstore='" + logstore + '\'' +
                ", topic='" + topic + '\'' +
                ", shardHash='" + shardHash + '\'' +
                ", source='" + source + '\'' +
                ", packageId='" + packageId + '\'' +
                ", items=" + items +
                ", callbacks=" + callbacks +
                '}';
    }
}
