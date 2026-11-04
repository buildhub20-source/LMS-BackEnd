package com.lms.common.sharedstate;

import java.time.Duration;

/**
 * Makes a scheduled job run on one backend copy at a time. With Redis enabled the
 * lock is shared; without it only overlapping runs in this process are prevented.
 */
public interface ClusterLock {

    /**
     * Runs {@code task} if no other copy holds {@code name}; otherwise skips it.
     * {@code maxHold} bounds how long the lock survives if this copy dies mid-run.
     *
     * @return true if the task ran here
     */
    boolean runExclusively(String name, Duration maxHold, Runnable task);
}
