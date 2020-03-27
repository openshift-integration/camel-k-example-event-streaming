// camel-k: language=java

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HealthBridge extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(HealthBridge.class);

    @PropertyInject("messaging.broker.url")
    private String messagingBrokerUrl;

    final String unsafeHeader = "unsafe";
    final String locationHeader = "location";

    public void configure() throws Exception {

        Sjms2Component sjms2Component = new Sjms2Component();
        sjms2Component.setConnectionFactory(new ActiveMQConnectionFactory(messagingBrokerUrl));
        getContext().addComponent("sjms2", sjms2Component);

        JacksonDataFormat dataFormat  = new JacksonDataFormat();
        dataFormat.setUnmarshalType(Data.class);

        from("kafka:health-data?brokers={{kafka.bootstrap.address}}")
                .unmarshal(dataFormat)
                .process(exchange -> {
                    Data eventData = exchange.getMessage().getBody(Data.class);

                    Alert alert = new Alert();

                    if (eventData.getReport().isAlert()) {
                        exchange.getMessage().setHeader(unsafeHeader, true);
                        alert.setSeverity("red");
                    } else {
                        alert.setSeverity("yellow");
                    }

                    String text = String.format("There is a %s incident on %s", eventData.getReport().getMeasurement(),
                        eventData.getReport().getLocation());

                    alert.setText(text);
                    

                    ObjectMapper mapper = new ObjectMapper();
                    String body = mapper.writeValueAsString(alert);

                    exchange.getMessage().setBody(body);
                    exchange.getMessage().setHeader(locationHeader, eventData.getReport().getLocation());
                })
                .streamCaching()
                .wireTap("direct:timeline")
                .choice()
                    .when(header(unsafeHeader).isEqualTo(true))
                        .to("sjms2://queue:alarms?ttl={{messaging.ttl.alarms}}")
                    .otherwise()
                        .to("sjms2://queue:notifications?ttl={{messaging.ttl.notifications}}");

        from("direct:timeline")
                .log("${body}")
                .to("kafka:timeline-data?brokers={{kafka.bootstrap.address}}");


    }
}
