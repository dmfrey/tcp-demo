package com.broadcom.tanzulabs.tcpdemo;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class EventSerializerTests {

    EventSerializer subject = new EventSerializer();

    String fakeMessage =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <event>
                <device>ANDROID-abc123</device>
            </event><?xml version="1.0"?>
            <event>
                <device>ANDROID-abc123</device>
            </event><?xml version="1.0"?>
            <event>
                <device>ANDROID-abc123</device>
            </event><?xml version="1.0"?>
            <event>
                <device>ANDROID-abc123</device>
            </event>
            """;


    @Test
    void testDeserialize() throws IOException {

        var actual = this.subject.deserialize( new ByteArrayInputStream( fakeMessage.getBytes() ) );
        assertThat( actual ).hasSize( 4 );

    }

//    @Test
//    void testSerialize() throws IOException {
//
//        var actual = new ByteArrayOutputStream();
//        this.subject.serialize( fakeMessage, actual );
//
//        assertThat( actual.toString() ).isEqualTo( fakeMessage );
//
//    }

}