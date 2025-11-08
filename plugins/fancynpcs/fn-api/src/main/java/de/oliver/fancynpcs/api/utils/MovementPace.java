package de.oliver.fancynpcs.api.utils;

/**
 * Represents the movement speed/style for an NPC
 */
public enum MovementPace {
    WALK(0.2, NpcPose.STANDING, false),
    SNEAK(0.1, NpcPose.SNEAKING, false),
    SPRINT(0.28, NpcPose.STANDING, true),
    SPRINT_JUMP(0.3, NpcPose.STANDING, true),
    SWIM(0.15, NpcPose.SWIMMING, false);

    private final double speed;
    private final NpcPose pose;
    private final boolean sprinting;

    MovementPace(double speed, NpcPose pose, boolean sprinting) {
        this.speed = speed;
        this.pose = pose;
        this.sprinting = sprinting;
    }

    /**
     * Gets the movement speed in blocks per tick
     *
     * @return the speed multiplier
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * Gets the visual pose for this pace
     *
     * @return the pose to display
     */
    public NpcPose getPose() {
        return pose;
    }

    /**
     * Whether the NPC should be marked as sprinting
     *
     * @return true if sprinting
     */
    public boolean isSprinting() {
        return sprinting;
    }

    /**
     * Represents the visual pose/animation state
     */
    public enum NpcPose {
        STANDING,
        SNEAKING,
        SWIMMING,
        SLEEPING,
        DYING
    }
}
