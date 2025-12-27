/*
 * JavaBlocks Engine - Math Utilities
 * 
 * Extended math utilities for game development.
 * Includes vectors, matrices, and game-specific math functions.
 */
package com.javablocks.core.math;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.Color;
import java.util.*;

/**
 * Extended math utilities for game development.
 * 
 * This class provides additional math functions beyond what's provided
 * by LibGDX's MathUtils class.
 * 
 * @author JavaBlocks Engine Team
 */
public final class MathUtils {
    
    // ==================== Constants ====================
    
    /** PI. */
    public static final float PI = (float) Math.PI;
    
    /** PI times 2. */
    public static final float PI2 = PI * 2;
    
    /** Half PI. */
    public static final float HALF_PI = PI / 2;
    
    /** PI over 4. */
    public static final float QUARTER_PI = PI / 4;
    
    /** Convert degrees to radians. */
    public static final float DEG_TO_RAD = PI / 180;
    
    /** Convert radians to degrees. */
    public static final float RAD_TO_DEG = 180 / PI;
    
    /** Epsilon for floating-point comparisons. */
    public static final float EPSILON = 1e-6f;
    
    /** Small epsilon. */
    public static final float EPSILON_SMALL = 1e-4f;
    
    /** Large epsilon. */
    public static final float EPSILON_LARGE = 1e-2f;
    
    // ==================== Private Constructor ====================
    
    private MathUtils() {} // Prevent instantiation
    
    // ==================== Clamping ====================
    
    /**
     * Clamps a value between min and max.
     * 
     * @param value The value to clamp
     * @param min The minimum value
     * @param max The maximum value
     * @return The clamped value
     */
    public static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    
    /**
     * Clamps a value between 0 and 1.
     * 
     * @param value The value to clamp
     * @return The clamped value
     */
    public static float clamp01(float value) {
        return clamp(value, 0, 1);
    }
    
    /**
     * Clamps an integer between min and max.
     * 
     * @param value The value to clamp
     * @param min The minimum value
     * @param max The maximum value
     * @return The clamped value
     */
    public static int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    
    // ==================== Interpolation ====================
    
    /**
     * Linear interpolation between two values.
     * 
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor (0-1)
     * @return Interpolated value
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    /**
     * Linear interpolation with clamping.
     * 
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor (clamped to 0-1)
     * @return Interpolated value
     */
    public static float lerpClamped(float a, float b, float t) {
        return lerp(a, b, clamp01(t));
    }
    
    /**
     * Linear interpolation for vectors.
     * 
     * @param start Start vector
     * @param end End vector
     * @param t Interpolation factor
     * @param result Vector to store result
     * @return The result vector
     */
    public static Vector3 lerp(Vector3 start, Vector3 end, float t, Vector3 result) {
        result.set(start).lerp(end, t);
        return result;
    }
    
    /**
     * Spherical linear interpolation for quaternions.
     * 
     * @param a Start quaternion
     * @param b End quaternion
     * @param t Interpolation factor
     * @param result Quaternion to store result
     * @return The result quaternion
     */
    public static Quaternion slerp(Quaternion a, Quaternion b, float t, Quaternion result) {
        result.set(a).slerp(b, t);
        return result;
    }
    
    /**
     * Ease-in interpolation.
     * 
     * @param t Interpolation factor
     * @return Eased value
     */
    public static float easeIn(float t) {
        return t * t;
    }
    
    /**
     * Ease-out interpolation.
     * 
     * @param t Interpolation factor
     * @return Eased value
     */
    public static float easeOut(float t) {
        return t * (2 - t);
    }
    
    /**
     * Ease-in-out interpolation.
     * 
     * @param t Interpolation factor
     * @return Eased value
     */
    public static float easeInOut(float t) {
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }
    
    /**
     * Smooth step interpolation.
     * 
     * @param t Interpolation factor
     * @return Smoothed value
     */
    public static float smoothStep(float t) {
        return t * t * (3 - 2 * t);
    }
    
