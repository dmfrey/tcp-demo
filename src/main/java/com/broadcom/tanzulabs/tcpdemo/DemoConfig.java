package com.broadcom.tanzulabs.tcpdemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.xml.splitter.XPathMessageSplitter;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableIntegration
public class DemoConfig {

    @Bean
    IntegrationFlow splitMultiple( MessageChannel splitMultipleInputChannel, MessageChannel splitMultipleOutputChannel ) {
        return IntegrationFlow.from( splitMultipleInputChannel )
                .handle( String.class, (p, h) -> p.replaceAll( "<\\?xml version=\"1\\.0\" encoding=\"UTF-8\"\\?>", "" ) )
                .handle( String.class, (p, h) -> p.replace( '\n', ' ' ) )
                .handle( String.class, (p, h) -> p.split( "<\\?xml version=\"1\\.0\"\\?>" ) )
                .channel( splitMultipleOutputChannel )
                .get();
    }

    @Bean
    MessageChannel splitMultipleInputChannel() {

        return new DirectChannel();
    }

    @Bean
    MessageChannel splitMultipleOutputChannel() {

        return new DirectChannel();
    }

    @Bean
    @EndpointId( "xml-output-endpoint" )
    @ServiceActivator( inputChannel = "splitMultipleOutputChannel" )
    public XPathMessageSplitter xPathMessageSplitter() {

        var xPathMessageSplitter = new XPathMessageSplitter( "/" );
        xPathMessageSplitter.setCreateDocuments( true );

        return xPathMessageSplitter;
    }

}
