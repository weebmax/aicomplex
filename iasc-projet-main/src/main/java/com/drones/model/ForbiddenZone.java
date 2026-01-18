package com.drones.model;

/**
 * Represents a forbidden zone (obstacles, no-fly areas) in the simulation
 */
public class ForbiddenZone {
    private int x;
    private int y;
    private int width;
    private int height;
    private String name;
    
    /**
     * Create a rectangular forbidden zone
     * @param x top-left x coordinate
     * @param y top-left y coordinate
     * @param width width of the zone
     * @param height height of the zone
     * @param name descriptive name (e.g., "Building A", "Crater")
     */
    public ForbiddenZone(int x, int y, int width, int height, String name) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.name = name;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getName() { return name; }
    
    /**
     * Check if a point is inside this forbidden zone
     */
    public boolean contains(double px, double py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
    
    /**
     * Check if a point is inside this forbidden zone (integer coordinates)
     */
    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
    
    /**
     * Get right boundary (x + width)
     */
    public int getRightBoundary() {
        return x + width;
    }
    
    /**
     * Get bottom boundary (y + height)
     */
    public int getBottomBoundary() {
        return y + height;
    }
}
