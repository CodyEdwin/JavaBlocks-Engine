package com.javablocks.core.utils;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IntStack utility class.
 */
class IntStackTest {
    
    private IntStack stack;
    
    @BeforeEach
    void setUp() {
        stack = new IntStack();
    }
    
    @Test
    @DisplayName("New stack should be empty")
    void newStackShouldBeEmpty() {
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }
    
    @Test
    @DisplayName("Push should increase size")
    void pushShouldIncreaseSize() {
        stack.push(10);
        assertEquals(1, stack.size());
        assertFalse(stack.isEmpty());
        
        stack.push(20);
        stack.push(30);
        assertEquals(3, stack.size());
    }
    
    @Test
    @DisplayName("Pop should return last pushed value")
    void popShouldReturnLastPushed() {
        stack.push(10);
        stack.push(20);
        stack.push(30);
        
        assertEquals(30, stack.pop());
        assertEquals(20, stack.pop());
        assertEquals(10, stack.pop());
    }
    
    @Test
    @DisplayName("Pop on empty stack should throw exception")
    void popOnEmptyStackShouldThrow() {
        assertThrows(java.util.NoSuchElementException.class, () -> {
            stack.pop();
        });
    }
    
    @Test
    @DisplayName("Peek should return top without removing")
    void peekShouldReturnTopWithoutRemoving() {
        stack.push(10);
        stack.push(20);
        
        assertEquals(20, stack.peek());
        assertEquals(2, stack.size()); // Size unchanged
    }
    
    @Test
    @DisplayName("Peek on empty stack should throw exception")
    void peekOnEmptyStackShouldThrow() {
        assertThrows(java.util.NoSuchElementException.class, () -> {
            stack.peek();
        });
    }
    
    @Test
    @DisplayName("Clear should reset stack")
    void clearShouldResetStack() {
        stack.push(10);
        stack.push(20);
        stack.push(30);
        
        stack.clear();
        
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }
    
    @Test
    @DisplayName("Stack should handle many elements")
    void stackShouldHandleManyElements() {
        for (int i = 0; i < 1000; i++) {
            stack.push(i);
        }
        
        assertEquals(1000, stack.size());
        
        for (int i = 999; i >= 0; i--) {
            assertEquals(i, stack.pop());
        }
        
        assertTrue(stack.isEmpty());
    }
    
    @Test
    @DisplayName("Stack should grow automatically")
    void stackShouldGrowAutomatically() {
        IntStack smallStack = new IntStack(2);
        
        smallStack.push(1);
        smallStack.push(2);
        smallStack.push(3); // Should trigger growth
        smallStack.push(4);
        
        assertEquals(4, smallStack.size());
        assertEquals(4, smallStack.pop());
        assertEquals(3, smallStack.pop());
        assertEquals(2, smallStack.pop());
        assertEquals(1, smallStack.pop());
    }
    
    @Test
    @DisplayName("Stack should handle negative values")
    void stackShouldHandleNegativeValues() {
        stack.push(-1);
        stack.push(-100);
        stack.push(0);
        
        assertEquals(0, stack.pop());
        assertEquals(-100, stack.pop());
        assertEquals(-1, stack.pop());
    }
    
    @Test
    @DisplayName("Stack should handle large values")
    void stackShouldHandleLargeValues() {
        stack.push(Integer.MAX_VALUE);
        stack.push(Integer.MIN_VALUE);
        
        assertEquals(Integer.MIN_VALUE, stack.pop());
        assertEquals(Integer.MAX_VALUE, stack.pop());
    }
}
