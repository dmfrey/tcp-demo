package com.broadcom.tanzulabs.tcpdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClientService {

    private static final Logger log = LoggerFactory.getLogger( ClientService.class );

    // could be database
    private final Map<String, String> clientUsernames = new ConcurrentHashMap<>();

    public void registerConnection( String connectionId ) {

        if( !this.clientUsernames.containsKey( connectionId ) ) {

            this.clientUsernames.put( connectionId, "" );
            log.info( "registerConnection : client [{}] registered!", connectionId );

        } else {

            log.info( "registerConnection : client [{}] already registered!", connectionId );
        }

    }

    public void removeConnection( String connectionId ) {

        if( this.clientUsernames.containsKey( connectionId ) ) {

            this.clientUsernames.remove( connectionId );
            log.info( "removeConnection : client [{}] unregistered!", connectionId );

        } else {

            log.info( "removeConnection : client [{}] not found!", connectionId );

        }


    }

    public boolean login( String connectionId, String username ) {

        if( this.clientUsernames.containsKey( connectionId ) ) {

            this.clientUsernames.put( connectionId, username );
            log.info( "login : user [{}] logged in at connectionId [{}]!", username, connectionId );

            return true;

        } else {

            log.info( "login : client [{}] not connected for user [{}]!", connectionId, username );

            return false;
        }

    }

    public boolean logout( String connectionId, String username ) {

        if( this.clientUsernames.containsKey( connectionId ) ) {

            this.clientUsernames.put( connectionId, "" );
            log.info( "logout : user [{}] logged out at connectionId [{}]!", username, connectionId );

            return true;

        } else {

            log.info( "logout : client [{}] not connected for user [{}]!", connectionId, username );

            return false;
        }

    }

    public Optional<String> getConnection( String username ) {

        return this.clientUsernames.entrySet().stream()
                .filter( e -> e.getValue().equals( username ) )
                .map( Map.Entry::getKey )
                .findFirst();
    }

    public Optional<String> getUsername(String connectionId ) {

        return this.clientUsernames.entrySet().stream()
                .filter( e -> e.getKey().equals( connectionId ) )
                .map( Map.Entry::getValue )
                .findFirst();
    }

    public boolean isClientConnected( String connectionId ) {

        return this.clientUsernames.containsKey( connectionId );
    }

    public boolean isUserLoggedIn( String username ) {

        return this.clientUsernames.containsValue( username );
    }

}
