/*
 * Copyright 2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package io.fabric8.funktion.runtime.designer;

import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.camel.RuntimeExchangeException;
import org.apache.camel.support.RoutePolicySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleMessageRoutePolicy extends RoutePolicySupport {

    private static final transient Logger LOG = LoggerFactory.getLogger(SingleMessageRoutePolicy.class);

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {
        super.onExchangeBegin(route, exchange);

        LOG.info("Exchange Begin for route " + route.getId() +
                " exchange: " + exchange.getExchangeId());
    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {
        super.onExchangeDone(route, exchange);

        LOG.info("Exchange Done for route " + route.getId() +
                " exchange: " + exchange.getExchangeId() + " in: " + exchange.getIn().getBody(String.class));

        stopCurrentRouteAsync(route);
    }

    /**
     * Allows to stop a route asynchronously using a separate background thread which can allow any current in-flight exchange
     * to complete while the route is being shutdown.
     * You may attempt to stop a route from processing an exchange which would be in-flight and therefore attempting to stop
     * the route will defer due there is an inflight exchange in-progress. By stopping the route independently using a separate
     * thread ensures the exchange can continue process and complete and the route can be stopped.
     */
    // TODO: in Camel 2.19 there is a stopRouteAsync method we can use
    private void stopCurrentRouteAsync(final Route route) {
        String threadId = route.getRouteContext().getCamelContext().getExecutorServiceManager().resolveThreadName("StopRouteAsync");
        Runnable task = () -> {
            try {
                route.getRouteContext().getCamelContext().stopRoute(route.getId());
            } catch (Exception e) {
                handleException(e);
            }
        };
        new Thread(task, threadId).start();
    }


}
