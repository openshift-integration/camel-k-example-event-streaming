package service;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

public class UserReportSystem extends RouteBuilder {

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

    @PropertyInject("users.allowed")
    private String usersAllowed;

    public void configure() throws Exception {
        final String AUTH_HEADER = "authorized";
        final String VALID_HEADER = "valid";
        final String REPORT_TYPE_HEADER = "type";

        restConfiguration()
                .component("netty-http")
                .host("0.0.0.0")
                .port("8080");

        rest("/")
                .get("/report/list").to("direct:report-list")
                .put("/report/new").to("direct:report-new");

        from("direct:report-list")
                .transform().constant("Not implemented");

        JacksonDataFormat reportFormat = new JacksonDataFormat();
        reportFormat.setUnmarshalType(Data.class);

        from("direct:report-new")
                .streamCaching()
                .wireTap("direct:audit")
                .unmarshal(reportFormat)
                .step()
                    .to("direct:authenticate")
                    .choice()
                    .when(header(AUTH_HEADER).isEqualTo(true))
                        .to("direct:publish")
                    .end();

        from("direct:audit")
                .to("knative:channel/audit");

        from("direct:log")
                .convertBodyTo(String.class)
                .to("log:info");

        from("direct:authenticate")
            .process(new Processor() {
            public void process(Exchange exchange) throws Exception {
                String[] userList = usersAllowed.split(",");

                Data data = exchange.getMessage().getBody(Data.class);

                if (Arrays.asList(userList).contains(data.getUser().getName())) {
                    exchange.getMessage().setHeader(AUTH_HEADER, true);
                    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);

                }
                else {
                    exchange.getMessage().setHeader(AUTH_HEADER, false);
                    exchange.getMessage().setBody("Unauthorized");
                    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 401);
                }
            }
        });

        from("direct:publish")
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Data data = exchange.getMessage().getBody(Data.class);

                        Data.Report report = data.getReport();

                        if (report == null || report.getType() == null || report.getType().isEmpty()) {
                            exchange.getMessage().setHeader(VALID_HEADER, false);
                            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
                            exchange.getMessage().setBody("Invalid report data: empty or null");
                        }
                        else {
                            ObjectMapper mapper = new ObjectMapper();

                            switch (report.getType()) {
                                case "health":
                                case "crime": {
                                    exchange.getMessage().setHeader(VALID_HEADER, true);
                                    exchange.getMessage().setHeader(REPORT_TYPE_HEADER, report.getType());

                                    String body = mapper.writeValueAsString(report);
                                    exchange.getMessage().setBody(body);
                                    break;
                                }
                                default: {
                                    exchange.getMessage().setHeader(VALID_HEADER, true);
                                    exchange.getMessage().setBody("Invalid report data: unsupported report data");
                                }
                            }
                        }
                    }
                })
                .choice()
                    .when(header(VALID_HEADER).isEqualTo(false))
                        .stop()
                    .when(header(VALID_HEADER).isEqualTo(true))
                        .choice()
                            .when(header(REPORT_TYPE_HEADER).isEqualTo("crime"))
                                .to("kafka:crime-data?brokers={{kafka.bootstrap.address}}")
                                .transform().constant("OK").stop()
                            .when(header(REPORT_TYPE_HEADER).isEqualTo("health"))
                                .to("kafka:health-data?brokers={{kafka.bootstrap.address}}")
                                .transform().constant("OK");





    }
}
