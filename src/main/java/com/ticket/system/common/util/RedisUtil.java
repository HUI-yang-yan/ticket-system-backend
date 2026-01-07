package com.ticket.system.common.util;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        redissonClient.getBucket(key).set(value);
    }

    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redissonClient.getBucket(key).set(value, timeout, unit);
    }

    /**
     * 获取缓存
     */
    public Object get(String key) {
        return redissonClient.getBucket(key).get();
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.expire(timeout, unit);
    }

    /**
     * 获取过期时间（秒 / 毫秒都支持）
     */
    public Long getExpire(String key, TimeUnit unit) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        long ttlMillis = bucket.remainTimeToLive();
        if (ttlMillis < 0) {
            return ttlMillis;
        }
        return unit.convert(ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 递增
     */
    public Long increment(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    /**
     * 递减
     */
    public Long decrement(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    /**
     * 获取分布式锁（SETNX 替代方案）
     */
    public Boolean lock(String key, String value, long timeout, TimeUnit unit) {
        RLock lock = redissonClient.getLock(key);
        try {
            return lock.tryLock(0, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 释放分布式锁
     */
    public Boolean unlock(String key, String value) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            return true;
        }
        return false;
    }
}
