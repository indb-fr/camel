package org.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Watch implements Processor {
    private final static Logger LOGGER = LogManager.getLogger(Watch.class);
    @Override
    public void process(Exchange exchange) throws Exception {
        var exchangeId = exchange.getExchangeId();
        var toEndPoint = exchange.getProperty(Exchange.TO_ENDPOINT);
        var fromRouteId = exchange.getFromRouteId();
        var body = exchange.getIn().getBody();
        LOGGER.info("900 - {} -> {} -> {} -> {}", exchangeId, toEndPoint, fromRouteId, body);
        exchange.getMessage().setBody(exchange.getIn().getBody());
    }
}