    /**
     * Smoother step interpolation.
     * 
     * @param t Interpolation factor
     * @return Smoothed value
     */
    public static float smootherStep(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    
    /**
     * Bounce interpolation.
     * 
     * @param t Interpolation factor
     * @return Bounced value
     */
    public static float bounce(float t) {
        if (t < 1 / 2.75f) {
            return 7.5625f * t * t;
        } else if (t < 2 / 2.75f) {
            t -= 1.5f / 2.75f;
            return 7.5625f * t * t + 0.75f;
        } else if (t < 2.5 / 2.75f) {
            t -= 2.25f / 2.75f;
            return 7.5625f * t * t + 0.9375f;
        } else {
            t -= 2.625f / 2.75f;
            return 7.5625f * t * t + 0.984375f;
        }
    }
    
    // ==================== Angular ====================
    
    /**
     * Normalizes an angle to the range [-PI, PI).
     * 
     * @param angle The angle in radians
     * @return Normalized angle
     */
    public static float normalizeAngle(float angle) {
        angle %= PI2;
        if (angle < -PI) angle += PI2;
        if (angle >= PI) angle -= PI2;
        return angle;
    }
    
    /**
     * Normalizes an angle to the range [0, 2*PI).
     * 
     * @param angle The angle in radians
     * @return Normalized angle
     */
    public static float normalizeAnglePositive(float angle) {
        angle %= PI2;
        if (angle < 0) angle += PI2;
        return angle;
    }
    
    /**
     * Gets the smallest angle difference between two angles.
     * 
     * @param from Starting angle
     * @param to Ending angle
     * @return Angle difference in range [-PI, PI]
     */
    public static float angleDifference(float from, float to) {
        float diff = to - from;
        diff = normalizeAnglePositive(diff);
        if (diff > PI) diff -= PI2;
        return diff;
    }
    
    /**
     * Interpolates angles with shortest path.
     * 
     * @param from Starting angle
     * @param to Ending angle
     * @param t Interpolation factor
     * @return Interpolated angle
     */
    public static float lerpAngle(float from, float to, float t) {
        return from + angleDifference(from, to) * t;
    }
    
    // ==================== Rounding ====================
    
    /**
     * Rounds to the nearest integer.
     * 
     * @param value The value to round
     * @return Rounded integer
     */
    public static int round(float value) {
        return Math.round(value);
    }
    
    /**
     * Rounds to a specific number of decimal places.
     * 
     * @param value The value to round
     * @param decimals Number of decimal places
     * @return Rounded value
     */
    public static float round(float value, int decimals) {
        float factor = (float) Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
    
    /**
     * Rounds up to the nearest integer.
     * 
     * @param value The value to round up
     * @return Ceiling value
     */
    public static int ceil(float value) {
        return (int) Math.ceil(value);
    }
    
    /**
     * Rounds down to the nearest integer.
     * 
     * @param value The value to round down
     * @return Floor value
     */
    public static int floor(float value) {
        return (int) Math.floor(value);
    }
    
    // ==================== Random ====================
    
    /**
     * Gets a random float between 0 and 1.
     * 
     * @return Random value
     */
    public static float random() {
        return (float) Math.random();
    }
    
    /**
     * Gets a random float in a range.
     * 
     * @param min Minimum value (inclusive)
     * @param max Maximum value (exclusive)
     * @return Random value
     */
    public static float random(float min, float max) {
        return min + random() * (max - min);
    }
    
    /**
     * Gets a random integer in a range.
     * 
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return Random value
     */
    public static int randomInt(int min, int max) {
        return min + (int) (random() * (max - min + 1));
    }
    
    /**
     * Randomly chooses true or false.
     * 
     * @return Random boolean
     */
    public static boolean randomBool() {
        return random() < 0.5f;
    }
    
    /**
     * Randomly chooses true with specified probability.
     * 
     * @param probability Probability of true (0-1)
     * @return Random boolean
     */
    public static boolean randomBool(float probability) {
        return random() < probability;
    }
    
    /**
     * Gets a random element from an array.
     * 
     * @param <T> Element type
     * @param array The array
     * @return Random element
     */
    public static <T> T randomElement(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        }
        return array[randomInt(0, array.length - 1)];
    }
    
    /**
     * Gets a random element from a list.
     * 
     * @param <T> Element type
     * @param list The list
     * @return Random element
     */
    public static <T> T randomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(randomInt(0, list.size() - 1));
    }
    
