package com.broadcom.tanzulabs.tcpdemo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientServiceTest {

    ClientService subject = new ClientService();

    String fakeConnectionId = "fakeConnectionId";
    String fakeUsername = "fakeUsername";

    @Test
    void registerConnection() {

        subject.registerConnection( fakeConnectionId );
        assertThat( subject.isClientConnected( fakeConnectionId ) ).isTrue();

    }

    @Test
    void removeConnection() {

        subject.removeConnection( fakeConnectionId );
        assertThat( subject.isClientConnected( fakeConnectionId ) ).isFalse();

    }

    @Test
    void login() {

        subject.registerConnection( fakeConnectionId );
        subject.login( fakeConnectionId, fakeUsername );
        assertThat( subject.isClientConnected( fakeConnectionId ) ).isTrue();
        assertThat( subject.isUserLoggedIn( fakeUsername ) ).isTrue();

    }

    @Test
    void logout() {

        subject.registerConnection( fakeConnectionId );
        subject.login( fakeConnectionId, fakeUsername );
        subject.logout( fakeConnectionId, fakeUsername );
        assertThat( subject.isClientConnected( fakeConnectionId ) ).isTrue();
        assertThat( subject.isUserLoggedIn( fakeConnectionId ) ).isFalse();

    }

    @Test
    void getConnection() {

        subject.registerConnection( fakeConnectionId );
        subject.login( fakeConnectionId, fakeUsername );

        var actual = subject.getConnection( fakeUsername );
        assertThat( actual.get() ).isEqualTo( fakeConnectionId );

    }

    @Test
    void getLoggedInConnections() {

        subject.registerConnection( fakeConnectionId );
        subject.login( fakeConnectionId, fakeUsername );

        var actual = subject.getLoggedInConnections();
        assertThat( actual ).hasSize( 1 );

    }

    @Test
    void getUsername() {

        subject.registerConnection( fakeConnectionId );
        subject.login( fakeConnectionId, fakeUsername );

        var actual = subject.getUsername( fakeConnectionId );
        assertThat( actual.get() ).isEqualTo( fakeUsername );

    }

}