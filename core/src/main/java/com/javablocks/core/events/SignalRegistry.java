/*
 * JavaBlocks Engine - Signal Registry
 * 
 * Central registry for managing all signals in the engine.
 * Provides type-safe signal access and lifecycle management.
 */
package com.javablocks.core.events;

import java.util.*;
import java.util.concurrent.*;

/**
 * Central registry for managing signals in the engine.
 * 
 * The signal registry provides:
 * - Centralized signal management
 * - Type-safe signal access
 * - Automatic signal creation
 * - Thread-safe operations
 * - Signal lifecycle management
 * 
 * @author JavaBlocks Engine Team
 */
public final class SignalRegistry {
    
    // ==================== Constants ====================
    
    /** Initial capacity for signal map. */
    private static final int INITIAL_CAPACITY = 32;
    
    // ==================== Instance Variables ====================
    
    /** Map from signal class to signal instance. */
    private final ConcurrentHashMap<Class<?>, Signal<?>> signals;
    
    /** Map from signal class to signal type info. */
    private final ConcurrentHashMap<Class<?>, SignalTypeInfo<?>> signalTypes;
    
    /** Registry for built-in signals. */
    private boolean sealed;
    
    /** Number of registered signals. */
    private volatile int signalCount;
    
    // ==================== Signal Type Information ====================
    
    /**
     * Information about a registered signal type.
     */
    private static final class SignalTypeInfo<T> {
        final Class<T> eventType;
        final String name;
        final boolean builtIn;
        
        SignalTypeInfo(Class<T> eventType, String name, boolean builtIn) {
            this.eventType = eventType;
            this.name = name;
            this.builtIn = builtIn;
        }
    }
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new signal registry.
     */
    public SignalRegistry() {
        this.signals = new ConcurrentHashMap<>(INITIAL_CAPACITY);
        this.signalTypes = new ConcurrentHashMap<>(INITIAL_CAPACITY);
        this.sealed = false;
        this.signalCount = 0;
    }
    
    // ==================== Registration Methods ====================
    
    /**
     * Registers a signal type.
     * 
     * @param signalClass The signal class (should be Signal.class or subclass)
     * @param <T> The event type
     * @return The registered signal
     */
    @SuppressWarnings("unchecked")
    public <T> Signal<T> register(Class<?> signalClass) {
        Objects.requireNonNull(signalClass, "Signal class cannot be null");
        
        if (sealed) {
            throw new IllegalStateException("Signal registry is sealed");
        }
        
        // Get or create the signal
        Signal<?> existing = signals.get(signalClass);
        if (existing != null) {
            return (Signal<T>) existing;
        }
        
        Signal<T> signal = new Signal<>(4);
        Signal<?> previous = signals.putIfAbsent(signalClass, signal);
        
        if (previous != null) {
            return (Signal<T>) previous;
        }
        
        signalCount++;
        
        return signal;
    }
    
    /**
     * Registers a signal type with custom event type information.
     * 
     * @param signalClass The signal class
     * @param eventType The event type class
     * @param name Optional name for the signal
     * @param <T> The event type
     * @return The registered signal
     */
    @SuppressWarnings("unchecked")
    public <T> Signal<T> register(Class<?> signalClass, Class<T> eventType, String name) {
        Objects.requireNonNull(signalClass, "Signal class cannot be null");
        Objects.requireNonNull(eventType, "Event type cannot be null");
        
        Signal<T> signal = register(signalClass);
        
        // Register type info
        String signalName = name != null ? name : signalClass.getSimpleName();
        signalTypes.put(signalClass, new SignalTypeInfo<>(eventType, signalName, false));
        
        return signal;
    }
    
    /**
     * Registers a built-in signal.
     * 
     * @param signalClass The signal class
     * @param <T> The event type
     * @return The registered signal
     */
    <T> Signal<T> registerBuiltin(Class<?> signalClass) {
        Objects.requireNonNull(signalClass, "Signal class cannot be null");
        
        Signal<T> signal = register(signalClass);
        
        // Mark as built-in
        Class<?> eventType = extractEventType(signalClass);
        String name = signalClass.getSimpleName();
        signalTypes.put(signalClass, new SignalTypeInfo<>(eventType, name, true));
        
        return signal;
    }
    
    // ==================== Lookup Methods ====================
    
    /**
     * Gets a signal by class.
     * 
     * @param signalClass The signal class
     * @param <T> The event type
     * @return The signal, or null if not registered
     */
    @SuppressWarnings("unchecked")
    public <T> Signal<T> getSignal(Class<?> signalClass) {
        return (Signal<T>) signals.get(signalClass);
    }
    
    /**
     * Gets a signal by class, creating it if necessary.
     * 
     * @param signalClass The signal class
     * @param <T> The event type
     * @return The signal
     */
    @SuppressWarnings("unchecked")
    public <T> Signal<T> getOrCreate(Class<?> signalClass) {
        Signal<?> signal = signals.get(signalClass);
        if (signal == null && !sealed) {
            signal = register(signalClass);
        }
        return (Signal<T>) signal;
    }
    
