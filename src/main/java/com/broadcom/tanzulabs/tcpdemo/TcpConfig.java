package com.broadcom.tanzulabs.tcpdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.dsl.Tcp;
import org.springframework.integration.ip.dsl.TcpNetServerConnectionFactorySpec;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionCloseEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.integration.router.HeaderValueRouter;
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
        public TcpNetServerConnectionFactorySpec serverConnectionFactory( @Value( "${tcp.server.port:9876}" ) int tcpServerPort ) {
            return Tcp.netServer( tcpServerPort );
        }

        @Bean
        public IntegrationFlow inboundFlow( AbstractServerConnectionFactory serverConnectionFactory ) {

            return IntegrationFlow
                    .from( Tcp.inboundAdapter( serverConnectionFactory ) )
                    .enrichHeaders( h -> h.headerExpression("type", "#jsonPath(payload, '$.type')" ) )
                    .enrichHeaders( h -> h.headerExpression("action", "#jsonPath(payload, '$.action')" ) )
                    .transform( "#jsonPath(payload, '$.payload')" )
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
        public IntegrationFlow commandHandler( MessageChannel commandChannel, MessageChannel replyChannel ) {

            return IntegrationFlow.from( commandChannel )
//                    .transform( Transformers.fromJson( Map.class ) )
                    .handle( Map.class, (p, h) -> {
                        log.info( "handle command message : enter" );

                        log.info( "message: [{}, {}]", h, p );

                        var response = "";
                        var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                        var action = Actions.valueOf( (String) h.get( "action" ) );
                        switch( action ) {
                            case login:

                                this.clientUsernames.putIfAbsent( connectionId, (String) p.get( "username" ) );

                                response =
                                        """
                                        {
                                            'status': 'login succeeded!'
                                        }
                                        """;
                                log.info( "login user [{}, {}]", connectionId, p.get( "username" ) );

                                break;

                            case logout:

                                this.clientUsernames.remove( connectionId );

                                response =
                                        """
                                        {
                                            'status': 'logout succeeded!'
                                        }
                                        """;
                                log.info( "logout user [{}, {}]", connectionId, p.get( "username" ) );

                                break;
                        }

                        log.info( "handle command message : exit" );
                        return response;
                    })
                    .channel( replyChannel )
                    .get();
        }

        @Bean
        public IntegrationFlow chatHandler( MessageChannel chatChannel, MessageChannel replyChannel ) {

            return IntegrationFlow.from( chatChannel )
//                    .transform( Transformers.fromJson( Map.class ) )
                    .handle( Map.class, (p, h) -> {
                        log.info( "handle chat message : enter" );

                        log.info( "message: [{}, {}]", h, p );
                        log.info( "clients: [{}]", this.clientUsernames );

                        var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                        var to = (String) p.get( "to" );
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

                            var message = (String) p.get( "message" );

                            var responsePayload =
                                    """
                                    {
                                        "type": "chatResponse",
                                        "payload": {
                                            "from": "%s",
                                            "message": "%s"
                                        }
                                    }
                                    """.formatted( from.get(), message );

                            log.info( "handle chat message : exit" );
                            return new GenericMessage<>( responsePayload, Map.of( IpHeaders.CONNECTION_ID, sendToConnection ) );

                        } else {

                            log.info( "error sending chat message!" );
                            return """
                                    {
                                        'status': 'chat sent!'
                                    }
                                    """;

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
