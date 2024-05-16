package com.broadcom.tanzulabs.tcpdemo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@SpringIntegrationTest( noAutoStartup = { "client1Adapter", "client2Adapter" } )
@DirtiesContext
class TcpDemoApplicationTests {

	private static final Logger log = LoggerFactory.getLogger( TcpDemoApplicationTests.class );

	@Test
	void contextLoads() {
	}

}
