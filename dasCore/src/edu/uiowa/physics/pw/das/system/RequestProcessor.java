/* File: TableDataSet.java
 * Copyright (C) 2002-2004 The University of Iowa
 *
 * Created on June 1, 2004, 11:46 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das.system;

import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.util.*;
import java.util.*;

/** Utility class for synchronous execution.
 * This class maintains a pool of threads that are used to execute arbitrary
 * code.  This class also serves as a central place to catch and handle
 * unchecked exceptions.
 *
 * The {@link #invokeLater(java.lang.Runnable)} method is similar to the
 * SwingUtilities {@link javax.swing.SwingUtilities#invokeLater(java.lang.Runnable)}
 * method, except that the request is not executed on the event thread.
 *
 * The {@link #invokeLater(java.lang.Runnable, java.lang.Object)},
 * the {@link #invokeAfter(java.lang.Runnable, java.lang.Object)},
 * and the {@link #waitFor(java.lang.Object)} methods are designed to work
 * together.  Both of the first two methods execute code asynchronously with
 * respect to the calling thread.  Multiple requests made with a call to
 * invokeLater that specified the same lock can execute at the same time,
 * but not while a request made with the invokeAfter with the same lock
 * is processing.  Any requests made before an invokeAfter request with the
 * same lock will finish before that invokeAfter request begins. An
 * invokeAfter request will finish before any requests with the same lock made
 * after that invokeAfter request begins.  The {@link #waitFor(java.lang.Object)}
 * method will cause the calling thread to block until all requests with the
 * specified lock finish.
 */
public final class RequestProcessor {
    
    private static final BlockingRequestQueue queue = new BlockingRequestQueue();
    private static final WeakHashMap runnableQueueMap = new WeakHashMap();
    private static final Runner runner = new Runner();
    
    private static int maxThreadCount = 5;
    private static int threadCount = 0;
    private static final Object THREAD_COUNT_LOCK = new Object();
    
    private static int threadOrdinal = 0;
    
    private RequestProcessor() {}
    
    private static void newThread() {
        String name = "RequestProcessor[" + (threadOrdinal++) + "]";
        new Thread(runner, name).start();
    }
    
    /** Executes run.run() asynchronously on a thread from the thread pool.
     * @param run the task to be executed.
     */    
    public static void invokeLater(Runnable run) {
        DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).info("invokeLater "+run);
        
