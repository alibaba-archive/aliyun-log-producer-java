package com.aliyun.openservices.log.producer.inner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.aliyun.openservices.log.Client;
import com.aliyun.openservices.log.common.Shard;
import com.aliyun.openservices.log.exception.LogException;
import com.aliyun.openservices.log.producer.ProducerConfig;

class ShardHash {
	public long updateTime;
	public ArrayList<String> hash;
	public ReadWriteLock rwLock = new ReentrantReadWriteLock();

	public ShardHash() {
		super();
		updateTime = 0L;
	}
};

public class ShardHashManager {
	private ClientPool clientPool;
	private HashMap<String, ShardHash> shardHash = new HashMap<String, ShardHash>();
	private ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private ProducerConfig producerConfig;

	public ShardHashManager(ClientPool clientPool, ProducerConfig producerConfig) {
		super();
		this.clientPool = clientPool;
		this.producerConfig = producerConfig;
	}

	public void filterExpired() {
		ArrayList<String> removes = new ArrayList<String>();
		rwLock.readLock().lock();
		long cur = System.currentTimeMillis();
		for (Entry<String, ShardHash> entry : shardHash.entrySet()) {
			entry.getValue().rwLock.readLock().lock();
			if (cur - entry.getValue().updateTime > producerConfig.shardHashUpdateIntervalInMS * 2) {
				removes.add(entry.getKey());
			}
			entry.getValue().rwLock.readLock().unlock();
		}
		rwLock.readLock().unlock();
		if (!removes.isEmpty()) {
			rwLock.writeLock().lock();
			for (String key : removes) {
				shardHash.remove(key);
			}
			rwLock.writeLock().unlock();
		}
	}

	public String getBeginHash(String project, String logstore, String hash) {
		if (hash == null) {
			return hash;
		}
		Client client = clientPool.getClient(project);
		if (client == null) {
			return hash;
		}
		String key = project + "|" + logstore;
		rwLock.readLock().lock();
		ShardHash h = shardHash.get(key);
		if (h == null) {
			rwLock.readLock().unlock();
			rwLock.writeLock().lock();
			h = shardHash.get(key);
			if (h == null) {
				h = new ShardHash();
				shardHash.put(key, h);
			}
			h.rwLock.readLock().lock();
			rwLock.writeLock().unlock();
		} else {
			h.rwLock.readLock().lock();
			rwLock.readLock().unlock();
		}
		long cur = System.currentTimeMillis();
		if (cur - h.updateTime >= producerConfig.shardHashUpdateIntervalInMS) {
			h.rwLock.readLock().unlock();
			h.rwLock.writeLock().lock();
			if (cur - h.updateTime >= producerConfig.shardHashUpdateIntervalInMS) {
				ArrayList<Shard> shards = null;
				try {
					shards = client.ListShard(project, logstore).GetShards();
				} catch (LogException e) {
				}
				if (shards != null) {
					h.hash = new ArrayList<String>();
					for (Shard s : shards) {
						if(s.getStatus().compareToIgnoreCase("readonly") != 0){
							h.hash.add(s.getInclusiveBeginKey());
						}
					}
					Collections.sort(h.hash);
					h.updateTime = cur;
				}
			}
			h.rwLock.writeLock().unlock();
			h.rwLock.readLock().lock();
		}
		if (h.hash != null && !h.hash.isEmpty()) {
			int low = 0, high = h.hash.size() - 1, mid = 0;
			int action = 0;
			while (low <= high) {
				mid = (low + high) / 2;
				int cmp = h.hash.get(mid).compareTo(hash);
				if (cmp == 0) {
					action = 0;
					break;
				} else if (cmp < 0) {
					action = 1;
					low = mid + 1;
				} else {
					action = -1;
					high = mid - 1;
				}
			}
			if (action == -1 && mid > 0) {
				--mid;
			}
			h.rwLock.readLock().unlock();
			return h.hash.get(mid);
		}
		h.rwLock.readLock().unlock();
		return hash;
	}
}
