## Overview

MQTT is a machine-to-machine (M2M)/"Internet of Things" connectivity
protocol. It was designed as an extremely lightweight publish/subscribe
messaging transport. It is useful for connections with remote locations where
a small code footprint is required and/or network bandwidth is at a premium.

mqtt-client provides an ASL 2.0 licensed API to MQTT. It takes care of
automatically reconnecting to your MQTT server and restoring your client
session if any network failures occur. Applications can use a blocking API
style, a futures based API, or a callback/continuations passing API style.

## Using from Maven

Add the following to your maven `pom.xml` file.

    <dependency>
      <groupId>org.fusesource.mqtt-client</groupId>
      <artifactId>mqtt-client</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>

    <repositories>
      <repository>
        <id>fusesource.snapshots</id>
        <name>FuseSource Snapshot Repository</name>
        <url>http://repo.fusesource.com/nexus/content/repositories/snapshots</url>
        <snapshots><enabled>true</enabled></snapshots>
        <releases><enabled>false</enabled></releases>
      </repository>
    </repositories>

## Using from any Other Build System

Download the 
[uber jar file](http://repo.fusesource.com/nexus/service/local/artifact/maven/redirect?r=snapshots&g=org.fusesource.mqtt-client&a=mqtt-client&v=1.0-SNAPSHOT&e=jar&c=uber) 
and add it to your build. The uber contains all the stripped down dependencies
which the mqtt-client depends on from other projects.

## Using on Java 1.4 

We also provide an 
[java 1.4 uber jar file](http://repo.fusesource.com/nexus/service/local/artifact/maven/redirect?r=snapshots&g=org.fusesource.mqtt-client&a=mqtt-client-java1.4-uber&v=1.0-SNAPSHOT&e=jar) 
which is compatible with Java 1.4 JVMs.  This version of the jar
does not support SSL connections since the SSLEngine class used to implement SSL on NIO
was not introduced until Java 1.5.

## Configuring the MQTT Connection

The blocking, future, and callback APIs all share the same connection setup.
You create a new instance of the `MQTT` class and configure it with connection
and socket related options. At a minimum the `setHost` method be called before
attempting to connect.

    MQTT mqtt = new MQTT();
    mqtt.setHost("localhost", 1883);
    // or 
    mqtt.setHost("tcp://localhost:1883");
    
### Controlling MQTT Options


* `setClientId` : Use to set the client Id of the session.  This is what an MQTT server
  uses to identify a session where `setCleanSession(false);` is being used.  The id must be
  23 characters or less.  Defaults to auto generated id (based on your socket address, port 
  and timestamp).

* `setCleanSession` : Set to false if you want the MQTT server to persist topic subscriptions
  and ack positions across client sessions. Defaults to true.

* `setKeepAlive` : Configures the Keep Alive timer in seconds. Defines the maximum time 
  interval between messages received from a client. It enables the server to detect that the 
  network connection to a client has dropped, without having to wait for the long TCP/IP timeout. 

* `setUserName` : Sets the user name used to authenticate against the server. 

* `setPassword` : Sets the password used to authenticate against the server. 

* `setWillTopic`: If set the server will publish the client's Will 
  message to the specified topics if the client has an unexpected 
  disconnection.

* `setWillMessage`:  The Will message to send. Defaults to a zero length message.

* `setWillQos` : Sets the quality of service to use for the Will message.  Defaults
  to QoS.AT_MOST_ONCE.

* `setWillRetain`: Set to true if you want the Will to be published with the retain 
  option.

### Controlling Connection Reconnects

Connection will automatically reconnect and re-establish messaging session
if any network error occurs.  You can control how often the reconnect
is attempted and define maximum number of attempts of reconnects using
the following methods:

* `setConnectAttemptsMax` : The maximum number of reconnect attempts before an error 
  is reported back to the client on the first attempt by the client to connect to a server. Set
  to -1 to use unlimited attempts.  Defaults to -1.
* `setReconnectAttemptsMax` : The maximum number of reconnect attempts before an error 
  is reported back to the client after a server connection had previously been established. Set
  to -1 to use unlimited attempts.  Defaults to -1.
* `setReconnectDelay` : How long to wait in ms before the first reconnect 
   attempt. Defaults to 10.
* `setReconnectDelayMax` : The maximum amount of time in ms to wait between 
   reconnect attempts.  Defaults to 30,000.
* `setReconnectBackOffMultiplier` : The Exponential backoff be used between reconnect 
  attempts. Set to 1 to disable exponential backoff. Defaults to 2.

### Configuring Socket Options

You can adjust some socket options by using the following methods:

* `setReceiveBufferSize` : Sets the size of the internal socket receive 
   buffer.  Defaults to 65536 (64k)

* `setSendBufferSize` : Sets the size of the internal socket send buffer.  
   Defaults to 65536 (64k)

* `setTrafficClass` : Sets traffic class or type-of-service octet in the IP 
  header for packets sent from the transport.  Defaults to `8` which
  means the traffic should be optimized for throughput.

### Throttling Connections

If you want slow down the read or write rate of your connections, use 
the following methods:

* `setMaxReadRate` : Sets the maximum bytes per second that this transport will
  receive data at.  This setting throttles reads so that the rate is not exceeded.
  Defaults to 0 which disables throttling.

* `setMaxWriteRate` : Sets the maximum bytes per second that this transport will
  send data at.  This setting throttles writes so that the rate is not exceeded.
  Defaults to 0 which disables throttling.

### Using SSL connections

If you want to connect over SSL/TLS instead of TCP, use an "ssl://" or
"tls://" URI prefix instead of "tcp://" for the `host` field. For finer
grained control of which algorithm is used. Supported protocol values are:

* `ssl://`    - Use the JVM default version of the SSL algorithm.
* `sslv*://`  - Use a specific SSL version where `*` is a version
  supported by your JVM.  Example: `sslv3`
* `tls://`    - Use the JVM default version of the TLS algorithm.
* `tlsv*://`  - Use a specific TLS version where `*` is a version
  supported by your JVM.  Example: `tlsv1.1`

The client will use the default JVM `SSLContext` which is configured via JVM
system properties unless you configure the MQTT instance using the
`setSslContext` method.

SSL connections perform blocking operations against internal thread pool
unless you call the `setBlockingExecutor` method to configure that executor
they will use instead.

### Selecting the Dispatch Queue

A [HawtDispatch](http://hawtdispatch.fusesource.org/) dispatch queue is used
to synchronize access to the connection. If an explicit queue is not
configured via the `setDispatchQueue` method, then a new queue will be created
for the connection. Setting an explicit queue might be handy if you want
multiple connection to share the same queue for synchronization.

## Using the Blocking API

The `MQTT.connectBlocking` method establishes a connection and provides you a connection
with an blocking API.

    BlockingConnection connection = mqtt.blockingConnection();
    connection.connect();

Publish messages to a topic using the `publish` method:

    connection.publish("foo", "Hello".toBytes(), QoS.AT_LEAST_ONCE, false);

You can subscribe to multiple topics using the the `subscribe` method:
    
    Topic[] topics = {new Topic("foo", QoS.AT_LEAST_ONCE)};
    byte[] qoses = connection.subscribe(topics);

Then receive and acknowledge consumption of messages using the `receive`, and `ack`
methods:
    
    Message message = connection.receive();
    System.out.println(message.getTopic());
    byte[] payload = message.getPayload();
    // process the message then:
    message.ack();

Finally to disconnect:

    connection.disconnect();

## Using the Future based API

The `MQTT.connectFuture` method establishes a connection and provides you a connection
with an futures style API.  All operations against the connection are non-blocking and
return the result via a Future.

    FutureConnection connection = mqtt.futureConnection();
    Future<Void> f1 = connection.connect();
    f1.await();

    Future<byte[]> f2 = connection.subscribe(new Topic[]{new Topic(utf8("foo"), QoS.AT_LEAST_ONCE)});
    byte[] qoses = f2.await();

    // We can start future receive..
    Future<Message> receive = connection.receive();

    // send the message..
    Future<Void> f3 = connection.publish("foo", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false);

    // Then the receive will get the message.
    Message message = receive.await();
    message.ack();
    
    Future<Void> f4 connection.disconnect();
    f4.await();


## Using the Callback/Continuation Passing based API

The `MQTT.connectCallback` method establishes a connection and provides you a connection with
an callback style API. This is the most complex to use API style, but can provide the best
performance. The future and blocking APIs use the callback api under the covers. All
operations on the connection are non-blocking and results of an operation are passed to
callback interfaces you implement.

Example:

    final CallbackConnection connection = mqtt.callbackConnection();
    connection.listener(new Listener() {
      
        public void onDisconnected() {
        }
        public void onConnected() {
        }

        public void onSuccess(UTF8Buffer topic, Buffer payload, Runnable ack) {
            // You can now process a received message from a topic.
            // Once process execute the ack runnable.
            ack.run();
        }
        public void onFailure(Throwable value) {
            connection.close(null); // a connection failure occured.
        }
    })
    connection.connect(new Callback<Void>() {
        public void onFailure(Throwable value) {
            result.failure(value); // If we could not connect to the server.
        }
  
        // Once we connect..
        public void onSuccess(Void v) {
        
            // Subscribe to a topic
            Topic[] topics = {new Topic("foo", QoS.AT_LEAST_ONCE)};
            connection.subscribe(topics, new Callback<byte[]>() {
                public void onSuccess(byte[] qoses) {
                    // The result of the subcribe request.
                }
                public void onFailure(Throwable value) {
                    connection.close(null); // subscribe failed.
                }
            });

            // Send a message to a topic
            connection.publish("foo", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
                public void onSuccess(Void v) {
                  // the pubish operation completed successfully.
                }
                public void onFailure(Throwable value) {
                    connection.close(null); // publish failed.
                }
            });
            
            // To disconnect..
            connection.disconnect(new Callback<Void>() {
                public void onSuccess(Void v) {
                  // called once the connection is disconnected.
                }
                public void onFailure(Throwable value) {
                  // Disconnects never fail.
                }
            });
        }
    });

Every connection has a [HawtDispatch](http://hawtdispatch.fusesource.org/) dispatch queue
which it uses to process IO events for the socket. The dispatch queue is an Executor that
provides serial execution of IO and processing events and is used to ensure synchronized
access of connection.

The callbacks will be executing the dispatch queue associated with the connection so
it safe to use the connection from the callback but you MUST NOT perform any blocking
operations within the callback. If you need to perform some processing which MAY block, you
must send it to another thread pool for processing. Furthermore, if another thread needs to
interact with the connection it can only do it by using a Runnable submitted to the
connection's dispatch queue.

Example of executing a Runnable on the connection's dispatch queue:

    connection.getDispatchQueue().execute(new Runnable(){
        public void run() {
          connection.publish( ..... );
        }
    });
