package com.broadcom.tanzulabs.tcpdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Transformers;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.dsl.TcpNetServerConnectionFactorySpec;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionCloseEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.integration.support.json.Jackson2JsonObjectMapper;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class TcpConfig {

    @Configuration
    static class ChatServer {

        private final Logger log = LoggerFactory.getLogger( ChatServer.class );

        private final Set<String> clients = ConcurrentHashMap.newKeySet();
        private final Map<String, String> clientUsernames = new ConcurrentHashMap<>();

        @Bean
        public ObjectMapper objectMapper() {

            var xmlMapper = new XmlMapper();

            return xmlMapper;
        }

        @Bean
        public Jackson2JsonObjectMapper jackson2JsonObjectMapper( ObjectMapper objectMapper ) {

            return new Jackson2JsonObjectMapper( objectMapper );
        }

        @Bean
        public TcpNetServerConnectionFactorySpec serverConnectionFactory( @Value( "${tcp.server.port:9876}" ) int tcpServerPort ) {
            return Tcp.netServer( tcpServerPort );
        }

        @Bean
        public IntegrationFlow inboundFlow( AbstractServerConnectionFactory serverConnectionFactory ) {

            return IntegrationFlow
                    .from( Tcp.inboundAdapter( serverConnectionFactory ) )
                    .enrichHeaders( h -> h.headerExpression("type", "#xpath(payload, '/root/type')" ) )
                    .enrichHeaders( h -> h.headerExpression("action", "#xpath(payload, '/root/action')" ) )
                    .route( headerRouter() )
                    .get();
        }

        @Bean
        public HeaderValueRouter headerRouter() {

            HeaderValueRouter router = new HeaderValueRouter( "type" );
            router.setResolutionRequired( false );
            router.setChannelMapping("command", "commandChannel" );
            router.setChannelMapping("chat", "chatChannel" );

            return router;
        }

        @Bean
        public MessageChannel commandChannel() {

            return new DirectChannel();
        }

        @Bean
        public MessageChannel chatChannel() {

            return new DirectChannel();
        }

        enum Actions { login, logout };

        @Bean
        public IntegrationFlow commandHandler( MessageChannel commandChannel, MessageChannel replyChannel, Jackson2JsonObjectMapper jackson2JsonObjectMapper ) {

            return IntegrationFlow.from( commandChannel )
                    .transform( Transformers.fromJson( jackson2JsonObjectMapper ) )
                    .handle( Map.class, (p, h) -> {
                        log.info( "handle command message : enter" );

                        log.info( "message: headers=[{}], payload=[{}]", h, p );
                        Map<String, String> payload = (Map<String, String>) p.get( "payload" );
                        log.info( "payload: [{}]", payload );

                        var response = "";
                        var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                        var action = Actions.valueOf( (String) h.get( "action" ) );
                        switch( action ) {
                            case login:

                                this.clientUsernames.putIfAbsent( connectionId, payload.get( "username" ) );

                                response =
                                        """
                                        <root>
                                            <status>login succeeded!</status>
                                        </root>
                                        """;
                                log.info( "login user [{}, {}]", connectionId, payload.get( "username" ) );

                                break;

                            case logout:

                                this.clientUsernames.remove( connectionId );

                                response =
                                        """
                                        <root>
                                            <status>logout succeeded!</status>
                                        </root>
                                        """;
                                log.info( "logout user [{}, {}]", connectionId, payload.get( "username" ) );

                                break;
                        }

                        log.info( "handle command message : exit" );
                        return response;
                    })
                    .channel( replyChannel )
                    .get();
        }

        @Bean
        public IntegrationFlow chatHandler( MessageChannel chatChannel, MessageChannel replyChannel, Jackson2JsonObjectMapper jackson2JsonObjectMapper ) {

            return IntegrationFlow.from( chatChannel )
                    .transform( Transformers.fromJson( jackson2JsonObjectMapper ) )
                    .handle( Map.class, (p, h) -> {
                        log.info( "handle chat message : enter" );

                        log.info( "message: [{}, {}]", h, p );
                        Map<String, String> payload = (Map<String, String>) p.get( "payload" );
                        log.info( "payload: [{}]", payload );

                        log.info( "clients: [{}]", this.clientUsernames );

                        var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                        var to = (String) payload.get( "to" );
                        var from =
                                this.clientUsernames.entrySet().stream()
                                        .filter( e -> e.getKey().equals( connectionId ) )
                                        .map( Map.Entry::getValue )
                                        .findFirst();

                        if( from.isPresent() ) {

                            var sendToConnection = this.clientUsernames.entrySet().stream()
                                    .filter( e -> e.getValue().equals( to ) )
                                    .map( Map.Entry::getKey )
                                    .findFirst()
                                    .get();

                            var message = (String) payload.get( "message" );

                            var responsePayload =
                                    """
                                    <root>
                                        <type>chatResponse</type>
                                        <payload>
                                            <from>%s</from>
                                            <message>%s</message>
                                        </payload>
                                    </root>
                                    """.formatted( from.get(), message );

                            log.info( "handle chat message : exit" );
                            return new GenericMessage<>( responsePayload, Map.of( IpHeaders.CONNECTION_ID, sendToConnection ) );

                        } else {

                            log.info( "error sending chat message!" );
                            return """
                                    <root>
                                        <type>chatResponse</type>
                                        <payload>
                                            <from>%s</from>
                                            <message>chat NOT sent!</message>
                                        </payload>
                                    </root>
                                    """.formatted( from.get() );

                        }
                    })
                    .channel( replyChannel )
                    .get();
        }

        @Bean
        @ServiceActivator( inputChannel = "replyChannel" )
        public TcpSendingMessageHandler outboundMessageHandler( AbstractServerConnectionFactory serverConnectionFactory ) {

            TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
            handler.setConnectionFactory( serverConnectionFactory );

            return handler;
        }

        @Bean
        public MessageChannel replyChannel() {
            return new DirectChannel();
        }

        @EventListener
        public void open( TcpConnectionOpenEvent event ) {

            if( !this.clients.contains( event.getConnectionId() ) ) {

                this.clients.add( event.getConnectionId() );
                log.info( "client [{}] registered!", event.getConnectionId() );

            }

        }

        @EventListener
        public void close( TcpConnectionCloseEvent event ) {

            this.clients.remove( event.getConnectionId() );
            log.info( "client [{}] unregistered!", event.getConnectionId() );

        }

    }

}
