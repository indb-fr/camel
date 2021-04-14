package org.example.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Service;

@Service
public class CustomBuilder extends RouteBuilder {

    private final static String LOG_PATTERN = " - ${exchangeId} -> ${routeId} -> ${exchange.getFromRouteId} -> ${body}";
    private static final String LOG_NAME = "logging-route";

    @Override
    public void configure() throws Exception {

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
                .aggregationStrategy(new CustomAggregationStrategy()).parallelProcessing()
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
    
    public class CustomAggregationStrategy implements AggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }
            var body = oldExchange.getIn().getBody(String.class);
            oldExchange.getIn().setBody(body + " * " + newExchange.getIn().getBody(String.class));
            return oldExchange;

        }
    }
    
}
