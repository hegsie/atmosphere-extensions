/*
 * Copyright 2013 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.samples.server;

import org.atmosphere.config.service.Get;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Post;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.gwt20.server.GwtRpcInterceptor;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.interceptor.BroadcastOnPostAtmosphereInterceptor;
import org.atmosphere.interceptor.SuspendTrackerInterceptor;

import java.util.logging.Logger;

/**
 * Super simple managed echo application that use two broadcaster for pushing data back to the client.
 *
 * @author Jeanfrancois Arcand
 */
@ManagedService(path = "/GwtRpcDemo/atmosphere/rpc", interceptors = {
        /**
         * Handle lifecycle for us
         */
        AtmosphereResourceLifecycleInterceptor.class,
        /**
         * Serialize/Deserialize GWT message for us
         */
        GwtRpcInterceptor.class,
        /**
         * Make sure our {@link AtmosphereResourceEventListener#onSuspend} is only called once for transport
         * that reconnect on every requests.
         */
        SuspendTrackerInterceptor.class,
        /**
         * Deserialize the GWT message
         */
        RPCEventDeserializerInterceptor.class,
        /**
         * Echo the messages we are receiving from the client either as w WebSocket message or an HTTP Post.
         */
        BroadcastOnPostAtmosphereInterceptor.class
})
public class ManagedGWTResource {

    static final Logger logger = Logger.getLogger("AtmosphereHandler");

    @Get
    public void get(final AtmosphereResource ar) {
        /**
         * For demonstration purpose, we add callback for when the client connect and disconnect.
         */
        ar.addEventListener(new AtmosphereResourceEventListenerAdapter() {
            @Override
            public void onSuspend(AtmosphereResourceEvent event) {
                logger.info("Received RPC GET");
                // Look up a new Broadcaster used for pushing who is connected.
                BroadcasterFactory.getDefault().lookup("Connected users", true).addAtmosphereResource(ar)
                        .broadcast("Browser UUID: " + ar.uuid() + " connected.");
            }

            @Override
            public void onDisconnect(AtmosphereResourceEvent event) {
                // isCancelled == true. means the client didn't send the close event, so an unexpected network glitch or browser
                // crash occurred.
                if (event.isCancelled()) {
                    logger.info("User:" + event.getResource().uuid() + " unexpectedly disconnected");
                } else if (event.isClosedByClient()) {
                    logger.info("User:" + event.getResource().uuid() + " closed the connection");
                }
            }
        });
    }

    @Post
    public void post(AtmosphereResource r) {
        // Don't need to do anything, the interceptor took care of it for us.
        logger.info("POST received with transport + " + r.transport());
    }

}
