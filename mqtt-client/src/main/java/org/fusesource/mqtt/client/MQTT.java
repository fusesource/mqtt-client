/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fusesource.mqtt.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MQTT {

    private MQTT() {}

    private static final long KEEP_ALIVE = Long.parseLong(System.getProperty("mqtt.thread.keep_alive", ""+1000));
    private static final long STACK_SIZE = Long.parseLong(System.getProperty("mqtt.thread.stack_size", ""+1024*512));
    private static ThreadPoolExecutor blockingThreadPool;

    public synchronized static ThreadPoolExecutor getBlockingThreadPool() {
        if( blockingThreadPool == null ) {
            blockingThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, KEEP_ALIVE, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread rc = new Thread(null, r, "MQTT Task", STACK_SIZE);
                        rc.setDaemon(true);
                        return rc;
                    }
                }) {

                    @Override
                    public void shutdown() {
                        // we don't ever shutdown since we are shared..
                    }

                    @Override
                    public List<Runnable> shutdownNow() {
                        // we don't ever shutdown since we are shared..
                        return Collections.emptyList();
                    }
                };
        }
        return blockingThreadPool;
    }

    static public org.fusesource.mqtt.client.callback.ConnectionBuilder callback(URI uri) {
        return new org.fusesource.mqtt.client.callback.ConnectionBuilder(uri);
    }
    static public org.fusesource.mqtt.client.callback.ConnectionBuilder callback(String uri) throws URISyntaxException {
        return callback(new URI(uri));
    }
    static public org.fusesource.mqtt.client.callback.ConnectionBuilder callback(String host, int port) throws URISyntaxException {
        return callback("tcp://"+host+":"+port);
    }

    static public org.fusesource.mqtt.client.future.ConnectionBuilder future(URI uri) {
        return new org.fusesource.mqtt.client.future.ConnectionBuilder(callback(uri));
    }
    static public org.fusesource.mqtt.client.future.ConnectionBuilder future(String uri) throws URISyntaxException {
        return future(new URI(uri));
    }
    static public org.fusesource.mqtt.client.future.ConnectionBuilder future(String host, int port) throws URISyntaxException {
        return future("tcp://"+host+":"+port);
    }


    static public org.fusesource.mqtt.client.blocking.ConnectionBuilder blocking(URI uri) {
        return new org.fusesource.mqtt.client.blocking.ConnectionBuilder(future(uri));
    }
    static public org.fusesource.mqtt.client.blocking.ConnectionBuilder blocking(String uri) throws URISyntaxException {
        return blocking(new URI(uri));
    }
    static public org.fusesource.mqtt.client.blocking.ConnectionBuilder blocking(String host, int port) throws URISyntaxException {
        return blocking("tcp://"+host+":"+port);
    }

}