    // ==================== Vector Operations ====================
    
    /**
     * Gets the magnitude of a 2D vector.
     * 
     * @param x X component
     * @param y Y component
     * @return Magnitude
     */
    public static float magnitude(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }
    
    /**
     * Gets the squared magnitude of a 2D vector.
     * 
     * @param x X component
     * @param y Y component
     * @return Squared magnitude
     */
    public static float magnitudeSquared(float x, float y) {
        return x * x + y * y;
    }
    
    /**
     * Gets the magnitude of a 3D vector.
     * 
     * @param x X component
     * @param y Y component
     * @param z Z component
     * @return Magnitude
     */
    public static float magnitude(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
    
    /**
     * Gets the squared magnitude of a 3D vector.
     * 
     * @param x X component
     * @param y Y component
     * @param z Z component
     * @return Squared magnitude
     */
    public static float magnitudeSquared(float x, float y, float z) {
        return x * x + y * y + z * z;
    }
    
    /**
     * Normalizes a 2D vector.
     * 
     * @param x X component
     * @param y Y component
     * @param result Result vector
     * @return Normalized vector
     */
    public static Vector2 normalize(float x, float y, Vector2 result) {
        float mag = magnitude(x, y);
        if (mag > EPSILON) {
            result.set(x / mag, y / mag);
        } else {
            result.set(0, 0);
        }
        return result;
    }
    
    /**
     * Dot product of two 2D vectors.
     * 
     * @param ax First vector X
     * @param ay First vector Y
     * @param bx Second vector X
     * @param by Second vector Y
     * @return Dot product
     */
    public static float dot(float ax, float ay, float bx, float by) {
        return ax * bx + ay * by;
    }
    
    /**
     * Cross product of two 2D vectors.
     * 
     * @param ax First vector X
     * @param ay First vector Y
     * @param bx Second vector X
     * @param by Second vector Y
     * @return Cross product (z component)
     */
    public static float cross(float ax, float ay, float bx, float by) {
        return ax * by - ay * bx;
    }
    
    /**
     * Perpendicular to a 2D vector (rotated 90 degrees counter-clockwise).
     * 
     * @param x X component
     * @param y Y component
     * @param result Result vector
     * @return Perpendicular vector
     */
    public static Vector2 perpendicular(float x, float y, Vector2 result) {
        result.set(-y, x);
        return result;
    }
    
    // ==================== Color Operations ====================
    
    /**
     * Linear to gamma color conversion.
     * 
     * @param value Linear color value
     * @param gamma Gamma value (typically 2.2)
     * @return Gamma-corrected value
     */
    public static float linearToGamma(float value, float gamma) {
        return (float) Math.pow(value, 1 / gamma);
    }
    
    /**
     * Gamma to linear color conversion.
     * 
     * @param value Gamma color value
     * @param gamma Gamma value (typically 2.2)
     * @return Linear value
     */
    public static float gammaToLinear(float value, float gamma) {
        return (float) Math.pow(value, gamma);
    }
    
    /**
     * Mixes two colors.
     * 
     * @param color1 First color
     * @param color2 Second color
     * @param t Mix factor (0-1)
     * @param result Result color
     * @return Mixed color
     */
    public static Color mix(Color color1, Color color2, float t, Color result) {
        result.set(
            lerp(color1.r, color2.r, t),
            lerp(color1.g, color2.g, t),
            lerp(color1.b, color2.b, t),
            lerp(color1.a, color2.a, t)
        );
        return result;
    }
    
    // ==================== Comparison ====================
    
    /**
     * Checks if two floats are approximately equal.
     * 
     * @param a First value
     * @param b Second value
     * @return true if approximately equal
     */
    public static boolean approx(float a, float b) {
        return Math.abs(a - b) <= EPSILON;
    }
    
    /**
     * Checks if two floats are approximately equal with custom epsilon.
     * 
     * @param a First value
     * @param b Second value
     * @param epsilon Comparison threshold
     * @return true if approximately equal
     */
    public static boolean approx(float a, float b, float epsilon) {
        return Math.abs(a - b) <= epsilon;
    }
    
    /**
     * Checks if a float is zero (approximately).
     * 
     * @param value Value to check
     * @return true if approximately zero
     */
    public static boolean isZero(float value) {
        return Math.abs(value) <= EPSILON;
    }
    
    /**
     * Checks if a float is positive.
     * 
     * @param value Value to check
     * @return true if greater than epsilon
     */
    public static boolean isPositive(float value) {
        return value > EPSILON;
    }
    
    /**
     * Checks if a float is negative.
     * 
     * @param value Value to check
     * @return true if less than negative epsilon
     */
    public static boolean isNegative(float value) {
        return value < -EPSILON;
    }
    
    // ==================== Utility ====================
    
    /**
     * Calculates a smooth delta time value.
     * 
     * @param currentDelta Current frame delta
     * @param previousDelta Previous frame delta
     * @param alpha Smoothing factor
     * @return Smoothed delta
     */
    public static float smoothDelta(float currentDelta, float previousDelta, float alpha) {
        return lerp(previousDelta, currentDelta, alpha);
    }
    
    /**
     * Calculates the factorial of a number.
     * 
     * @param n The number
     * @return Factorial
     */
    public static long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Cannot calculate factorial of negative number");
        }
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
    
