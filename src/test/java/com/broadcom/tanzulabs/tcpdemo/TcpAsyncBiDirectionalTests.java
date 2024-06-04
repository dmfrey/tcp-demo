package com.broadcom.tanzulabs.tcpdemo;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.dsl.TcpInboundChannelAdapterSpec;
import org.springframework.integration.ip.dsl.TcpNetClientConnectionFactorySpec;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
public class TcpAsyncBiDirectionalTests {

    private static final Logger log = LoggerFactory.getLogger( TcpAsyncBiDirectionalTests.class );

    @Autowired
    @Qualifier( "inboundFlow.channel#0" )
    private AbstractMessageChannel serverIn;

    @Autowired
    TcpNetClientConnectionFactory client1;

    @Autowired
    @Qualifier( "client1channel" )
    MessageChannel client1Channel;

    @Autowired
    @Qualifier( "client1Output" )
    QueueChannel client1Output;

    @Autowired
    TcpNetClientConnectionFactory client2;

    @Autowired
    @Qualifier( "client2channel" )
    MessageChannel client2Channel;

    @Autowired
    @Qualifier( "client2Output" )
    QueueChannel client2Output;

    @Test
    void testBothReceive() throws IOException, XMLStreamException {

        var client1LoginMessage =
                MessageBuilder.withPayload(
                                """
                                <root>
                                    <type>command</type>
                                    <action>login</action>
                                    <payload>
                                        <username>client1</username>
                                    </payload>
                                </root>
                                """
                        )
                        .build();

        var client1LogoutMessage =
                MessageBuilder.withPayload(
                                """
                                <root>
                                    <type>command</type>
                                    <action>logout</action>
                                    <payload>
                                        <username>client1</username>
                                    </payload>
                                </root>
                                """
                        )
                        .build();

        var client2LoginMessage =
                MessageBuilder.withPayload(
                                """
                                <root>
                                    <type>command</type>
                                    <action>login</action>
                                    <payload>
                                        <username>client2</username>
                                    </payload>
                                </root>
                                """
                        )
                        .build();

        var client2LogoutMessage =
                MessageBuilder.withPayload(
                                """
                                <root>
                                    <type>command</type>
                                    <action>logout</action>
                                    <payload>
                                        <username>client2</username>
                                    </payload>
                                </root>
                                """
                        )
                        .build();

        var client1ChatMessage =
                MessageBuilder.withPayload(
                                """
                                <root>
                                    <type>chat</type>
                                    <action>sendMessage</action>
                                    <payload>
                                        <to>client2</to>
                                        <message>Hello from client1</message>
                                    </payload>
                                </root>
                                """
                        )
                        .build();

        client1Channel.send( client1LoginMessage );
        var client1LoginReturnMessage = client1Output.receive( 10000 );
        assertThat( client1LoginReturnMessage ).isNotNull();

        client2Channel.send( client2LoginMessage );
        var client2LoginReturnMessage = client2Output.receive( 10000 );
        assertThat( client2LoginReturnMessage ).isNotNull();

        client1Channel.send( client1ChatMessage );
        var client2ChatReturnMessage = client2Output.receive( 10000 );
        assertThat( client2ChatReturnMessage ).isNotNull();
        assertThat( client2ChatReturnMessage.getPayload() ).isInstanceOf( byte[].class );

        var bytes = (byte[]) client2ChatReturnMessage.getPayload();
        var xmlInputFactory = XMLInputFactory.newFactory();
        var xmlStreamReader = xmlInputFactory.createXMLStreamReader( new ByteArrayInputStream( bytes ) );
        var mapper = new XmlMapper();
        var client2ChatReturnMessagePayload = (ChatResponse) mapper.readValue( xmlStreamReader, ChatResponse.class );
        assertThat( client2ChatReturnMessagePayload.payload.from ).isEqualTo( "client1" );
        assertThat( client2ChatReturnMessagePayload.payload.message ).isEqualTo( "Hello from client1" );

        client1Channel.send( client1LogoutMessage );
        var client1LogoutReturnMessage = client1Output.receive( 10000 );
        assertThat( client1LogoutReturnMessage ).isNotNull();

        client2Channel.send( client2LogoutMessage );
        var client2LogoutReturnMessage = client2Output.receive( 10000 );
        assertThat( client2LogoutReturnMessage ).isNotNull();

    }


    @JacksonXmlRootElement( localName = "root" )
    record ChatResponse( @JacksonXmlProperty( localName = "type" ) String type, @JacksonXmlProperty( localName = "payload" ) Payload payload ) { }

    record Payload( @JacksonXmlProperty( localName = "from" ) String from, @JacksonXmlProperty( localName = "message" ) String message ) { }

    @TestConfiguration
    static class TestConfig {

        @Bean
        TcpNetClientConnectionFactorySpec client1() {

            return Tcp.netClient( "localhost", 9876 );
        }

        @Bean
        TcpInboundChannelAdapterSpec client1Adapter(TcpNetClientConnectionFactory client1 ) {

            return Tcp.inboundAdapter( client1 ).outputChannel( "client1Output" );
        }

        @Bean
        QueueChannel client1Output() {

            return new QueueChannel();
        }

        @Bean
        @ServiceActivator( inputChannel = "client1channel" )
        public TcpSendingMessageHandler client1MessageHandler( TcpNetClientConnectionFactory client1 ) {

            TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
            handler.setConnectionFactory( client1 );

            return handler;
        }

        @Bean
        TcpNetClientConnectionFactorySpec client2() {

            return Tcp.netClient( "localhost", 9876 )
                    .mapper( new TcpMessageMapper() );
        }

        @Bean
        TcpInboundChannelAdapterSpec client2Adapter(TcpNetClientConnectionFactory client2 ) {

            return Tcp.inboundAdapter( client2 ).outputChannel( "client2Output" );
        }

        @Bean
        QueueChannel client2Output() {

            return new QueueChannel();
        }

        @Bean
        @ServiceActivator( inputChannel = "client2channel" )
        public TcpSendingMessageHandler client2MessageHandler( TcpNetClientConnectionFactory client2 ) {

            TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
            handler.setConnectionFactory( client2 );

            return handler;
        }

    }

}
