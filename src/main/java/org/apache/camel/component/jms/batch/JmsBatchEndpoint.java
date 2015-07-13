package org.apache.camel.component.jms.batch;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;

/**
 * @author jkorab
 */
@UriEndpoint(scheme = "sjmsBatch", title = "Simple JMS Batch Component", syntax = "sjms-batch:destinationName?aggregationStrategy=#aggStrategy", consumerClass = JmsBatchComponent.class, label = "messaging")
public class JmsBatchEndpoint extends DefaultEndpoint {


    public static final int DEFAULT_COMPLETION_SIZE = 200; // the default dispatch queue size in ActiveMQ
    public static final int DEFAULT_COMPLETION_TIMEOUT = 500;
    public static final String PROPERTY_BATCH_SIZE = "CamelSjmsBatchSize";

    @UriPath
    @Metadata(required = "true")
    private String destinationName;

    @UriParam(label = "consumer", defaultValue = "1", description = "The number of concurrent consumers per JMS Session")
    private int concurrentConsumers = 1;

    @UriParam(label = "consumer", defaultValue = "1", description = "The number of JMS sessions to consume from")
    private int jmsConsumers = 1;

    @UriParam(label = "consumer", defaultValue = "200",
            description = "The number of messages consumed at which the batch will be completed")
    private Integer completionSize = DEFAULT_COMPLETION_SIZE;

    @UriParam(label = "consumer", defaultValue = "500",
            description = "The timeout from receipt of the first first message when the batch will be completed")
    private Integer completionTimeout = DEFAULT_COMPLETION_TIMEOUT;

    @Metadata(required = "true")
    @UriParam(label = "consumer", description = "A #-reference to an AggregationStrategy visible to Camel")
    private AggregationStrategy aggregationStrategy;

    public JmsBatchEndpoint() {}

    public JmsBatchEndpoint(String endpointUri, Component component, String remaining) {
        super(endpointUri, component);
        this.destinationName = remaining;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException("Cannot produce though a " + JmsBatchEndpoint.class.getName());
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new JmsBatchConsumer(this, processor);
    }

    public AggregationStrategy getAggregationStrategy() {
        return aggregationStrategy;
    }

    public void setAggregationStrategy(AggregationStrategy aggregationStrategy) {
        this.aggregationStrategy = aggregationStrategy;
    }

    public Integer getCompletionSize() {
        return completionSize;
    }

    public void setCompletionSize(Integer completionSize) {
        this.completionSize = completionSize;
    }

    public Integer getCompletionTimeout() {
        return completionTimeout;
    }

    public void setCompletionTimeout(Integer completionTimeout) {
        this.completionTimeout = completionTimeout;
    }

    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public int getJmsConsumers() {
        return jmsConsumers;
    }

    public void setJmsConsumers(int jmsConsumers) {
        this.jmsConsumers = jmsConsumers;
    }

}
