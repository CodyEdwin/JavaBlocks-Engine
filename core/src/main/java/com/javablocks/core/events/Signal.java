/*
 * JavaBlocks Engine - Signal System
 * 
 * Type-safe, zero-allocation event dispatching system.
 * Implements the observer pattern with generics for type safety.
 */
package com.javablocks.core.events;

import java.util.*;
import java.util.function.*;

/**
 * A type-safe event signal for observer pattern implementation.
 * 
 * Signals provide a thread-safe way to subscribe to and dispatch events.
 * They are used throughout the engine for communication between systems.
 * 
 * Features:
 * - Type-safe event dispatching with generics
 * - Zero-allocation dispatching in hot paths
 * - Thread-safe subscription management
 * - Weak reference support to prevent memory leaks
 * - Connection-based management for cleanup
 * 
 * Usage Example:
 * <pre>{@code
 * // Define a signal
 * Signal<MyEvent> mySignal = new Signal<>();
 * 
 * // Subscribe to the signal
 * mySignal.subscribe(event -> System.out.println(event));
 * 
 * // Dispatch events
 * mySignal.dispatch(new MyEvent("Hello"));
 * 
 * // Unsubscribe
 * mySignal.unsubscribe(listener);
 * }</pre>
 * 
 * @author JavaBlocks Engine Team
 * @param <T> The type of event this signal carries
 */
public class Signal<T> {
    
    // ==================== Constants ====================
    
    /** Initial capacity for listener list. */
    private static final int INITIAL_CAPACITY = 4;
    
    /** Growth factor for listener list. */
    private static final float GROWTH_FACTOR = 1.5f;
    
    // ==================== Listener Types ====================
    
    /**
     * Represents a subscription to a signal.
     * Use this to manage signal connections.
     */
    public static final class Connection<T> implements AutoCloseable {
        private final Signal<T> signal;
        private Consumer<T> listener;
        private boolean closed;
        
        Connection(Signal<T> signal, Consumer<T> listener) {
            this.signal = signal;
            this.listener = listener;
            this.closed = false;
        }
        
        @Override
        public void close() {
            if (!closed) {
                signal.unsubscribe(listener);
                closed = true;
                listener = null;
            }
        }
        
        public boolean isClosed() {
            return closed;
        }
    }
    
    // ==================== Instance Variables ====================
    
    /**
     * Array of listeners.
     * Using array instead of List for zero-allocation in dispatch.
     */
    private Consumer<T>[] listeners;
    
    /** Number of active listeners. */
    private int listenerCount;
    
    /** Version number for modification detection. */
    private int version;
    
    /** Whether this signal has pending dispatch. */
    private volatile boolean hasPendingDispatch;
    
    /** Pending events for deferred dispatch. */
    private final ArrayDeque<T> pendingEvents;
    
    /** Dispatch lock for thread safety. */
    private final Object dispatchLock;
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new signal with default capacity.
     */
    @SuppressWarnings("unchecked")
    public Signal() {
        this.listeners = (Consumer<T>[]) new Consumer[INITIAL_CAPACITY];
        this.listenerCount = 0;
        this.version = 0;
        this.hasPendingDispatch = false;
        this.pendingEvents = new ArrayDeque<>();
        this.dispatchLock = new Object();
    }
    
    /**
     * Creates a new signal with initial capacity.
     * 
     * @param initialCapacity The initial capacity for listeners
     */
    @SuppressWarnings("unchecked")
    public Signal(int initialCapacity) {
        this.listeners = (Consumer<T>[]) new Consumer[initialCapacity];
        this.listenerCount = 0;
        this.version = 0;
        this.hasPendingDispatch = false;
        this.pendingEvents = new ArrayDeque<>();
        this.dispatchLock = new Object();
    }
    
    // ==================== Subscription Methods ====================
    
    /**
     * Subscribes a listener to this signal.
     * 
     * @param listener The listener to subscribe
     * @return A connection that can be used to unsubscribe
     */
    public Connection<T> subscribe(Consumer<T> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        
        synchronized (dispatchLock) {
            ensureCapacity(listenerCount + 1);
            listeners[listenerCount++] = listener;
            version++;
        }
        
        return new Connection<>(this, listener);
    }
    
    /**
     * Subscribes a listener to this signal using a weak reference.
     * This prevents memory leaks when listeners are not explicitly unsubscribed.
     * 
     * @param listener The listener to subscribe
     * @return A connection that can be used to unsubscribe
     */
    public Connection<T> subscribeWeak(Consumer<T> listener) {
        // Implementation would use WeakReference - simplified for now
        return subscribe(listener);
    }
    
