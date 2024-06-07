package com.broadcom.tanzulabs.tcpdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.EndpointId;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
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

@Configuration
@EnableIntegration
public class TcpConfig {

    @Configuration
    static class ChatServer {

        private final Logger log = LoggerFactory.getLogger( ChatServer.class );

        private final ClientService clientService;

        ChatServer( final ClientService clientService ) {

            this.clientService = clientService;

        }

        @Bean
        public TcpNetServerConnectionFactorySpec serverConnectionFactory( @Value( "${tcp.server.port:9876}" ) int tcpServerPort ) {
            return Tcp.netServer( tcpServerPort );
        }

        @Bean
        public IntegrationFlow inboundFlow( AbstractServerConnectionFactory serverConnectionFactory ) {

            return IntegrationFlow
                    .from( Tcp.inboundAdapter( serverConnectionFactory ).id( "tcp-request-endpoint" ) )
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
                    .handle( Map.class, (p, h) -> {
                        log.info( "handle command message : enter" );

                        log.info( "message: [{}, {}]", h, p );

                        var response = "";
                        var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                        var action = Actions.valueOf( (String) h.get( "action" ) );
                        switch( action ) {
                            case login:

                                var loggedIn = this.clientService.login( connectionId, (String) p.get( "username" ) );

                                response =
                                        """
                                        {
                                            'status': 'login %s!'
                                        }
                                        """.formatted( loggedIn ? "succeeded" : "failed" );

                                break;

                            case logout:

                                var loggedOut = this.clientService.logout( connectionId, (String) p.get( "username" ) );

                                response =
                                        """
                                        {
                                            'status': 'logout %s!'
                                        }
                                        """.formatted( loggedOut ? "succeeded" : "failed" );

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
                    .handle( Map.class, (p, h) -> {
                        log.info( "handle chat message : enter" );

                        log.info( "message: [{}, {}]", h, p );

                        var connectionId = (String) h.get( IpHeaders.CONNECTION_ID );
                        var to = (String) p.get( "to" );

                        var from = this.clientService.getUsername( connectionId );
                        if( from.isPresent() ) {

                            var message = (String) p.get( "message" );

                            var sendToConnection = this.clientService.getConnection( to );
                            if( sendToConnection.isPresent() ) {

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
                                return new GenericMessage<>( responsePayload, Map.of( IpHeaders.CONNECTION_ID, sendToConnection.get() ) );
                            }

                        }

                        log.info( "error sending chat message!" );
                        return
                            """
                            {
                                'status': 'chat not sent!'
                            }
                            """;

                    })
                    .channel( replyChannel )
                    .get();
        }

        @Bean
        @EndpointId( "reply-tcp-endpoint" )
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

            this.clientService.registerConnection( event.getConnectionId() );

        }

        @EventListener
        public void close( TcpConnectionCloseEvent event ) {

            this.clientService.removeConnection( event.getConnectionId() );

        }

    }

}
