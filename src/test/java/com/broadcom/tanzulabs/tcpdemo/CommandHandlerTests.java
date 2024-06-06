package com.broadcom.tanzulabs.tcpdemo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.integration.test.mock.MockIntegration;
import org.springframework.integration.test.mock.MockMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig( classes = TcpConfig.class )
@SpringIntegrationTest( noAutoStartup = { "reply-tcp-endpoint", "tcp-request-endpoint" } )
public class CommandHandlerTests {

    @Autowired
    DirectChannel commandChannel;

    @Autowired
    DirectChannel replyChannel;

    @Test
    void testCommandLogin() {

        ArgumentCaptor<Message<?>> messageArgumentCaptor = MockIntegration.messageArgumentCaptor();
        MockMessageHandler mockMessageHandler =
                MockIntegration.mockMessageHandler( messageArgumentCaptor )
                        .handleNext( m -> {} );

        // Subscribe first to listen for returned messages, otherwise test will fail
        this.replyChannel.subscribe( mockMessageHandler );

        Message<Map<String, String>> messageSource =
                MessageBuilder
                        .withPayload( Map.of( "username", "client1" ) )
                        .setHeader( IpHeaders.CONNECTION_ID, "test" )
                        .setHeader( "action", "login" )
                        .build();

        // Send the message to the Command Channel
        this.commandChannel.send( messageSource );

        // Get the Message from the ArgumentCaptor, extract the payload and verify it's contents
        assertThat( messageArgumentCaptor.getValue().getPayload() ).isEqualTo(
                """
                {
                    'status': 'login succeeded!'
                }
                """
        );

        this.replyChannel.unsubscribe( mockMessageHandler );

    }

    @Test
    void testCommandLogout() {

        ArgumentCaptor<Message<?>> messageArgumentCaptor = MockIntegration.messageArgumentCaptor();
        MockMessageHandler mockMessageHandler =
                MockIntegration.mockMessageHandler( messageArgumentCaptor )
                        .handleNext( m -> {} );

        // Subscribe first to listen for returned messages, otherwise test will fail
        this.replyChannel.subscribe( mockMessageHandler );

        Message<Map<String, String>> messageSource =
                MessageBuilder
                        .withPayload( Map.of( "username", "client1" ) )
                        .setHeader( IpHeaders.CONNECTION_ID, "test" )
                        .setHeader( "action", "logout" )
                        .build();

        // Send the message to the Command Channel
        this.commandChannel.send( messageSource );

        // Get the Message from the ArgumentCaptor, extract the payload and verify it's contents
        assertThat( messageArgumentCaptor.getValue().getPayload() ).isEqualTo(
                """
                {
                    'status': 'logout succeeded!'
                }
                """
        );

        this.replyChannel.unsubscribe( mockMessageHandler );

    }

}
