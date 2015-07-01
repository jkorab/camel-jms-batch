To use the JMS batch component, instantiate a `ConnectionFactory`:

    <bean id="connectionFactory" class="..."/>

Instantiate an `AggegationStrategy`:

    <bean id="myAggregationStrategy" class="..."/>

Instantiate the component:

    <bean id="jmsBatch" class="JmsBatchComponent">
        <property name="connectionFactory" ref="connectionFactory"/>
    </bean>

Then use it within an consumer endpoint as follows:

    <from uri="jmsBatch:{{queueName}}?aggregationStrategyRef=myAggregationStrategy"/>

The following attributes are optional:

* `concurrentConsumers` - the number of threads that out to process the aggregated messages
* `completionSize` - the size of the batch (default 100)
* `completionTimeout` - time since receipt of the first message in the batch in ms (default 1000)

No transactional configuration is necessary, as the component uses local transactions directly via the JMS API.