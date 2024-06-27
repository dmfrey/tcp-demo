package com.broadcom.tanzulabs.tcpdemo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.integration.test.mock.MockIntegration;
import org.springframework.integration.test.mock.MockMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig( classes = DemoConfig.class )
@SpringIntegrationTest
public class DemoConfigTests {

    @Autowired
    DirectChannel inputChannel;

    @Autowired
    DirectChannel replyChannel;

    @Test
    void test() {

        ArgumentCaptor<Message<?>> messageArgumentCaptor = MockIntegration.messageArgumentCaptor();
        MockMessageHandler mockMessageHandler =
                MockIntegration.mockMessageHandler( messageArgumentCaptor )
                        .handleNext( m -> {} );

        this.replyChannel.subscribe( mockMessageHandler );

        var fakeMessage =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <event type="abc">
                    <device>ANDROID-abc123</device>
                </event><?xml version="1.0"?>
                <event type="def">
                    <device>ANDROID-abc123</device>
                </event><?xml version="1.0"?>
                <event type="def">
                    <device>ANDROID-abc123</device>
                </event><?xml version="1.0"?>
                <event type="def">
                    <device>ANDROID-abc123</device>
                </event>
                """;

        this.inputChannel.send( MessageBuilder.withPayload( fakeMessage ).build() );

        var expected =
                """
                <event type="abc">
                    <device>ANDROID-abc123</device></event>""";

        assertThat( messageArgumentCaptor.getValue().getPayload() ).isNotNull();

        var actual = messageArgumentCaptor.getValue();
        assertThat( actual.getPayload() ).isEqualTo( expected );

        this.replyChannel.unsubscribe( mockMessageHandler );

    }

}
