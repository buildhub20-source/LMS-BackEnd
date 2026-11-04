package com.lms.common.sharedstate;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Single-process lock, for running without Redis. */
public class LocalClusterLock implements ClusterLock {

    private final Set<String> held = ConcurrentHashMap.newKeySet();

    @Override
    public boolean runExclusively(String name, Duration maxHold, Runnable task) {
        if (!held.add(name)) {
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            held.remove(name);
        }
    }
}
