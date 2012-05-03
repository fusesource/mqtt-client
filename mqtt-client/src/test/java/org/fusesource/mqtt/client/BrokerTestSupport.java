/**
 * Copyright (C) 2010-2012, FuseSource Corp.  All rights reserved.
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

import junit.framework.TestCase;
import org.apache.activemq.apollo.broker.Broker;
import org.apache.activemq.apollo.broker.BrokerFactory;
import org.apache.activemq.apollo.util.ServiceControl;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class BrokerTestSupport extends TestCase {

    public int port = 1883;
    public Broker broker;

    protected Broker createBroker() throws Exception {
        URL resource = getClass().getResource("apollo-mqtt.xml");
        return BrokerFactory.createBroker(resource.toURI().toString());
    }

    @Override
    protected void setUp() throws Exception {
        if (System.getProperty("basedir") == null) {
            File file = new File(".");
            System.setProperty("basedir", file.getAbsolutePath());
        }
        broker = createBroker();
        ServiceControl.start(broker, "Starting Apollo Broker");
        this.port = ((InetSocketAddress)broker.get_socket_address()).getPort();
    }

    @Override
    protected void tearDown() throws Exception {
        if(broker!=null) {
            ServiceControl.stop(broker, "Stopped Apollo Broker");
            broker = null;
        }
    }
}
