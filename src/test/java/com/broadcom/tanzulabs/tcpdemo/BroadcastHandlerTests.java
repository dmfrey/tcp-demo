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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig( classes = TcpConfig.class )
@SpringIntegrationTest( noAutoStartup = { "reply-tcp-endpoint", "tcp-request-endpoint" } )
public class BroadcastHandlerTests {

    @Autowired
    DirectChannel broadcastChannel;

    @Autowired
    DirectChannel replyChannel;

    @MockBean
    ClientService mockClientService;

    final String fakeSenderConnectionId = "fakeSenderConnectionId";
    final String fakeSenderUsername = "fakeSenderUsername";
    final String fakeSenderMessage = "fakeSenderMessage";

    final String fakeReceiver1ConnectionId = "fakeReceiver1ConnectionId";
    final String fakeReceiver2ConnectionId = "fakeReceiver2ConnectionId";

    @Test
    void testBroadcast() {

        when( this.mockClientService.getUsername( fakeSenderConnectionId ) ).thenReturn( Optional.of( fakeSenderUsername ) );
        when( this.mockClientService.getLoggedInConnections() ).thenReturn( List.of( fakeSenderConnectionId, fakeReceiver1ConnectionId, fakeReceiver2ConnectionId ) );

        ArgumentCaptor<Message<?>> messageArgumentCaptor = MockIntegration.messageArgumentCaptor();
        MockMessageHandler mockMessageHandler =
                MockIntegration.mockMessageHandler( messageArgumentCaptor )
                        .handleNext( m -> {} );

        // Subscribe first to listen for returned messages, otherwise test will fail
        this.replyChannel.subscribe( mockMessageHandler );

        Message<Map<String, String>> fakePayload =
                MessageBuilder
                        .withPayload( Map.of( "message", fakeSenderMessage ) )
                        .setHeader( IpHeaders.CONNECTION_ID, fakeSenderConnectionId )
                        .setHeader( "action", "broadcast" )
                        .build();

        // Send the message to the Command Channel
        this.broadcastChannel.send( fakePayload );

        var expected =
            """
            <root>
                <type>chatResponse</type>
                <payload>
                    <from>%s</from>
                    <message>%s</message>
                </payload>
            </root>
            """.formatted( fakeSenderUsername, fakeSenderMessage );

        // Get the Messages from the ArgumentCaptor, extract the payload and verify its contents to `expected`
        assertThat( messageArgumentCaptor.getAllValues() )
                .map( message -> (String) message.getPayload())
                .containsExactly( expected, expected )
                .size()
                .returnToIterable()
                .hasSize( 2 );

        assertThat( messageArgumentCaptor.getAllValues() )
                .map( message -> message.getHeaders().get( IpHeaders.CONNECTION_ID ) )
                .contains( fakeReceiver1ConnectionId, fakeReceiver2ConnectionId )
                .size()
                .returnToIterable()
                .hasSize( 2 );

        verify( this.mockClientService ).getUsername( fakeSenderConnectionId );
        verify( this.mockClientService ).getLoggedInConnections();
        verifyNoMoreInteractions( this.mockClientService );

        this.replyChannel.unsubscribe( mockMessageHandler );

    }

}
