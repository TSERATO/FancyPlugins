package de.oliver.fancynpcs.api.utils;

/**
 * Represents a single position in an NPC's movement path
 *
 * @param x The X coordinate
 * @param y The Y coordinate
 * @param z The Z coordinate
 * @param yaw The yaw rotation (horizontal)
 * @param pitch The pitch rotation (vertical)
 * @param isWaypoint Whether this is a waypoint (passes through without stopping) or a regular position (stops, waits, executes actions)
 */
public record PathPosition(double x, double y, double z, float yaw, float pitch, boolean isWaypoint) {

    /**
     * Constructor for regular positions (backwards compatibility)
     */
    public PathPosition(double x, double y, double z, float yaw, float pitch) {
        this(x, y, z, yaw, pitch, false);
    }
}