        synchronized (THREAD_COUNT_LOCK) {
            if (threadCount < maxThreadCount) {
                newThread();
            }
        }
        queue.add(run);
    }
    
    /** Executes run.run() asynchronously on a thread from the thread pool.
     * The task will not be executed until after all requests made with
     * {@link #invokeAfter(java.lang.Runnable, java.lang.Object)} with the same
     * lock have finished.
     * @param run the taks to be executed.
     * @param lock associates run with other tasks.
     */
    public static void invokeLater(Runnable run, Object lock) {
        DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).info("invokeLater "+run+" "+lock);        
        synchronized (THREAD_COUNT_LOCK) {
            if (threadCount < maxThreadCount) {
                newThread();
            }
        }
        synchronized (runnableQueueMap) {
            RunnableQueue rq = (RunnableQueue)runnableQueueMap.get(lock);
            if (rq == null) {
                rq = new RunnableQueue();
                runnableQueueMap.put(lock, rq);
            }
            rq.add(run, false);
            queue.add(rq);
        }
    }
    
    /** Executes run.run() asynchronously on a thread from the thread pool.
     * The task will not be executed until after all requests made with
     * {@link #invokeAfter(java.lang.Runnable, java.lang.Object)} or
     * {@link #invokeLater(java.lang.Runnable, java.lang.Object)} with the same
     * lock have finished.
     * @param run the taks to be executed.
     * @param lock associates run with other tasks.
     */
    public static void invokeAfter(Runnable run, Object lock) {
        DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).info("invokeAfter "+run+" "+lock);
        synchronized (THREAD_COUNT_LOCK) {
            if (threadCount < maxThreadCount) {
                newThread();
            }
        }
        synchronized (runnableQueueMap) {
            RunnableQueue rq = (RunnableQueue)runnableQueueMap.get(lock);
            if (rq == null) {
                rq = new RunnableQueue();
                runnableQueueMap.put(lock, rq);
            }
            rq.add(run, true);
            queue.add(rq);
        }
    }
    
    /** Blocks until all tasks with the same lock have finished.
     * @param lock
     * @throws InterruptedException if the current thread is
     *      interrupted while waiting.
     */
    public static void waitFor(Object lock) throws InterruptedException {
        WaitTask wt = new WaitTask();
        synchronized (wt) {
            while (true) {
                invokeLater(wt, lock);
                wt.wait();
                return;
            }
        }
    }
    
    /*
    public static int getMaximumThreadCount(int i) {
        return maxThreadCount;
    }
    
    public static void setMaximumThreadCount(int i) {
        if (i < 5) {
            throw new IllegalArgumentException("Must be >= 5: " + i);
        }
        maxThreadCount = i;
    }
     */
    
    private static class Runner implements Runnable {
        public void run() {
            synchronized (THREAD_COUNT_LOCK) {
                threadCount++;
            }
            try {
                while (true) {
                    try {                        
                        Runnable run = queue.remove();
                        DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).fine("running "+run);
                        if (run != null) {
                            run.run();
                            DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).fine("completed "+run);
                        }                             
                        synchronized (THREAD_COUNT_LOCK) {
                            if (threadCount > maxThreadCount) {
                                break;
                            }
                        }
                    }
                    catch (ThreadDeath td) {
                        // See documentation for ThreadDeath.  If this error is caught but not thrown, then the thread doesn't die.
                        throw td;
                    }
                    catch (Throwable t) {
                        DasApplication.getDefaultApplication().getLogger(DasApplication.SYSTEM_LOG).info("uncaught exception "+t);
                        DasExceptionHandler.handleUncaught(t);
                        //Clear interrupted status (if set)
                        Thread.interrupted();
                    }
                }
            }
            finally {
                synchronized (THREAD_COUNT_LOCK) {
                    threadCount--;
                }
            }
        }
    }
    
    private static class WaitTask implements Runnable {
        public synchronized void run() {
            notifyAll();
        }
    }
    
    private static class RunnableQueue implements Runnable {
        
        private LinkedList list = new LinkedList();
        private int readCount = 0;
        private Object writer;
        
        public void run() {
            Runnable run = null;
            RequestEntry entry = null;
            while (run == null) {
                synchronized (this) {
                    entry = (RequestEntry)list.removeFirst();
                    if (entry.async && readCount == 0 && writer == null) {
                        writer = entry;
                        run = entry.run;
                    }
                    else if (!entry.async && writer == null) {
                        readCount++;
                        run = entry.run;
                    }
                    else {
                        list.addFirst(entry);
                    }
                }
            }
            run.run();
            synchronized (this) {
                if (entry.async) {
                    writer = null;
                }
                else {
                    readCount--;
                }
                notifyAll();
            }
        }
        
        synchronized void add(Runnable run, boolean async) {
            RequestEntry entry = new RequestEntry();
            entry.run = run;
            entry.async = async;
            list.add(entry);
        }
    }
    
    private static class RequestEntry { Runnable run; boolean async; }
    
    private static class BlockingRequestQueue {
        private LinkedList list;
        
        BlockingRequestQueue() {
            list = new LinkedList();
        }
        
        synchronized void add(Runnable r) {
            list.add(r);
            notify();
        }
        
        synchronized Runnable remove() {
            while (list.isEmpty()) {
                try { wait(); } catch (InterruptedException ie) {};
            }
            return (Runnable)list.removeFirst();
        }
        
    }
}