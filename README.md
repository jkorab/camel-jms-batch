To use the JMS batch component, instantiate a `ConnectionFactory`:

    <bean id="connectionFactory" class="..."/>

Instantiate an `AggegationStrategy`:

    <bean id="myAggregationStrategy" class="..."/>

Instantiate the component:

    <bean id="jmsBatch" class="JmsBatchComponent">
        <property name="connectionFactory" ref="connectionFactory"/>
    </bean>

Then use it within an consumer endpoint as follows:

    <from uri="jmsBatch:{{destinationName}}?aggregationStrategy=#myAggregationStrategy"/>

The following attributes are optional:

* `consumerCount` - the number of concurrent JMS consumers (default 1) 
* `completionSize` - the size of the batch (default 200)
* `completionTimeout` - time since receipt of the first message in the batch in ms (default 500)
* `pollDuration` - max wait duration in ms of each poll for messages (default 1000). `completionTimeOut` will be used if it 
is shorter and a batch has started.

No transactional configuration is necessary, as the component uses local transactions directly via the JMS API.