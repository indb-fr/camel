package org.example.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class CustomBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        var timer = "timer:scheduler";
        var route1 = "seda:route-1";
        var route2 = "seda:route-2";
//        var route1 = "direct:route-1";
//        var route2 = "direct:route-2";

        from(timer + "?period=1000000").routeId(timer)
                .setBody(simple("TIMER"))
                .log(LoggingLevel.INFO, "logging-route","000 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .recipientList(simple(route1 + "," + route2))
                .aggregationStrategy(new CustomAggregationStrategy()).parallelProcessing().timeout(10000)
                //.aggregate(new CustomAggregationStrategy()).body().completionSize(3)
                .log(LoggingLevel.INFO, "logging-route","000 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .end();
                

        from(route1).routeId(route1)
                .setBody(simple("${body} ROUTE1"))
                .log(LoggingLevel.INFO, "logging-route","100 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .process(new Watch());

        from(route2).routeId(route2)
                .setBody(simple("${body} ROUTE2"))
                .log(LoggingLevel.INFO, "logging-route","200 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .process(new Watch())
                .log(LoggingLevel.INFO, "logging-route","...");

    }
    
    public void configure1() throws Exception {

        var timer = "timer:scheduler";
        var route1 = "seda:route-1";
        var route2 = "seda:route-2";
//        var route1 = "direct:route-1";
//        var route2 = "direct:route-2";
       
        from(timer + "?period=10000").routeId(timer)
                .setBody(simple("TIMER"))
                .log(LoggingLevel.INFO, "logging-route","000 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .to(route1);

        from(route1).routeId(route1)
                .setBody(simple("${body} ROUTE1"))
                .log(LoggingLevel.INFO, "logging-route","100 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .process(new Watch())                
                .to(route2);

        from(route2).routeId(route2)
                .setBody(simple("${body} ROUTE2"))
                .log(LoggingLevel.INFO, "logging-route","200 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .process(new Watch())
                .log(LoggingLevel.INFO, "logging-route","...");

    }

    public class CustomAggregationStrategy implements AggregationStrategy {

        private final Logger LOGGER = LogManager.getLogger(CustomAggregationStrategy.class);

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            var exchangeId = newExchange.getExchangeId();
            var toEndPoint = newExchange.getProperty(Exchange.TO_ENDPOINT);
            var fromRouteId = newExchange.getFromRouteId();
            var body = newExchange.getIn().getBody();
            LOGGER.info("800 - {} -> {} -> {} -> {}", exchangeId, toEndPoint, fromRouteId, body);

            Message newIn = newExchange.getIn();
            String aggregator = null;
            if (oldExchange == null) {
                aggregator = newIn.getBody(String.class);
                newExchange.getMessage().setBody(aggregator);
                return newExchange;
            }
            aggregator = oldExchange.getIn().getBody(String.class);
            aggregator += newIn.getBody(String.class);
            oldExchange.getMessage().setBody(aggregator);
            return oldExchange;
        }

        @Override
        public void onCompletion(Exchange exchange) {
            var uri = exchange.getProperty(Exchange.RECIPIENT_LIST_ENDPOINT, String.class);
            var camelCorrelationId = (String)exchange.getProperty("CamelCorrelationId");
            LOGGER.info("onCompletion() -> CamelCorrelationId : {} {}", camelCorrelationId, uri);
        }
    }
    
}
