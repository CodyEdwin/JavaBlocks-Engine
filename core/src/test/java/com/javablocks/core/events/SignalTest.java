package com.javablocks.core.events;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Signal class.
 */
class SignalTest {
    
    private Signal<Integer> signal;
    
    @BeforeEach
    void setUp() {
        signal = new Signal<>();
    }
    
    @Test
    @DisplayName("New signal should have no listeners")
    void newSignalShouldHaveNoListeners() {
        assertEquals(0, signal.getListenerCount());
    }
    
    @Test
    @DisplayName("subscribe should add listener and return connection")
    void subscribeShouldAddListenerAndReturnConnection() {
        AtomicInteger callCount = new AtomicInteger(0);
        Consumer<Integer> listener = callCount::addAndGet;
        
        Signal.Connection<Integer> connection = signal.subscribe(listener);
        
        assertEquals(1, signal.getListenerCount());
        assertNotNull(connection);
        assertFalse(connection.isClosed());
    }
    
    @Test
    @DisplayName("subscribe with null should throw exception")
    void subscribeWithNullShouldThrowException() {
        assertThrows(NullPointerException.class, () -> {
            signal.subscribe(null);
        });
    }
    
    @Test
    @DisplayName("close should remove listener")
    void closeShouldRemoveListener() {
        AtomicInteger callCount = new AtomicInteger(0);
        Consumer<Integer> listener = callCount::addAndGet;
        
        Signal.Connection<Integer> connection = signal.subscribe(listener);
        connection.close();
        
        assertEquals(0, signal.getListenerCount());
        assertTrue(connection.isClosed());
    }
    
    @Test
    @DisplayName("dispatch should call listener")
    void dispatchShouldCallListener() {
        AtomicInteger receivedValue = new AtomicInteger(0);
        signal.subscribe(receivedValue::set);
        
        signal.dispatch(42);
        
        assertEquals(42, receivedValue.get());
    }
    
    @Test
    @DisplayName("dispatch should call multiple listeners")
    void dispatchShouldCallMultipleListeners() {
        AtomicInteger callCount = new AtomicInteger(0);
        
        signal.subscribe(value -> callCount.incrementAndGet());
        signal.subscribe(value -> callCount.incrementAndGet());
        signal.subscribe(value -> callCount.incrementAndGet());
        
        signal.dispatch(1);
        
        assertEquals(3, callCount.get());
    }
    
    @Test
    @DisplayName("dispatch should handle multiple dispatches")
    void dispatchShouldHandleMultipleDispatches() {
        AtomicInteger total = new AtomicInteger(0);
        signal.subscribe(total::addAndGet);
        
        signal.dispatch(10);
        signal.dispatch(20);
        signal.dispatch(30);
        
        assertEquals(60, total.get());
    }
    
    @Test
    @DisplayName("listener should be called with correct value type")
    void listenerShouldBeCalledWithCorrectValueType() {
        StringBuilder result = new StringBuilder();
        Signal<String> stringSignal = new Signal<>();
        
        stringSignal.subscribe(result::append);
        stringSignal.dispatch("Hello");
        
        assertEquals("Hello", result.toString());
    }
    
    @Test
    @DisplayName("dispatching closed listener should not call it")
    void dispatchingClosedListenerShouldNotCallIt() {
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);
        
        Signal.Connection<Integer> conn1 = signal.subscribe(count1::addAndGet);
        signal.subscribe(count2::addAndGet);
        
        conn1.close();
        signal.dispatch(5);
        
        assertEquals(0, count1.get()); // Closed listener not called
        assertEquals(5, count2.get()); // Active listener called
    }
    
    @Test
    @DisplayName("multiple closes should not cause issues")
    void multipleClosesShouldNotCauseIssues() {
        AtomicInteger callCount = new AtomicInteger(0);
        Consumer<Integer> listener = callCount::addAndGet;
        
        Signal.Connection<Integer> connection = signal.subscribe(listener);
        
        connection.close();
        // Second close should not throw
        assertDoesNotThrow(() -> connection.close());
        
        assertTrue(connection.isClosed());
        assertEquals(0, signal.getListenerCount());
    }
}
