package com.javablocks.core.graph;

import org.junit.jupiter.api.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Connection class.
 */
class ConnectionTest {
    
    @Test
    @DisplayName("New connection should be active")
    void newConnectionShouldBeActive() {
        Consumer<String> listener = s -> {};
        Connection<String> connection = new Connection<>(listener);
        
        assertTrue(connection.isActive());
    }
    
    @Test
    @DisplayName("getListener should return the listener")
    void getListenerShouldReturnTheListener() {
        Consumer<String> listener = s -> {};
        Connection<String> connection = new Connection<>(listener);
        
        assertSame(listener, connection.getListener());
    }
    
    @Test
    @DisplayName("disconnect should make connection inactive")
    void disconnectShouldMakeConnectionInactive() {
        Consumer<String> listener = s -> {};
        Connection<String> connection = new Connection<>(listener);
        
        connection.disconnect();
        
        assertFalse(connection.isActive());
    }
    
    @Test
    @DisplayName("multiple disconnects should not cause issues")
    void multipleDisconnectsShouldNotCauseIssues() {
        Consumer<String> listener = s -> {};
        Connection<String> connection = new Connection<>(listener);
        
        connection.disconnect();
        connection.disconnect(); // Second disconnect should not throw
        connection.disconnect();
        
        assertFalse(connection.isActive());
    }
}
