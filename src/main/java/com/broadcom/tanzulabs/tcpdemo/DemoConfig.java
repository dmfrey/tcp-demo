package com.broadcom.tanzulabs.tcpdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableIntegration
public class DemoConfig {

    private static Logger log = LoggerFactory.getLogger( DemoConfig.class );

    @Bean
    MessageChannel inputChannel() {

        return new DirectChannel();
    }

    @Bean
    IntegrationFlow splitMultipleXmls( MessageChannel inputChannel, HeaderValueRouter headerRouter ) {
        return IntegrationFlow.from( inputChannel )
                .handle( String.class, (p, h) -> p.replaceAll( "<\\?xml version=\"1\\.0\" encoding=\"UTF-8\"\\?>", "" ) )
                .handle( String.class, (p, h) -> p.replaceAll( "<\\?xml version=\"1\\.0\"\\?>", "" ) )
                .handle( String.class, (p, h) -> p.split( "</event>" ) )
                .split()
                .handle( String.class, (p, h) -> p.trim() )
                .filter( String.class, source -> !source.isBlank() )
                .handle( String.class, (p, h) -> p + "</event>" )
                .log( message -> {
                    log.info( "Message: [{}]", message.getPayload() );
                    return message;
                })
                .enrichHeaders( h -> h.headerExpression("type", "#xpath(payload, '/event/@type')" ) )
                .route( headerRouter )
                .get();
    }

    @Bean
    MessageChannel discardChannel() {

        return new NullChannel();
    }

    @Bean
    public HeaderValueRouter headerRouter() {

        HeaderValueRouter router = new HeaderValueRouter( "type" );
        router.setResolutionRequired( false );
        router.setChannelMapping("abc", "abcChannel" );
//        router.setChannelMapping("def", "discardChannel" );
        router.setDefaultOutputChannelName( "discardChannel" );

        return router;
    }

    @Bean
    MessageChannel replyChannel() {

        return new DirectChannel();
    }

    @Bean
    MessageChannel abcChannel() {

        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow abcHandler( MessageChannel abcChannel, MessageChannel replyChannel ) {

        return IntegrationFlow.from( abcChannel )
                .channel( replyChannel )
                .get();
    }

}
