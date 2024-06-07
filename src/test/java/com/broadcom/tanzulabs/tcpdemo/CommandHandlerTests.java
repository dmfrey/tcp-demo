package com.broadcom.tanzulabs.tcpdemo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig( classes = TcpConfig.class )
@SpringIntegrationTest( noAutoStartup = { "reply-tcp-endpoint", "tcp-request-endpoint" } )
public class CommandHandlerTests {

    @Autowired
    DirectChannel commandChannel;

    @Autowired
    DirectChannel replyChannel;

    @MockBean
    ClientService mockClientService;

    final String fakeConnectionId = "fakeConnectionId";
    final String fakeUsername = "fakeUsername";

    @Test
    void testCommandLogin() {

        when( this.mockClientService.login( fakeConnectionId, fakeUsername ) ).thenReturn( true );

        ArgumentCaptor<Message<?>> messageArgumentCaptor = MockIntegration.messageArgumentCaptor();
        MockMessageHandler mockMessageHandler =
                MockIntegration.mockMessageHandler( messageArgumentCaptor )
                        .handleNext( m -> {} );

        // Subscribe first to listen for returned messages, otherwise test will fail
        this.replyChannel.subscribe( mockMessageHandler );

        Message<Map<String, String>> fakeMessage =
                MessageBuilder
                        .withPayload( Map.of( "username", fakeUsername ) )
                        .setHeader( IpHeaders.CONNECTION_ID, fakeConnectionId )
                        .setHeader( "action", "login" )
                        .build();

        // Send the message to the Command Channel
        this.commandChannel.send( fakeMessage );

        // Get the Message from the ArgumentCaptor, extract the payload and verify it's contents
        assertThat( messageArgumentCaptor.getValue().getPayload() ).isEqualTo(
                """
                {
                    'status': 'login succeeded!'
                }
                """
        );

        verify( this.mockClientService ).login( fakeConnectionId, fakeUsername );
        verifyNoMoreInteractions( this.mockClientService );

        this.replyChannel.unsubscribe( mockMessageHandler );

    }

    @Test
    void testCommandLogout() {

        when( this.mockClientService.logout( fakeConnectionId, fakeUsername ) ).thenReturn( true );

        ArgumentCaptor<Message<?>> messageArgumentCaptor = MockIntegration.messageArgumentCaptor();
        MockMessageHandler mockMessageHandler =
                MockIntegration.mockMessageHandler( messageArgumentCaptor )
                        .handleNext( m -> {} );

        // Subscribe first to listen for returned messages, otherwise test will fail
        this.replyChannel.subscribe( mockMessageHandler );

        Message<Map<String, String>> fakeMessage =
                MessageBuilder
                        .withPayload( Map.of( "username", fakeUsername ) )
                        .setHeader( IpHeaders.CONNECTION_ID, fakeConnectionId )
                        .setHeader( "action", "logout" )
                        .build();

        // Send the message to the Command Channel
        this.commandChannel.send( fakeMessage );

        // Get the Message from the ArgumentCaptor, extract the payload and verify it's contents
        assertThat( messageArgumentCaptor.getValue().getPayload() ).isEqualTo(
                """
                {
                    'status': 'logout succeeded!'
                }
                """
        );

        verify( this.mockClientService ).logout( fakeConnectionId, fakeUsername );
        verifyNoMoreInteractions( this.mockClientService );

        this.replyChannel.unsubscribe( mockMessageHandler );

    }

}
