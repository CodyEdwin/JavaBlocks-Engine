package com.javablocks.core.resource;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple resource implementation for testing.
 */
class TestResource implements Resource<String> {
    private final String path;
    private final String data;
    private boolean loaded = false;
    private boolean disposed = false;
    
    public TestResource(String path, String data) {
        this.path = path;
        this.data = data;
    }
    
    @Override
    public String getPath() { return path; }
    
    @Override
    public String getName() { return path.substring(path.lastIndexOf('/') + 1); }
    
    @Override
    public String getType() { return "TestResource"; }
    
    @Override
    public boolean isLoaded() { return loaded; }
    
    @Override
    public long getSize() { return data != null ? data.length() : 0; }
    
    @Override
    public String[] getDependencies() { return new String[0]; }
    
    @Override
    public float getProgress() { return loaded ? 1.0f : 0.0f; }
    
    @Override
    public boolean reload() {
        loaded = false;
        return true;
    }
    
    @Override
    public void dispose() {
        disposed = true;
    }
    
    @Override
    public String getData() { return data; }
    
    public boolean isDisposed() { return disposed; }
    
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
}

/**
 * Tests for Resource interface default methods.
 */
class ResourceTest {
    
    @Test
    @DisplayName("getId should return path by default")
    void getIdShouldReturnPathByDefault() {
        TestResource resource = new TestResource("/test/path", "data");
        assertEquals("/test/path", resource.getId());
    }
    
    @Test
    @DisplayName("Resource should track loaded state")
    void resourceShouldTrackLoadedState() {
        TestResource resource = new TestResource("/test", "test data");
        
        assertFalse(resource.isLoaded());
        resource.setLoaded(true);
        assertTrue(resource.isLoaded());
    }
    
    @Test
    @DisplayName("Resource should track disposed state")
    void resourceShouldTrackDisposedState() {
        TestResource resource = new TestResource("/test", "test data");
        
        assertFalse(resource.isDisposed());
        resource.dispose();
        assertTrue(resource.isDisposed());
    }
    
    @Test
    @DisplayName("Resource should return correct size")
    void resourceShouldReturnCorrectSize() {
        TestResource resource = new TestResource("/test", "Hello");
        assertEquals(5, resource.getSize());
    }
    
    @Test
    @DisplayName("Resource should return correct name")
    void resourceShouldReturnCorrectName() {
        TestResource resource = new TestResource("/test/path/file.txt", "data");
        assertEquals("file.txt", resource.getName());
    }
    
    @Test
    @DisplayName("Resource should return correct type")
    void resourceShouldReturnCorrectType() {
        TestResource resource = new TestResource("/test", "data");
        assertEquals("TestResource", resource.getType());
    }
    
    @Test
    @DisplayName("Resource should return empty dependencies")
    void resourceShouldReturnEmptyDependencies() {
        TestResource resource = new TestResource("/test", "data");
        assertEquals(0, resource.getDependencies().length);
    }
    
    @Test
    @DisplayName("Resource should return correct progress")
    void resourceShouldReturnCorrectProgress() {
        TestResource resource = new TestResource("/test", "data");
        
        assertEquals(0.0f, resource.getProgress());
        resource.setLoaded(true);
        assertEquals(1.0f, resource.getProgress());
    }
    
    @Test
    @DisplayName("Resource should return data")
    void resourceShouldReturnData() {
        TestResource resource = new TestResource("/test", "test data");
        assertEquals("test data", resource.getData());
    }
    
    @Test
    @DisplayName("Resource reload should work")
    void resourceReloadShouldWork() {
        TestResource resource = new TestResource("/test", "data");
        resource.setLoaded(true);
        
        boolean result = resource.reload();
        
        assertTrue(result);
        assertFalse(resource.isLoaded());
    }
}