    /**
     * Gets the event type for a signal class.
     * 
     * @param signalClass The signal class
     * @param <T> The event type
     * @return The event type class
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> getEventType(Class<?> signalClass) {
        SignalTypeInfo<?> info = signalTypes.get(signalClass);
        if (info != null) {
            return (Class<T>) info.eventType;
        }
        return (Class<T>) Object.class;
    }
    
    /**
     * Checks if a signal is registered.
     * 
     * @param signalClass The signal class
     * @return true if registered
     */
    public boolean isRegistered(Class<?> signalClass) {
        return signals.containsKey(signalClass);
    }
    
    // ==================== Dispatch Methods ====================
    
    /**
     * Dispatches an event to a specific signal.
     * 
     * @param signalClass The signal class
     * @param event The event to dispatch
     * @param <T> The event type
     * @return true if the signal was found
     */
    @SuppressWarnings("unchecked")
    public <T> boolean dispatch(Class<?> signalClass, T event) {
        Signal<?> signal = signals.get(signalClass);
        if (signal != null) {
            ((Signal<T>) signal).dispatch(event);
            return true;
        }
        return false;
    }
    
    /**
     * Dispatches an event to all signals (broadcast).
     * 
     * @param event The event to dispatch
     * @param <T> The event type
     * @return The number of signals that received the event
     */
    public <T> int broadcast(T event) {
        int count = 0;
        
        for (Signal<?> signal : signals.values()) {
            try {
                @SuppressWarnings("unchecked")
                Signal<T> typedSignal = (Signal<T>) signal;
                if (typedSignal.hasListeners()) {
                    typedSignal.dispatch(event);
                    count++;
                }
            } catch (ClassCastException e) {
                // Signal doesn't accept this event type
            }
        }
        
        return count;
    }
    
    // ==================== Lifecycle ====================
    
    /**
     * Seals the registry to prevent new signal registrations.
     * This should be called after all built-in signals are registered.
     */
    public void seal() {
        this.sealed = true;
    }
    
    /**
     * Checks if the registry is sealed.
     * 
     * @return true if sealed
     */
    public boolean isSealed() {
        return sealed;
    }
    
    /**
     * Unregisters a signal.
     * 
     * @param signalClass The signal class
     * @return The unregistered signal, or null if not found
     */
    public Signal<?> unregister(Class<?> signalClass) {
        Signal<?> removed = signals.remove(signalClass);
        if (removed != null) {
            signalCount--;
            removed.unsubscribeAll();
        }
        signalTypes.remove(signalClass);
        return removed;
    }
    
    /**
     * Clears all registered signals.
     */
    public void clear() {
        for (Signal<?> signal : signals.values()) {
            signal.unsubscribeAll();
        }
        signals.clear();
        signalTypes.clear();
        signalCount = 0;
        sealed = false;
    }
    
    /**
     * Disposes the registry and all signals.
     */
    public void dispose() {
        clear();
    }
    
    // ==================== Information Methods ====================
    
    /**
     * Gets the number of registered signals.
     * 
     * @return The signal count
     */
    public int getSignalCount() {
        return signalCount;
    }
    
    /**
     * Gets all registered signal classes.
     * 
     * @return Set of registered signal classes
     */
    public Set<Class<?>> getRegisteredSignals() {
        return Collections.unmodifiableSet(signals.keySet());
    }
    
    /**
     * Gets information about a signal type.
     * 
     * @param signalClass The signal class
     * @return Signal type information, or null if not found
     */
    public SignalTypeInfo<?> getSignalTypeInfo(Class<?> signalClass) {
        return signalTypes.get(signalClass);
    }
    
    /**
     * Gets all built-in signal classes.
     * 
     * @return Collection of built-in signal classes
     */
    public Collection<Class<?>> getBuiltInSignals() {
        List<Class<?>> builtIn = new ArrayList<>();
        
        for (SignalTypeInfo<?> info : signalTypes.values()) {
            if (info.builtIn) {
                builtIn.add(info.eventType.getClass());
            }
        }
        
        return builtIn;
    }
    
    /**
     * Gets debug information about the registry.
     * 
     * @return A map of debug information
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("Signal Count", signalCount);
        info.put("Sealed", sealed);
        
        Map<String, Integer> signalStats = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, Signal<?>> entry : signals.entrySet()) {
            String name = entry.getKey().getSimpleName();
            int listenerCount = entry.getValue().getListenerCount();
            signalStats.put(name, listenerCount);
        }
        info.put("Signals", signalStats);
        
        return info;
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Extracts the event type from a signal class.
     * 
     * @param signalClass The signal class
     * @return The event type class
     */
    @SuppressWarnings("unchecked")
    private Class<?> extractEventType(Class<?> signalClass) {
        // Get the generic type parameter
        java.lang.reflect.Type[] genericInterfaces = signalClass.getGenericInterfaces();
        
        for (java.lang.reflect.Type type : genericInterfaces) {
            if (type instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) type;
                if (pt.getRawType() == Signal.class) {
                    java.lang.reflect.Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                        return (Class<?>) typeArgs[0];
                    }
                }
            }
        }
        
        // Fallback to Object
        return Object.class;
    }
    
    /**
     * Gets a string representation of this registry.
     * 
     * @return String representation
     */
    @Override
    public String toString() {
        return "SignalRegistry[signals=" + signalCount + ", sealed=" + sealed + "]";
    }
}
