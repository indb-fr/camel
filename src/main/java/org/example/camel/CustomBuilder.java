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
//        var route1 = "seda:route-1";
//        var route2 = "seda:route-2";
//        var route3 = "seda:route-3";        
        var route1 = "direct:route-1";
        var route2 = "direct:route-2";
        var route3 = "direct:route-3";
        var recipients = String.format("%s,%s,%s",route1,route2,route3);

        from(timer + "?period=1000000").routeId(timer)
                .setBody(simple("TIMER"))
                .log(LoggingLevel.INFO, "logging-route","000 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .recipientList(simple(recipients))
                .aggregationStrategy(new CustomAggregationStrategy()).parallelProcessing()
                .log(LoggingLevel.INFO, "logging-route","000 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .end();

        from(route1).routeId(route1)
                .setBody(simple("${body} ROUTE1"))
                .log(LoggingLevel.INFO, "logging-route","100 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .process(new Watch());

        from(route2).routeId(route2)
                .setBody(simple("${body} ROUTE2"))
                .log(LoggingLevel.INFO, "logging-route","200 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .process(new Watch());

        from(route3).routeId(route3)
                .setBody(simple("${body} ROUTE3"))
                .log(LoggingLevel.INFO, "logging-route","300 - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}")
                .process(new Watch());
        
    }
    
    public class CustomAggregationStrategy implements AggregationStrategy {

        private final Logger LOGGER = LogManager.getLogger(CustomAggregationStrategy.class);

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }

            String body = oldExchange.getIn().getBody(String.class);
            oldExchange.getIn().setBody(body + " * " + newExchange.getIn().getBody(String.class));
            return oldExchange;

        }
    }
    
}
