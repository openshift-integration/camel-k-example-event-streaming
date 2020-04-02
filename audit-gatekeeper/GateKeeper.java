// camel-k: language=java

import org.apache.camel.builder.RouteBuilder;

public class GateKeeper extends RouteBuilder {

    public void configure() throws Exception {
        /**
         * This leverages the Knative channels and decouples the route from the
         * transport mechanism used to send the events. By using this, it is possible
         * to use features like scale-to-zero from Knative.
         */
        from("knative:channel/audit")
                .to("log:info?multiline=true&showBodyType=false");
    }
}
