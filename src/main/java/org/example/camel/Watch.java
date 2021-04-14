package org.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Watch implements Processor {
    private final static Logger LOGGER = LogManager.getLogger(Watch.class);
    @Override
    public void process(Exchange exchange) throws Exception {
        var body = exchange.getIn().getBody();
        var routeId = exchange.getFromRouteId();
        var exchangeId = exchange.getExchangeId();
        LOGGER.info("900 - {} -> {} -> {}", exchangeId, routeId, body);
        exchange.getMessage().setBody(exchange.getIn().getBody());
    }
}

