package org.example.camel;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Service;

@Service
public class CustomBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        var timer = "timer:scheduler";
//        var route1 = "seda:route-1";
//        var route2 = "seda:route-2";
        var route1 = "direct:route-1";
        var route2 = "direct:route-2";
       
        from(timer + "?period=10000").routeId(timer)
                .setBody(simple("TIMER"))
                .log(LoggingLevel.INFO, "logging-route","000 - ${exchangeId} -> ${routeId} -> ${body}")
                .to(route1);

        from(route1).routeId(route1)
                .setBody(simple("${body} ROUTE1"))
                .process(new Watch())
                .log(LoggingLevel.INFO, "logging-route","100 - ${exchangeId} -> ${routeId} -> ${body}")                
                .to(route2);

        from(route2).routeId(route2)
                .setBody(simple("${body} ROUTE2"))
                .log(LoggingLevel.INFO, "logging-route","200 - ${exchangeId} -> ${routeId} -> ${body}")
                .process(new Watch())
                .log(LoggingLevel.INFO, "logging-route","...");

        var route3 = "seda:route-3";
        var route4 = "seda:route-4";
        var route5 = "seda:route-5";


    }
}
