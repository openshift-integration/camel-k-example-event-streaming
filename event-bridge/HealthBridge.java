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

    public static class Data {
        public static class User {
            private String name;
            private String token;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getToken() {
                return token;
            }

            public void setToken(String token) {
                this.token = token;
            }
        }

        public static class Report {
            private String type;
            private String measurement;
            private boolean alert;
            private String location;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getMeasurement() {
                return measurement;
            }

            public void setMeasurement(String measurement) {
                this.measurement = measurement;
            }

            public boolean isAlert() {
                return alert;
            }

            public void setAlert(boolean alert) {
                this.alert = alert;
            }

            public String getLocation() {
                return location;
            }

            public void setLocation(String location) {
                this.location = location;
            }
        }

        private User user;
        private Report report;

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public Report getReport() {
            return report;
        }

        public void setReport(Report report) {
            this.report = report;
        }
    }

    public static class Alert {
        private String text;
        private String severity;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }

    public void configure() throws Exception {
        final String unsafeHeader = "unsafe";
        final String locationHeader = "location";

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
