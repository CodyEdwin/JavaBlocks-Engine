/*
 * JavaBlocks Engine - Connection
 * 
 * Represents a connection between signal subscribers for managing subscriptions.
 */
package com.javablocks.core.graph;

import java.util.function.Consumer;

/**
 * Represents a connection between a signal and a listener.
 * 
 * Connections can be used to unsubscribe from signals
 * and query the connection state.
 * 
 * @param <T> The type of event data
 */
public final class Connection<T> {
    
    /** The listener callback. */
    private final Consumer<T> listener;
    
    /** Whether this connection is still active. */
    private volatile boolean active;
    
    /**
     * Creates a new connection.
     * 
     * @param listener The listener callback
     */
    Connection(Consumer<T> listener) {
        this.listener = listener;
        this.active = true;
    }
    
    /**
     * Disconnects this connection.
     * After disconnection, the listener will no longer receive events.
     */
    public void disconnect() {
        active = false;
    }
    
    /**
     * Checks if this connection is still active.
     * 
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Gets the listener callback.
     * 
     * @return The listener
     */
    public Consumer<T> getListener() {
        return listener;
    }
}
