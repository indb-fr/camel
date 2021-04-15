package org.example.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomBuilder extends RouteBuilder {

    private final static String LOG_PATTERN = " - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}";
    private static final String LOG_NAME = "logging-route";
    private static final String CORRELATION_HEADER = "custom.correlation-header";

    @Override
    public void configure() throws Exception {
        recipientListWithSedaAndAggregate();
        // recipientListWithDirect();
    }

    private void recipientListWithSedaAndAggregate() {
        var timer = "timer:scheduler";
        var dispatch = "seda:dispatch";
        var route1 = "seda:route-1";
        var route2 = "seda:route-2";
        var route3 = "seda:route-3";        
        var recipients = String.format("%s,%s,%s",route1,route2,route3);
        var aggregate = "seda:aggregate";

        from(timer + "?period=100").routeId(timer)
                .setHeader(CORRELATION_HEADER, simple("${exchangeId}"))
                .setBody(simple("TIMER ${random(100,201)}"))
                .log(LoggingLevel.INFO, LOG_NAME,"TIM" + LOG_PATTERN)
                // aggréger les messages par blocs de 10 puis dispatcher
                .to(dispatch);

        from(dispatch).routeId(dispatch)
                .recipientList(simple(recipients))
                .log(LoggingLevel.INFO, LOG_NAME,"010" + LOG_PATTERN)
                .end();

        from(route1).routeId(route1)
                .setBody(simple("${body} ROUTE1"))
                .log(LoggingLevel.INFO, LOG_NAME,"100" + LOG_PATTERN)
                .process(new CustomProcessor())
                .to(aggregate);

        from(route2).routeId(route2)
                .setBody(simple("${body} ROUTE2"))
                .log(LoggingLevel.INFO, LOG_NAME,"200" + LOG_PATTERN)
                .process(new CustomProcessor())
                .to(aggregate);

        from(route3).routeId(route3)
                .setBody(simple("${body} ROUTE3"))
                .log(LoggingLevel.INFO, LOG_NAME,"300" + LOG_PATTERN)
                .process(new CustomProcessor())
                .to(aggregate);

        from(aggregate).routeId(aggregate)
                .aggregate(header(CORRELATION_HEADER), new CustomAggregationStrategy2()).completionSize(3)
                .log(LoggingLevel.INFO, LOG_NAME,"AGG" + LOG_PATTERN);

    }    
    private void recipientListWithDirect() {
        var timer = "timer:scheduler";
        var dispatch = "direct:dispatch";
//        var route1 = "seda:route-1";
//        var route2 = "seda:route-2";
//        var route3 = "seda:route-3";        
        var route1 = "direct:route-1";
        var route2 = "direct:route-2";
        var route3 = "direct:route-3";
        var recipients = String.format("%s,%s,%s",route1,route2,route3);


        from(timer + "?period=1000000").routeId(timer)
                .setBody(simple("TIMER"))
                .log(LoggingLevel.INFO, LOG_NAME,"000" + LOG_PATTERN)
                .to(dispatch);

        from(dispatch).routeId(dispatch)
                .recipientList(simple(recipients))
                .aggregationStrategy(new CustomAggregationStrategy1()).parallelProcessing()
                .log(LoggingLevel.INFO, LOG_NAME,"010" + LOG_PATTERN)
                .end();

        from(route1).routeId(route1)
                .setBody(simple("${body} ROUTE1"))
                .log(LoggingLevel.INFO, LOG_NAME,"100" + LOG_PATTERN)
                .process(new CustomProcessor());

        from(route2).routeId(route2)
                .setBody(simple("${body} ROUTE2"))
                .log(LoggingLevel.INFO, LOG_NAME,"200" + LOG_PATTERN)
                .process(new CustomProcessor());

        from(route3).routeId(route3)
                .setBody(simple("${body} ROUTE3"))
                .log(LoggingLevel.INFO, LOG_NAME,"300" + LOG_PATTERN)
                .process(new CustomProcessor());
        
    }
    
    public class CustomAggregationStrategy1 implements AggregationStrategy {
        
        private final Logger LOGGER = LogManager.getLogger(CustomAggregationStrategy1.class);
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            var exchangeId = newExchange.getExchangeId();
            var toEndPoint = newExchange.getProperty(Exchange.TO_ENDPOINT);
            var fromRouteId = newExchange.getFromRouteId();
            LOGGER.info("800 - {} -> {} -> {}", exchangeId, toEndPoint, fromRouteId);
            
            if (oldExchange == null) {
                return newExchange;
            }
            var body = oldExchange.getIn().getBody(String.class);
            oldExchange.getIn().setBody(body + " * " + newExchange.getIn().getBody(String.class));
            return oldExchange;

        }
    }

    public class CustomAggregationStrategy2 implements AggregationStrategy {

        private final Logger LOGGER = LogManager.getLogger(CustomAggregationStrategy1.class);

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            var exchangeId = newExchange.getExchangeId();
            var toEndPoint = newExchange.getProperty(Exchange.TO_ENDPOINT);
            var fromRouteId = newExchange.getFromRouteId();
            LOGGER.info("800 - {} -> {} -> {}", exchangeId, toEndPoint, fromRouteId);
            
            Object newBody = newExchange.getIn().getBody();
            ArrayList<Object> list = null;
            if (oldExchange == null) {
                list = new ArrayList<>();
                list.add(newBody);
                newExchange.getIn().setBody(list);
                return newExchange;
            } else {
                list = oldExchange.getIn().getBody(ArrayList.class);
                list.add(newBody);
                return oldExchange;
            }
        }        
    }
    
}