    /**
     * Calculates combinations (n choose k).
     * 
     * @param n Total items
     * @param k Items to choose
     * @return Number of combinations
     */
    public static long combinations(int n, int k) {
        if (k < 0 || k > n) {
            return 0;
        }
        if (k == 0 || k == n) {
            return 1;
        }
        if (k > n / 2) {
            k = n - k;
        }
        long result = 1;
        for (int i = 0; i < k; i++) {
            result = result * (n - i) / (i + 1);
        }
        return result;
    }
    
    /**
     * Calculates the Fibonacci number at an index.
     * 
     * @param n Index
     * @return Fibonacci number
     */
    public static long fibonacci(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Fibonacci index cannot be negative");
        }
        if (n <= 1) {
            return n;
        }
        long a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            long c = a + b;
            a = b;
            b = c;
        }
        return b;
    }
    
    /**
     * Checks if a number is a power of two.
     * 
     * @param value Value to check
     * @return true if power of two
     */
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }
    
    /**
     * Rounds up to the next power of two.
     * 
     * @param value Value to round up
     * @return Next power of two
     */
    public static int nextPowerOfTwo(int value) {
        if (value <= 0) {
            return 1;
        }
        value--;
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        value++;
        return value;
    }
    
    /**
     * Converts a value from one range to another.
     * 
     * @param value Value to convert
     * @param inMin Input range minimum
     * @param inMax Input range maximum
     * @param outMin Output range minimum
     * @param outMax Output range maximum
     * @return Converted value
     */
    public static float mapRange(float value, float inMin, float inMax, float outMin, float outMax) {
        return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }
    
    /**
     * Converts a value from 0-1 range to another range.
     * 
     * @param value Value to convert
     * @param outMin Output range minimum
     * @param outMax Output range maximum
     * @return Converted value
     */
    public static float mapRange01(float value, float outMin, float outMax) {
        return value * (outMax - outMin) + outMin;
    }
}
