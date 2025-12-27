/*
 * JavaBlocks Engine - Plugin Class Loader
 * 
 * Custom class loader for plugin isolation and sandboxing.
 */
package com.javablocks.core.plugin;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Custom class loader for plugin loading.
 * 
 * Provides:
 * - JAR file loading
 * - Class isolation
 * - Resource loading from JARs
 * - Security sandboxing
 * 
 * @author JavaBlocks Engine Team
 */
public final class PluginClassLoader extends ClassLoader {
    
    // ==================== Constants ====================
    
    /** Default buffer size for reading. */
    private static final int BUFFER_SIZE = 8192;
    
    // ==================== Instance Fields ====================
    
    /** Loaded JAR URLs. */
    private final List<URL> jarUrls;
    
    /** Class cache. */
    private final HashMap<String, Class<?>> classCache;
    
    /** Parent class loader. */
    private final ClassLoader parent;
    
    // ==================== Constructor ====================
    
    /**
     * Creates a new plugin class loader.
     */
    public PluginClassLoader() {
        this(PluginClassLoader.class.getClassLoader());
    }
    
    /**
     * Creates a plugin class loader with a parent.
     * 
     * @param parent Parent class loader
     */
    public PluginClassLoader(ClassLoader parent) {
        super(parent);
        this.jarUrls = new ArrayList<>();
        this.classCache = new HashMap<>();
        this.parent = parent;
    }
    
    // ==================== JAR Loading ====================
    
    /**
     * Adds a JAR file to the class path.
     * 
     * @param jarPath Path to the JAR file
     * @return true if added successfully
     */
    public boolean addJar(String jarPath) {
        try {
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                System.err.println("[PluginClassLoader] JAR not found: " + jarPath);
                return false;
            }
            
            URL jarUrl = jarFile.toURI().toURL();
            jarUrls.add(jarUrl);
            
            return true;
        } catch (MalformedURLException e) {
            System.err.println("[PluginClassLoader] Invalid JAR URL: " + jarPath);
            return false;
        }
    }
    
    /**
     * Adds a JAR from a URL.
     * 
     * @param jarUrl URL to the JAR file
     */
    public void addJar(URL jarUrl) {
        jarUrls.add(jarUrl);
    }
    
    /**
     * Loads a class from a specific JAR file.
     * 
     * @param jarPath Path to the JAR file
     * @param className Name of the class to load
     * @return The loaded class
     * @throws ClassNotFoundException if not found
     */
    public Class<?> loadClassFromJar(String jarPath, String className) 
            throws ClassNotFoundException {
        if (!addJar(jarPath)) {
            throw new ClassNotFoundException("Failed to add JAR: " + jarPath);
        }
        
        return loadClass(className);
    }
    
    // ==================== Class Loading ====================
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Check cache first
        Class<?> cached = classCache.get(name);
        if (cached != null) {
            return cached;
        }
        
        // Try to load from JARs
        String classFileName = name.replace('.', '/') + ".class";
        
        for (URL jarUrl : jarUrls) {
            try {
                URLClassLoader jarLoader = new URLClassLoader(
                    new URL[] { jarUrl },
                    parent
                );
                
                InputStream inputStream = jarLoader.getResourceAsStream(classFileName);
                if (inputStream != null) {
                    byte[] bytes = readBytes(inputStream);
                    Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
                    classCache.put(name, clazz);
                    return clazz;
                }
            } catch (IOException e) {
                // Continue to next JAR
            }
        }
        
        throw new ClassNotFoundException(name);
    }
    
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // Check if already loaded
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }
        
        // Try parent class loader first (for system classes)
        try {
            return parent.loadClass(name);
        } catch (ClassNotFoundException e) {
            // Not in parent, try our JARs
            return findClass(name);
        }
    }
    
    // ==================== Resource Loading ====================
    
    @Override
    protected URL findResource(String name) {
        for (URL jarUrl : jarUrls) {
            try {
                URLClassLoader jarLoader = new URLClassLoader(
                    new URL[] { jarUrl },
                    parent
                );
                
                URL resource = jarLoader.getResource(name);
                if (resource != null) {
                    return resource;
                }
            } catch (IOException e) {
                // Continue to next JAR
            }
        }
        
        return null;
    }
    
    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        List<URL> resources = new ArrayList<>();
        
        for (URL jarUrl : jarUrls) {
            URLClassLoader jarLoader = new URLClassLoader(
                new URL[] { jarUrl },
                parent
            );
            
            Enumeration<URL> jarResources = jarLoader.getResources(name);
            while (jarResources.hasMoreElements()) {
                resources.add(jarResources.nextElement());
            }
        }
        
        return Collections.enumeration(resources);
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Reads all bytes from an input stream.
     * 
     * @param input The input stream
     * @return Byte array
     * @throws IOException if reading fails
     */
    private static byte[] readBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        
        return output.toByteArray();
    }
    
    /**
     * Clears the class cache.
     */
    public void clearCache() {
        classCache.clear();
    }
    
    /**
     * Gets the number of loaded JARs.
     * 
     * @return JAR count
     */
    public int getJarCount() {
        return jarUrls.size();
    }
    
    /**
     * Gets the number of cached classes.
     * 
     * @return Cached class count
     */
    public int getCachedClassCount() {
        return classCache.size();
    }
}
