package de.oliver.fancynpcs.api.utils;

/**
 * Represents the movement behavior mode for an NPC
 */
public enum MovementMode {
    /** Automatically moves through all positions in sequence */
    CONTINUOUS,

    /** Waits at each position for player interaction to continue (guide mode) */
    GUIDE,

    /** Only moves when explicitly triggered by action */
    MANUAL
}
