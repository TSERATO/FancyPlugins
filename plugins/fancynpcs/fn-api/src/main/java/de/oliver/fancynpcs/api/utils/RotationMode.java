package de.oliver.fancynpcs.api.utils;

/**
 * Represents when an NPC should rotate its head during movement
 */
public enum RotationMode {
    /** Rotates head smoothly while walking towards target */
    SMOOTH,

    /** Only rotates head when arriving at position */
    ON_ARRIVAL,

    /** Never rotates head during movement */
    NONE
}
