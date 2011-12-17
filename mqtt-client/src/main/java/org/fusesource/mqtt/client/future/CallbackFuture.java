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

package org.fusesource.mqtt.client.future;

import org.fusesource.mqtt.client.callback.Callback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class CallbackFuture<T> extends Callback<T> implements Future<T> {

    private final CountDownLatch latch = new CountDownLatch(1);

    Throwable error;
    T value;

    @Override
    public void failure(Throwable value) {
        error = value;
        latch.countDown();
    }

    @Override
    public void success(T value) {
        this.value = value;
        latch.countDown();
    }

    public T await(long amount, TimeUnit unit) throws Exception {
        latch.await(amount, unit);
        return get();
    }

    public T await() throws Exception {
        latch.await();
        return get();
    }

    private T get() throws Exception {
        Throwable e = error;
        if( e !=null ) {
            if( e instanceof RuntimeException ) {
                throw (RuntimeException) e;
            } else if( e instanceof Exception) {
                throw (Exception) e;
            } else if( e instanceof Error) {
                throw (Error) e;
            } else {
                // don't expect to hit this case.
                throw new RuntimeException(e);
            }
        }
        return value;
    }
}
