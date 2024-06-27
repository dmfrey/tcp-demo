package com.broadcom.tanzulabs.tcpdemo;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

public class EventSerializer implements Serializer<byte[]>, Deserializer<Collection<byte[]>> {

    private static Logger log = LoggerFactory.getLogger( EventSerializer.class );

    private static final byte[] CRLF = "\r\n".getBytes();

    @Override
    public Collection<byte[]> deserialize( @NotNull InputStream inputStream ) throws IOException {
        log.debug( "deserialize : enter" );

        var xmls = new ArrayList<byte[]>();
        try( var scanner = new Scanner( inputStream, StandardCharsets.UTF_8 ) ) {

            scanner.useDelimiter( "</event>" );
            while ( scanner.hasNext() ) {

                var xml = scanner.next();
                xml = xml.replace( '\n', ' ' );
                xml = xml.trim();

                if( !xml.isEmpty() ) {

                    xmls.add( xml.getBytes( StandardCharsets.UTF_8 ) );

                }

            }

            log.info( "Deserialized {} events", xmls.size() );
        }

        log.debug( "deserialize : exit" );
        return xmls;
    }

    @Override
    public void serialize( @NotNull byte[] bytes, @NotNull OutputStream outputStream ) throws IOException {
        log.info( "serialize : enter" );

        outputStream.write( bytes );
        outputStream.write( CRLF );

        log.info( "serialize : exit" );
    }

}
