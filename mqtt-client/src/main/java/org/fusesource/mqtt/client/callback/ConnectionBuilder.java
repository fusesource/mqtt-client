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

package org.fusesource.mqtt.client.callback;

import static org.fusesource.hawtbuf.Buffer.*;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.mqtt.client.*;
import org.fusesource.mqtt.client.codec.CONNACK;
import org.fusesource.mqtt.client.codec.CONNECT;
import org.fusesource.mqtt.client.codec.MQTTFrame;
import org.fusesource.mqtt.client.codec.MQTTProtocolCodec;
import org.fusesource.hawtdispatch.transport.SslTransport;
import org.fusesource.hawtdispatch.transport.TcpTransport;
import org.fusesource.hawtdispatch.transport.Transport;
import org.fusesource.hawtdispatch.transport.TransportListener;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.URI;
import java.util.concurrent.Executor;

import static org.fusesource.hawtdispatch.Dispatch.NOOP;
import static org.fusesource.hawtdispatch.Dispatch.createQueue;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ConnectionBuilder {

    private URI remoteURI;
    private URI localURI;
    private SSLContext sslContext;
    private DispatchQueue dispatchQueue;
    private Executor blockingExecutor;
    private int maxReadRate;
    private int maxWriteRate;
    private int trafficClass = TcpTransport.IPTOS_THROUGHPUT;
    private int receiveBufferSize = 1024*64;
    private int sendBufferSize = 1024*64;
    private boolean useLocalHost = true;
    private CONNECT connect = new CONNECT();

    public ConnectionBuilder(ConnectionBuilder other) {
        this.remoteURI = other.remoteURI;
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

    public ConnectionBuilder(URI remoteURI) {
        assert remoteURI !=null : "URI should not be null.";
        this.remoteURI = remoteURI;
    }

    public ConnectionBuilder cleanSession(boolean cleanSession) {
        connect.cleanSession(cleanSession);
        return this;
    }

    public ConnectionBuilder clientId(String clientId) {
        connect.clientId(utf8(clientId));
        return this;
    }

    public ConnectionBuilder keepAlive(short keepAlive) {
        connect.keepAlive(keepAlive);
        return this;
    }

    public ConnectionBuilder password(String password) {
        connect.password(utf8(password));
        return this;
    }

    public ConnectionBuilder userName(String userName) {
        connect.userName(utf8(userName));
        return this;
    }

    public ConnectionBuilder willMessage(String willMessage) {
        connect.willMessage(utf8(willMessage));
        return this;
    }

    public ConnectionBuilder willQos(byte willQos) {
        connect.willQos(willQos);
        return this;
    }

    public ConnectionBuilder willRetain(boolean willRetain) {
        connect.willRetain(willRetain);
        return this;
    }

    public ConnectionBuilder willTopic(String willTopic) {
        connect.willTopic(utf8(willTopic));
        return this;
    }

    public ConnectionBuilder blockingExecutor(Executor blockingExecutor) {
        this.blockingExecutor = blockingExecutor;
        return this;
    }

    public ConnectionBuilder dispatchQueue(DispatchQueue dispatchQueue) {
        this.dispatchQueue = dispatchQueue;
        return this;
    }

    public ConnectionBuilder localURI(URI localURI) {
        this.localURI = localURI;
        return this;
    }

    public ConnectionBuilder maxReadRate(int maxReadRate) {
        this.maxReadRate = maxReadRate;
        return this;
    }

    public ConnectionBuilder maxWriteRate(int maxWriteRate) {
        this.maxWriteRate = maxWriteRate;
        return this;
    }

    public ConnectionBuilder receiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
        return this;
    }

    public ConnectionBuilder sendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    public ConnectionBuilder sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public ConnectionBuilder trafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
        return this;
    }

    public ConnectionBuilder useLocalHost(boolean useLocalHost) {
        this.useLocalHost = useLocalHost;
        return this;
    }

    public void connect(final Callback<Connection> cb) {
        final ConnectionBuilder builder = new ConnectionBuilder(this);
        assert cb!=null : "Callback should not be null.";
        try {
            String scheme = builder.remoteURI.getScheme();
            final Transport transport;
            if( "tcp".equals(scheme) ) {
                transport = new TcpTransport();
            }  else if( SslTransport.protocol(scheme)!=null ) {
                SslTransport ssl = new SslTransport();
                if( builder.sslContext == null ) {
                    builder.sslContext = SSLContext.getInstance(SslTransport.protocol(scheme));
                }
                ssl.setSSLContext(sslContext);
                if( builder.blockingExecutor == null ) {
                    builder.blockingExecutor = MQTT.getBlockingThreadPool();
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
                tcp.connecting(builder.remoteURI, builder.localURI);
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
                                        cb.success(new Connection(transport, builder));
                                        break;
                                    default:
                                        cb.failure(new IOException("Could not connect: "+connack.code()));
                                }
                                break;
                            default:
                                cb.failure(new IOException("Could not connect. Received unexpected command: " + response.commandType()));

                        }
                    } catch (ProtocolException e) {
                        cb.failure(e);
                    }
                }

                public void onTransportFailure(final IOException error) {
                    transport.stop(new Runnable() {
                        public void run() {
                            cb.failure(error);
                        }
                    });
                }

                public void onRefill() {
                }

                public void onTransportDisconnected() {
                }
            };
            transport.setTransportListener(commandListener);
            transport.start(NOOP);

        } catch (Throwable e) {
            cb.failure(e);
        }


    }
}
