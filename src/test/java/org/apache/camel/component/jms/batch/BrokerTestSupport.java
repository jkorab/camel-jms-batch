package org.apache.camel.component.jms.batch;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.usage.MemoryUsage;
import org.apache.activemq.usage.SystemUsage;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author jkorab
 */
public class BrokerTestSupport extends CamelTestSupport {

    private final static long MB = 1024 * 1024;
    private static BrokerService brokerService;

    @BeforeClass
    public static void startBroker() throws Exception {
        brokerService = new BrokerService();
        brokerService.setBrokerId("localhost");
        brokerService.setBrokerName("localhost");
        brokerService.setPersistent(false);
        SystemUsage systemUsage = new SystemUsage();
        {
            MemoryUsage memoryUsage = new MemoryUsage();
            memoryUsage.setLimit(32 * MB);
            systemUsage.setMemoryUsage(memoryUsage);
        }
        brokerService.setSystemUsage(systemUsage);
        brokerService.start();
    }

    @AfterClass
    public static void stopBroker() throws Exception {
        brokerService.stop();
    }

}
