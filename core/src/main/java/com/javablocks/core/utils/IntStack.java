/*
 * JavaBlocks Engine - IntStack Utility
 * 
 * A lightweight int stack implementation for high-performance scenarios.
 */
package com.javablocks.core.utils;

/**
 * A lightweight integer stack implementation.
 * 
 * Provides efficient push/pop operations for integer values
 * without the overhead of boxing/unboxing with Integer objects.
 */
public final class IntStack {
    
    /** The underlying array. */
    private int[] items;
    
    /** Current size of the stack. */
    private int size;
    
    /**
     * Creates a new stack with default capacity.
     */
    public IntStack() {
        this(16);
    }
    
    /**
     * Creates a new stack with specified capacity.
     * 
     * @param capacity Initial capacity
     */
    public IntStack(int capacity) {
        this.items = new int[capacity];
        this.size = 0;
    }
    
    /**
     * Pushes an integer onto the stack.
     * 
     * @param item The item to push
     */
    public void push(int item) {
        if (size >= items.length) {
            grow();
        }
        items[size++] = item;
    }
    
    /**
     * Pops an integer from the stack.
     * 
     * @return The popped item
     * @throws java.util.NoSuchElementException if stack is empty
     */
    public int pop() {
        if (size == 0) {
            throw new java.util.NoSuchElementException("Stack is empty");
        }
        return items[--size];
    }
    
    /**
     * Peeks at the top of the stack without removing it.
     * 
     * @return The top item
     * @throws java.util.NoSuchElementException if stack is empty
     */
    public int peek() {
        if (size == 0) {
            throw new java.util.NoSuchElementException("Stack is empty");
        }
        return items[size - 1];
    }
    
    /**
     * Checks if the stack is empty.
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Returns the current size of the stack.
     * 
     * @return The size
     */
    public int size() {
        return size;
    }
    
    /**
     * Clears the stack.
     */
    public void clear() {
        size = 0;
    }
    
    /**
     * Grows the internal array when needed.
     */
    private void grow() {
        int[] newItems = new int[items.length * 2];
        System.arraycopy(items, 0, newItems, 0, size);
        items = newItems;
    }
}
