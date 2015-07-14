package org.apache.camel.component.sjms.batch;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.util.toolbox.AggregationStrategies;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author jkorab
 */
public class SjmsBatchEndpointTest extends CamelTestSupport {

    @Rule
    public EmbeddedActiveMQBroker broker = new EmbeddedActiveMQBroker("localhost");

    @Override
    protected CamelContext createCamelContext() throws Exception {
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("aggStrategy", AggregationStrategies.groupedExchange());

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(broker.getTcpConnectorUri());

        SjmsComponent sjmsComponent = new SjmsComponent();
        sjmsComponent.setConnectionFactory(connectionFactory);

        SjmsBatchComponent sjmsBatchComponent = new SjmsBatchComponent();
        sjmsBatchComponent.setConnectionFactory(connectionFactory);

        CamelContext context = new DefaultCamelContext(registry);
        context.addComponent("jmsBatch", sjmsBatchComponent);
        context.addComponent("jms", sjmsComponent);

        return context;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jmsBatch:in?aggregationStrategy=#aggStrategy").routeId("in")
                    .to("mock:out");
            }
        };
    }

    @Test
    public void testMessageFlow() throws InterruptedException {
        MockEndpoint mockOut = getMockEndpoint("mock:out");
        mockOut.setExpectedMessageCount(1);

        template.sendBody("jms:in", "Ping");
        assertMockEndpointsSatisfied();
    }

}
