package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2017 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.searchbox.action.BulkableAction;
import io.searchbox.core.Bulk;

public class BulkEmitter extends Observable {

    private final AtomicInteger size = new AtomicInteger();

    private final int maxSize;
    private final int interval;

    private final AtomicReference<Bulk.Builder> builder = new AtomicReference<>(new Bulk.Builder());

    private final Timer scheduler = new Timer();

    private final BuilderLock builderLock = new BuilderLock();
    private final ObserverLock observerLock = new ObserverLock();

    public BulkEmitter(int atSize, int intervalInMillis) {
        this.maxSize = atSize;
        this.interval = intervalInMillis;
        this.scheduler.scheduleAtFixedRate(createNotificationTask(), 0, interval);
    }

    @Override
    public final void notifyObservers() {
        if (size.get() == 0) {
            return;
        }
        this.size.set(0);
        synchronized (observerLock) {
            setChanged();
            notifyObservers(builder.getAndSet(new Bulk.Builder()).build());
        }
    }

    public void add(BulkableAction action) {
        synchronized (builderLock) {
            builder.get().addAction(action);
        }
        if (size.incrementAndGet() >= maxSize) {
            notifyObservers();
        }
    }

    private TimerTask createNotificationTask() {
        return new TimerTask() {
            @Override
            public void run() {
                notifyObservers();
            }
        };
    }

    /*
     * Class used as monitor to increase lock visibility in profiling tools
     */
    private class ObserverLock {
    }

    /*
     * Class used as monitor to increase lock visibility in profiling tools
     */
    private class BuilderLock {
    }
}
