// camel-k: language=java property=file:application.properties
// camel-k: dependency=github:openshift-integration:camel-k-example-event-streaming:main-SNAPSHOT
// camel-k: config=secret:example-event-streaming-user-reporting

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.redhat.integration.common.Data;

public class UserReportSystem extends RouteBuilder {

    @PropertyInject("users.allowed")
    private String usersAllowed;

    public void configure() throws Exception {
        final String AUTH_HEADER = "authorized";
        final String VALID_HEADER = "valid";
        final String REPORT_TYPE_HEADER = "type";

        rest("/")
                .get("/report/list").to("direct:report-list")
                .put("/report/new").to("direct:report-new");

        from("direct:report-list")
                .transform().constant("Not implemented");

        from("direct:report-new")
                .streamCaching()
                .wireTap("direct:audit")
                .unmarshal().json(JsonLibrary.Jackson, Data.class)
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

                                    String body = mapper.writeValueAsString(data);
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
