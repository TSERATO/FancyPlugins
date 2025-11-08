package de.oliver.fancynpcs.api.utils;

/**
 * Represents the order in which an NPC visits positions on its path
 */
public enum WalkingOrder {
    /**
     * Normal: A → B → C → D (forward direction)
     */
    NORMAL,

    /**
     * Backwards: D → C → B → A (reverse direction)
     */
    BACKWARDS,

    /**
     * Ping-Pong: A → B → C → D → C → B → A (back and forth)
     */
    PING_PONG,

    /**
     * Random: Visits positions in random order
     */
    RANDOM
}
