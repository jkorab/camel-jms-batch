package org.apache.camel.component.sjms.batch;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.util.ObjectHelper;

import javax.jms.ConnectionFactory;
import java.util.Map;

/**
 * @author jkorab
 */
public class SjmsBatchComponent extends UriEndpointComponent {

    private ConnectionFactory connectionFactory;

    public SjmsBatchComponent() {
        super(SjmsBatchEndpoint.class);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        ObjectHelper.notNull(connectionFactory, "connectionFactory is null");
        SjmsBatchEndpoint sjmsBatchEndpoint = new SjmsBatchEndpoint(uri, this, remaining);
        setProperties(sjmsBatchEndpoint, parameters);
        return sjmsBatchEndpoint;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

}