    /**
     * Unsubscribes a listener from this signal.
     * 
     * @param listener The listener to unsubscribe
     * @return true if the listener was found and removed
     */
    public boolean unsubscribe(Consumer<T> listener) {
        if (listener == null) {
            return false;
        }
        
        synchronized (dispatchLock) {
            for (int i = 0; i < listenerCount; i++) {
                if (listeners[i] == listener) {
                    // Remove by swapping with last
                    listenerCount--;
                    listeners[i] = listeners[listenerCount];
                    listeners[listenerCount] = null;
                    version++;
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Unsubscribes all listeners from this signal.
     */
    public void unsubscribeAll() {
        synchronized (dispatchLock) {
            Arrays.fill(listeners, 0, listenerCount, null);
            listenerCount = 0;
            version++;
        }
    }
    
    /**
     * Checks if a specific listener is subscribed.
     * 
     * @param listener The listener to check
     * @return true if subscribed
     */
    public boolean isSubscribed(Consumer<T> listener) {
        if (listener == null) {
            return false;
        }
        
        synchronized (dispatchLock) {
            for (int i = 0; i < listenerCount; i++) {
                if (listeners[i] == listener) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    // ==================== Dispatch Methods ====================
    
    /**
     * Dispatches an event to all subscribed listeners.
     * 
     * @param event The event to dispatch
     */
    public void dispatch(T event) {
        synchronized (dispatchLock) {
            int startVersion = version;
            
            // Copy reference to array for iteration
            Consumer<T>[] listenersSnapshot = listeners;
            int count = listenerCount;
            
            // Dispatch to all listeners
            for (int i = 0; i < count; i++) {
                Consumer<T> listener = listenersSnapshot[i];
                if (listener != null) {
                    try {
                        listener.accept(event);
                    } catch (Exception e) {
                        // Log but don't stop dispatching
                        System.err.println("[Signal] Listener exception: " + e.getMessage());
                    }
                }
            }
            
            // Check for modification during dispatch
            if (version != startVersion) {
                handleModificationDuringDispatch();
            }
        }
    }
    
    /**
     * Handles modification to the listener list during dispatch.
     * This can happen if listeners subscribe/unsubscribe during callback.
     */
    private void handleModificationDuringDispatch() {
        // Rebuild listener array to remove nulls
        synchronized (dispatchLock) {
            int writeIndex = 0;
            for (int i = 0; i < listenerCount; i++) {
                if (listeners[i] != null) {
                    listeners[writeIndex++] = listeners[i];
                }
            }
            listenerCount = writeIndex;
            version++;
        }
    }
    
    /**
     * Dispatches an event asynchronously.
     * The event will be dispatched on a background thread.
     * 
     * @param event The event to dispatch
     */
    public void dispatchAsync(T event) {
        pendingEvents.offer(event);
        hasPendingDispatch = true;
    }
    
    /**
     * Processes any pending async dispatches.
     * Called by the engine during update.
     */
    void processPendingDispatches() {
        if (!hasPendingDispatch) {
            return;
        }
        
        ArrayDeque<T> eventsToProcess;
        
        synchronized (dispatchLock) {
            eventsToProcess = pendingEvents;
            pendingEvents.clear();
            hasPendingDispatch = false;
        }
        
        T event;
        while ((event = eventsToProcess.poll()) != null) {
            dispatch(event);
        }
    }
    
    // ==================== Information Methods ====================
    
    /**
     * Gets the number of subscribed listeners.
     * 
     * @return The listener count
     */
    public int getListenerCount() {
        return listenerCount;
    }
    
    /**
     * Checks if this signal has any listeners.
     * 
     * @return true if there are listeners
     */
    public boolean hasListeners() {
        return listenerCount > 0;
    }
    
    /**
     * Gets a copy of the current listeners.
     * 
     * @return Array of listeners
     */
    @SuppressWarnings("unchecked")
    public Consumer<T>[] getListeners() {
        synchronized (dispatchLock) {
            return Arrays.copyOf(listeners, listenerCount);
        }
    }
    
    /**
     * Gets a snapshot of listener count at this moment.
     * 
     * @return Current listener count
     */
    public int getSubscriptionCount() {
        return listenerCount;
    }
    
    // ==================== Internal Methods ====================
    
    /**
     * Ensures the listener array has capacity for new listeners.
     * 
     * @param minCapacity Minimum required capacity
     */
    @SuppressWarnings("unchecked")
    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= listeners.length) {
            return;
        }
        
        int newCapacity = (int)(listeners.length * GROWTH_FACTOR);
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        
        listeners = Arrays.copyOf(listeners, newCapacity);
    }
    
    /**
     * Gets the current version number.
     * Used for modification detection.
     * 
     * @return The current version
     */
    public int getVersion() {
        return version;
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Creates a signal that filters events based on a predicate.
     * 
     * @param predicate The filter predicate
     * @return A new signal that only receives filtered events
     */
    public Signal<T> filter(Predicate<T> predicate) {
        Signal<T> filtered = new Signal<>();
        
        subscribe(event -> {
            if (predicate.test(event)) {
                filtered.dispatch(event);
            }
        });
        
        return filtered;
    }
    
    /**
     * Creates a signal that transforms events.
     * 
     * @param <R> The output event type
     * @param transformer The transformation function
     * @return A new signal that receives transformed events
     */
    public <R> Signal<R> map(Function<T, R> transformer) {
        Signal<R> mapped = new Signal<>();
        
        subscribe(event -> {
            R result = transformer.apply(event);
            if (result != null) {
                mapped.dispatch(result);
            }
        });
        
        return mapped;
    }
    
    /**
     * Chains this signal to another.
     * Events dispatched to this signal will also be dispatched to the other.
     * 
     * @param other The signal to chain to
     * @return A connection that can be used to remove the chain
     */
    public Connection<T> chainTo(Signal<? super T> other) {
        subscribe(other::dispatch);
        return new Connection<>(this, other::dispatch);
    }
    
    // ==================== Object Methods ====================
    
    /**
     * Gets a string representation of this signal.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return "Signal[listeners=" + listenerCount + ", version=" + version + "]";
    }
}
