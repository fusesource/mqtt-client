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

import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.transport.SslTransport;
import org.fusesource.hawtdispatch.transport.TcpTransport;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportListener;
import org.fusesource.mqtt.codec.CONNACK;
import org.fusesource.mqtt.codec.CONNECT;
import org.fusesource.mqtt.codec.MQTTFrame;
import org.fusesource.mqtt.codec.MQTTProtocolCodec;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.fusesource.hawtbuf.Buffer.utf8;
import static org.fusesource.hawtdispatch.Dispatch.NOOP;
import static org.fusesource.hawtdispatch.Dispatch.createQueue;


/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class MQTT {

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
    public synchronized static void setBlockingThreadPool(ThreadPoolExecutor pool) {
        blockingThreadPool = pool;
    }

    URI host;
    URI localURI;
    SSLContext sslContext;
    DispatchQueue dispatchQueue;
    Executor blockingExecutor;
    int maxReadRate;
    int maxWriteRate;
    int trafficClass = TcpTransport.IPTOS_THROUGHPUT;
    int receiveBufferSize = 1024*64;
    int sendBufferSize = 1024*64;
    boolean useLocalHost = true;
    CONNECT connect = new CONNECT();

    public MQTT() {
    }
    public MQTT(MQTT other) {
        this.host = other.host;
        this.localURI = other.localURI;
        this.sslContext = other.sslContext;
        this.dispatchQueue = other.dispatchQueue;
        this.blockingExecutor = other.blockingExecutor;
        this.maxReadRate = other.maxReadRate;
        this.maxWriteRate = other.maxWriteRate;
        this.trafficClass = other.trafficClass;
        this.receiveBufferSize = other.receiveBufferSize;
        this.sendBufferSize = other.sendBufferSize;
        this.useLocalHost = other.useLocalHost;
        this.connect = new CONNECT(other.connect);
    }

    public void connectCallback(final Callback<CallbackConnection> cb) {
        final MQTT builder = new MQTT(this);
        assert cb !=null : "Callback should not be null.";
        try {
            String scheme = builder.host.getScheme();
            final Transport transport;
            if( "tcp".equals(scheme) ) {
                transport = new TcpTransport();
            }  else if( SslTransport.protocol(scheme)!=null ) {
                SslTransport ssl = new SslTransport();
                if( builder.sslContext == null ) {
                    builder.sslContext = SSLContext.getInstance(SslTransport.protocol(scheme));
                }
                ssl.setSSLContext(builder.sslContext);
                if( builder.blockingExecutor == null ) {
                    builder.blockingExecutor = getBlockingThreadPool();
                }
                ssl.setBlockingExecutor(builder.blockingExecutor);
                transport = ssl;
            } else {
                throw new Exception("Unsupported URI scheme '"+scheme+"'");
            }

            if(builder.dispatchQueue == null) {
                builder.dispatchQueue = createQueue("stomp client");
            }
            transport.setDispatchQueue(builder.dispatchQueue);
            transport.setProtocolCodec(new MQTTProtocolCodec());

            if( transport instanceof TcpTransport ) {
                TcpTransport tcp = (TcpTransport)transport;
                tcp.setMaxReadRate(builder.maxReadRate);
                tcp.setMaxWriteRate(builder.maxWriteRate);
                tcp.setReceiveBufferSize(builder.receiveBufferSize);
                tcp.setSendBufferSize(builder.sendBufferSize);
                tcp.setTrafficClass(builder.trafficClass);
                tcp.setUseLocalHost(builder.useLocalHost);
                tcp.connecting(builder.host, builder.localURI);
            }

            TransportListener commandListener = new TransportListener() {
                public void onTransportConnected() {
                    transport.resumeRead();
                    boolean accepted = transport.offer(builder.connect.encode());
                    assert accepted: "First frame should always be accepted by the transport";

                }

                public void onTransportCommand(Object command) {
                    MQTTFrame response = (MQTTFrame) command;
                    try {
                        switch( response.commandType() ) {
                            case CONNACK.TYPE:
                                CONNACK connack = new CONNACK().decode(response);
                                switch(connack.code()) {
                                    case CONNECTION_ACCEPTED:
                                        transport.suspendRead();
                                        cb.onSuccess(new CallbackConnection(transport, builder));
                                        break;
                                    default:
                                        cb.onFailure(new IOException("Could not connect: " + connack.code()));
                                }
                                break;
                            default:
                                cb.onFailure(new IOException("Could not connect. Received unexpected command: " + response.commandType()));

                        }
                    } catch (ProtocolException e) {
                        cb.onFailure(e);
                    }
                }

                public void onTransportFailure(final IOException error) {
                    transport.stop(new Runnable() {
                        public void run() {
                            cb.onFailure(error);
                        }
                    });
                }

                public void onRefill() {
                }

                public void onTransportDisconnected(boolean reconnecting) {
                }
            };
            transport.setTransportListener(commandListener);
            transport.start(NOOP);

        } catch (Throwable e) {
            cb.onFailure(e);
        }
    }

    public Future<FutureConnection> connectFuture() {
        final Promise<FutureConnection> future = new Promise<FutureConnection>();
        connectCallback(new Callback<CallbackConnection>() {
            public void onFailure(Throwable value) {
                future.onFailure(value);
            }

            public void onSuccess(CallbackConnection value) {
                future.onSuccess(new FutureConnection(value));
            }
        });
        return future;
    }

    public BlockingConnection connectBlocking() throws Exception {
        return new BlockingConnection(connectFuture().await());
    }

    public UTF8Buffer getClientId() {
        return connect.getClientId();
    }

    public short getKeepAlive() {
        return connect.getKeepAlive();
    }

    public UTF8Buffer getPassword() {
        return connect.getPassword();
    }

    public byte getType() {
        return connect.getType();
    }

    public UTF8Buffer getUserName() {
        return connect.getUserName();
    }

    public UTF8Buffer getWillMessage() {
        return connect.getWillMessage();
    }

    public byte getWillQos() {
        return connect.getWillQos();
    }

    public UTF8Buffer getWillTopic() {
        return connect.getWillTopic();
    }

    public boolean isCleanSession() {
        return connect.isCleanSession();
    }

    public boolean isWillRetain() {
        return connect.isWillRetain();
    }

    public void setCleanSession(boolean cleanSession) {
        connect.setCleanSession(cleanSession);
    }

    public void setClientId(String clientId) {
        this.setClientId(utf8(clientId));
    }
    public void setClientId(UTF8Buffer clientId) {
        connect.setClientId(clientId);
    }

    public void setKeepAlive(short keepAlive) {
        connect.setKeepAlive(keepAlive);
    }

    public void setPassword(String password) {
        this.setPassword(utf8(password));
    }
    public void setPassword(UTF8Buffer password) {
        connect.setPassword(password);
    }

    public void setUserName(String password) {
        this.setUserName(utf8(password));
    }
    public void setUserName(UTF8Buffer userName) {
        connect.setUserName(userName);
    }

    public void setWillMessage(UTF8Buffer willMessage) {
        connect.setWillMessage(willMessage);
    }

    public void setWillQos(byte willQos) {
        connect.setWillQos(willQos);
    }

    public void setWillRetain(boolean willRetain) {
        connect.setWillRetain(willRetain);
    }

    public void setWillTopic(String password) {
        this.setWillTopic(utf8(password));
    }
    public void setWillTopic(UTF8Buffer willTopic) {
        connect.setWillTopic(willTopic);
    }

    public Executor getBlockingExecutor() {
        return blockingExecutor;
    }

    public void setBlockingExecutor(Executor blockingExecutor) {
        this.blockingExecutor = blockingExecutor;
    }

    public DispatchQueue getDispatchQueue() {
        return dispatchQueue;
    }

    public void setDispatchQueue(DispatchQueue dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
    }

    public URI getLocalURI() {
        return localURI;
    }

    public void setLocalURI(URI localURI) {
        this.localURI = localURI;
    }

    public int getMaxReadRate() {
        return maxReadRate;
    }

    public void setMaxReadRate(int maxReadRate) {
        this.maxReadRate = maxReadRate;
    }

    public int getMaxWriteRate() {
        return maxWriteRate;
    }

    public void setMaxWriteRate(int maxWriteRate) {
        this.maxWriteRate = maxWriteRate;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public URI getHost() {
        return host;
    }
    public void setHost(String host, int port) throws URISyntaxException {
        this.setHost(new URI("tcp://"+host+":"+port));
    }
    public void setHost(String host) throws URISyntaxException {
        this.setHost(new URI("tcp://"+host+":1883"));
    }
    public void setHost(URI host) {
        this.host = host;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public int getTrafficClass() {
        return trafficClass;
    }

    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    public boolean isUseLocalHost() {
        return useLocalHost;
    }

    public void setUseLocalHost(boolean useLocalHost) {
        this.useLocalHost = useLocalHost;
    }

}
