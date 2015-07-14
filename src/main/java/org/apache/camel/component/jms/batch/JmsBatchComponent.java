package org.apache.camel.component.jms.batch;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.util.ObjectHelper;

import javax.jms.ConnectionFactory;
import java.util.Map;

/**
 * @author jkorab
 */
public class JmsBatchComponent extends UriEndpointComponent {

    private ConnectionFactory connectionFactory;

    public JmsBatchComponent() {
        super(JmsBatchEndpoint.class);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        ObjectHelper.notNull(connectionFactory, "connectionFactory is null");
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
