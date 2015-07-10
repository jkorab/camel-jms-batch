package org.apache.camel.component.jms.batch;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.commons.lang.Validate;

import javax.jms.ConnectionFactory;
import java.util.Map;

/**
 * @author jkorab
 */
public class JmsBatchComponent extends DefaultComponent {

    private ConnectionFactory connectionFactory;

    public JmsBatchComponent() {
    }

    public JmsBatchComponent(CamelContext camelContext) {
        super(camelContext);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Validate.notNull(connectionFactory);
        JmsBatchEndpoint jmsBatchEndpoint = new JmsBatchEndpoint(uri, this, remaining);
        setProperties(jmsBatchEndpoint, parameters);
        return jmsBatchEndpoint;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

}
