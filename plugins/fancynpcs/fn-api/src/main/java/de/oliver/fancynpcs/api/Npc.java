package de.oliver.fancynpcs.api;

import de.oliver.fancylib.RandomUtils;
import de.oliver.fancylib.translations.Translator;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutor;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import de.oliver.fancynpcs.api.utils.Interval;
import de.oliver.fancynpcs.api.utils.Interval.Unit;
import de.oliver.fancynpcs.api.utils.MovementMode;
import de.oliver.fancynpcs.api.utils.MovementPace;
import de.oliver.fancynpcs.api.utils.MovementPath;
import de.oliver.fancynpcs.api.utils.PathPosition;
import de.oliver.fancynpcs.api.utils.RotationMode;
import de.oliver.fancynpcs.api.utils.WalkingOrder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Npc {

    private static final NpcAttribute INVISIBLE_ATTRIBUTE = FancyNpcsPlugin.get().getAttributeManager().getAttributeByName(EntityType.PLAYER, "invisible");
    private static final char[] localNameChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'};
    protected final Map<UUID, Boolean> isTeamCreated = new ConcurrentHashMap<>();
    protected final Map<UUID, Boolean> isVisibleForPlayer = new ConcurrentHashMap<>();
    protected final Map<UUID, Boolean> isLookingAtPlayer = new ConcurrentHashMap<>();
    protected final Map<UUID, Long> lastPlayerInteraction = new ConcurrentHashMap<>();
    private final Translator translator = FancyNpcsPlugin.get().getTranslator();
    protected NpcData data;
    protected boolean saveToFile;
    private BukkitTask movementTask;
    private int currentPathIndex = 0;
    private boolean isMoving = false;
    private boolean isWaitingAtPosition = false;
    private long waitStartTime = 0;
    private UUID guidingPlayer = null; // For GUIDE mode
    private long stuckCheckTime = 0;
    private Location lastStuckCheckLocation = null;
    private boolean movingForward = true; // For BACKWARDS mode
    private int previousPathIndex = -1; // For RANDOM mode to avoid going back
    private double verticalVelocity = 0.0; // For realistic gravity and falling
    private boolean wasOnGround = false; // Track if was on ground last tick
    private int targetPosition = -1; // For goToPosition - stops after reaching this position

    public Npc(NpcData data) {
        this.data = data;
        this.saveToFile = true;
    }

    protected String generateLocalName() {
        String localName = "";
        for (int i = 0; i < 8; i++) {
            localName += "&" + localNameChars[(int) RandomUtils.randomInRange(0, localNameChars.length)];
        }

        localName = ChatColor.translateAlternateColorCodes('&', localName);

        return localName;
    }

    public abstract void create();

    public abstract void spawn(Player player);

    public void spawnForAll() {
        FancyNpcsPlugin.get().getNpcThread().submit(() -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                spawn(onlinePlayer);
            }
        });
    }

    public abstract void remove(Player player);

    public void removeForAll() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            remove(onlinePlayer);
        }
    }

    /**
     * Checks if the NPC should be visible for the player.
     *
     * @param player The player to check for.
     * @return True if the NPC should be visible for the player, otherwise false.
     */
    protected boolean shouldBeVisible(Player player) {
        int visibilityDistance = (data.getVisibilityDistance() > -1) ? data.getVisibilityDistance() : FancyNpcsPlugin.get().getFancyNpcConfig().getVisibilityDistance();

        if (visibilityDistance == 0) {
            return false;
        }

        if (!data.isSpawnEntity()) {
            return false;
        }

        if (data.getLocation() == null) {
            return false;
        }

        if (player.getLocation().getWorld() != data.getLocation().getWorld()) {
            return false;
        }

        if (visibilityDistance != Integer.MAX_VALUE && data.getLocation().distanceSquared(player.getLocation()) > visibilityDistance * visibilityDistance) {
            return false;
        }

        if (FancyNpcsPlugin.get().getFancyNpcConfig().isSkipInvisibleNpcs() && data.getAttributes().getOrDefault(INVISIBLE_ATTRIBUTE, "false").equalsIgnoreCase("true") && !data.isGlowing() && data.getEquipment().isEmpty()) {
            return false;
        }

        return true;
    }

    public void checkAndUpdateVisibility(Player player) {
        FancyNpcsPlugin.get().getNpcThread().submit(() -> {
            boolean shouldBeVisible = shouldBeVisible(player);
            boolean wasVisible = isVisibleForPlayer.getOrDefault(player.getUniqueId(), false);

            if (shouldBeVisible && !wasVisible) {
                spawn(player);
            } else if (!shouldBeVisible && wasVisible) {
                remove(player);
            }
        });
    }

    public abstract void lookAt(Player player, Location location);

    /**
     * Sets the visual pose for an NPC for a specific player using the attribute system
     */
    public void setPose(Player player, MovementPace.NpcPose pose) {
        // Get the pose attribute from the attribute manager
        NpcAttribute poseAttribute = FancyNpcsPlugin.get().getAttributeManager()
                .getAttributeByName(data.getType(), "pose");

        if (poseAttribute != null) {
            // Convert our NpcPose enum to the attribute value string
            String poseValue = pose.name().toLowerCase();
            // Apply the attribute (this will handle version-specific implementation)
            poseAttribute.apply(this, poseValue);
        }
    }

    /**
     * Sets sprinting state - currently implemented via entity flags in subclasses
     */
    public void setSprinting(Player player, boolean sprinting) {
        // Sprinting is handled via entity data flags
        // This is a no-op in the base class as implementations handle this
        // through the entity's shared flags system
    }

    public void setPoseForAll(MovementPace.NpcPose pose) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            setPose(onlinePlayer, pose);
        }
    }

    public void setSprintingForAll(boolean sprinting) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            setSprinting(onlinePlayer, sprinting);
        }
    }

    public abstract void update(Player player, boolean swingArm);

    public void update(Player player) {
        update(player, FancyNpcsPlugin.get().getFancyNpcConfig().isSwingArmOnUpdate());
    }

    public void updateForAll(boolean swingArm) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            update(onlinePlayer, swingArm);
        }
    }

    public void updateForAll() {
        updateForAll(FancyNpcsPlugin.get().getFancyNpcConfig().isSwingArmOnUpdate());
    }

    public abstract void move(Player player, boolean swingArm);

    public void move(Player player) {
        move(player, FancyNpcsPlugin.get().getFancyNpcConfig().isSwingArmOnUpdate());
    }

    public void moveForAll(boolean swingArm) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            move(onlinePlayer, swingArm);
        }
    }

    public void moveForAll() {
        moveForAll(FancyNpcsPlugin.get().getFancyNpcConfig().isSwingArmOnUpdate());
    }

    public void interact(Player player) {
        interact(player, ActionTrigger.CUSTOM);
    }

    public void interact(Player player, ActionTrigger actionTrigger) {
        if (data.getInteractionCooldown() > 0) {
            final long interactionCooldownMillis = (long) (data.getInteractionCooldown() * 1000);
            final long lastInteractionMillis = lastPlayerInteraction.getOrDefault(player.getUniqueId(), 0L);
            final Interval interactionCooldownLeft = Interval.between(lastInteractionMillis + interactionCooldownMillis, System.currentTimeMillis(), Unit.MILLISECONDS);
            if (interactionCooldownLeft.as(Unit.MILLISECONDS) > 0) {

                if (!FancyNpcsPlugin.get().getFancyNpcConfig().isInteractionCooldownMessageDisabled()) {
                    translator.translate("interaction_on_cooldown").replace("time", interactionCooldownLeft.toString()).send(player);
                }

                return;
            }
            lastPlayerInteraction.put(player.getUniqueId(), System.currentTimeMillis());
        }

        List<NpcAction.NpcActionData> actions = data.getActions(actionTrigger);
        NpcInteractEvent npcInteractEvent = new NpcInteractEvent(this, data.getOnClick(), actions, player, actionTrigger);
        npcInteractEvent.callEvent();

        if (npcInteractEvent.isCancelled()) {
            return;
        }

        // onClick
        if (data.getOnClick() != null) {
            data.getOnClick().accept(player);
        }

        // actions
        ActionExecutor.execute(actionTrigger, this, player);
        
        if (actionTrigger == ActionTrigger.LEFT_CLICK || actionTrigger == ActionTrigger.RIGHT_CLICK) {
            ActionExecutor.execute(ActionTrigger.ANY_CLICK, this, player);
        }
    }

    protected abstract void refreshEntityData(Player serverPlayer);

    public abstract int getEntityId();

    public NpcData getData() {
        return data;
    }

    public abstract float getEyeHeight();

    public Map<UUID, Boolean> getIsTeamCreated() {
        return isTeamCreated;
    }

    public Map<UUID, Boolean> getIsVisibleForPlayer() {
        return isVisibleForPlayer;
    }

    public Map<UUID, Boolean> getIsLookingAtPlayer() {
        return isLookingAtPlayer;
    }

    public Map<UUID, Long> getLastPlayerInteraction() {
        return lastPlayerInteraction;
    }

    public boolean isDirty() {
        return data.isDirty();
    }

    public void setDirty(boolean dirty) {
        data.setDirty(dirty);
    }

    public boolean isSaveToFile() {
        return saveToFile;
    }

    public void setSaveToFile(boolean saveToFile) {
        this.saveToFile = saveToFile;
    }

    /* ========== MOVEMENT SYSTEM ========== */

    /**
     * Starts movement along the current path
     */
    public void startMovement() {
        startMovement(data.getCurrentPathName());
    }

    /**
     * Starts movement along a specified path
     */
    public void startMovement(String pathName) {
        MovementPath path = data.getPath(pathName);
        if (path == null || path.getPositions().isEmpty()) {
            return;
        }

        data.setCurrentPath(pathName);

        if (isMoving) {
            stopMovement();
        }

        isMoving = true;
        data.setWalking(true); // Set walking state to true
        currentPathIndex = 0;
        isWaitingAtPosition = false;
        stuckCheckTime = System.currentTimeMillis();
        lastStuckCheckLocation = data.getLocation().clone();

        movementTask = Bukkit.getScheduler().runTaskTimer(FancyNpcsPlugin.get().getPlugin(), this::updateMovement, 0L, 1L);
    }

    /**
     * Stops the NPC's movement
     */
    public void stopMovement() {
        if (movementTask != null) {
            movementTask.cancel();
            movementTask = null;
        }
        isMoving = false;
        data.setWalking(false); // Set walking state to false
        isWaitingAtPosition = false;
        guidingPlayer = null;
        targetPosition = -1; // Clear target position when stopping
    }

    /**
     * Moves to the next position (for GUIDE/MANUAL modes)
     */
    public void moveToNextPosition() {
        MovementPath path = data.getCurrentPath();
        if (path == null) return;

        if (!isMoving) {
            startMovement();
            return;
        }

        if (isWaitingAtPosition) {
            isWaitingAtPosition = false;
            advanceToNextPosition(path);
        }
    }

    /**
     * Moves to the previous position
     */
    public void moveToPreviousPosition() {
        MovementPath path = data.getCurrentPath();
        if (path == null || currentPathIndex <= 0) return;

        isWaitingAtPosition = false;
        currentPathIndex--;
    }

    /**
     * Returns to the start of the path
     */
    public void returnToStart() {
        MovementPath path = data.getCurrentPath();
        if (path == null || path.getPositions().isEmpty()) return;

        PathPosition firstPos = path.getPositions().get(0);
        Location startLoc = new Location(
            data.getLocation().getWorld(),
            firstPos.x(),
            firstPos.y(),
            firstPos.z(),
            firstPos.yaw(),
            firstPos.pitch()
        );

        data.setLocation(startLoc);
        moveForAll(false);
        currentPathIndex = 0;
        stopMovement();
    }

    /**
     * Walks to a specific position and stops there
     */
    public void goToPosition(int position) {
        MovementPath path = data.getCurrentPath();
        if (path == null || path.getPositions().isEmpty()) return;

        // Validate position index
        if (position < 0 || position >= path.getPositions().size()) return;

        // Set target position to stop at
        targetPosition = position;

        // If already at target position, just stop
        if (currentPathIndex == position) {
            stopMovement();
            targetPosition = -1;
            return;
        }

        // Stop current movement and start fresh
        stopMovement();

        // Set current index to start walking towards target
        // We need to be smart about the path - just set it one position before target
        // Or set it to current position and let advanceToNextPosition handle it
        // For now, let's use a simple approach: start from current position

        // Start movement - it will walk until reaching targetPosition
        startMovement();
    }

    /**
     * Advances to the next position index based on walking order
     */
    private void advanceToNextPosition(MovementPath path) {
        int pathSize = path.getPositions().size();
        if (pathSize == 0) return;

        // If we have a target position set, check if we've reached it
        if (targetPosition >= 0 && currentPathIndex == targetPosition) {
            stopMovement();
            targetPosition = -1;
            return;
        }

        switch (path.getWalkingOrder()) {
            case NORMAL:
                // Forward: A → B → C → D
                currentPathIndex++;
                if (currentPathIndex >= pathSize) {
                    if (path.isLoop()) {
                        currentPathIndex = 0;
                    } else {
                        stopMovement();
                    }
                }
                break;

            case BACKWARDS:
                // Reverse: D → C → B → A
                currentPathIndex--;
                if (currentPathIndex < 0) {
                    if (path.isLoop()) {
                        currentPathIndex = pathSize - 1; // Loop back to end
                    } else {
                        stopMovement();
                    }
                }
                break;

            case PING_PONG:
                // Ping-pong: A → B → C → D → C → B → A
                if (movingForward) {
                    currentPathIndex++;
                    if (currentPathIndex >= pathSize - 1) {
                        // Reached end (or second-to-last for size 2), reverse direction
                        movingForward = false;
                        if (pathSize == 1) {
                            currentPathIndex = 0; // Single position, stay
                        } else if (pathSize == 2) {
                            currentPathIndex = 0; // For 2 positions, go back to first
                        } else {
                            currentPathIndex = pathSize - 2; // Go back to second-to-last
                        }
                    }
                } else {
                    currentPathIndex--;
                    if (currentPathIndex <= 0) {
                        // Reached start, reverse direction
                        movingForward = true;
                        if (pathSize == 1) {
                            currentPathIndex = 0; // Single position, stay
                        } else {
                            currentPathIndex = 1; // Go forward to second position
                        }
                    }
                }
                break;

            case RANDOM:
                // Random: Visits positions randomly, avoiding previous position
                if (pathSize > 1) {
                    // Store current position as previous before changing
                    previousPathIndex = currentPathIndex;
                    int nextIndex;
                    do {
                        nextIndex = (int) (Math.random() * pathSize);
                    } while ((nextIndex == currentPathIndex || nextIndex == previousPathIndex) && pathSize > 2);
                    currentPathIndex = nextIndex;
                } else {
                    currentPathIndex = 0;
                }
                break;
        }
    }

    /**
     * Main movement update loop
     */
    private void updateMovement() {
        MovementPath path = data.getCurrentPath();
        if (path == null || path.getPositions().isEmpty()) {
            stopMovement();
            return;
        }

        List<PathPosition> positions = path.getPositions();

        // Handle path completion
        if (currentPathIndex >= positions.size()) {
            if (path.isLoop()) {
                currentPathIndex = 0;
            } else {
                stopMovement();
                return;
            }
        }

        // Handle wait time at position
        if (isWaitingAtPosition) {
            float waitTime = path.getWaitTime(currentPathIndex - 1);
            if (waitTime > 0) {
                long elapsed = (System.currentTimeMillis() - waitStartTime) / 1000;
                if (elapsed < waitTime) {
                    return; // Still waiting
                }
            }

            // For GUIDE mode, wait for player interaction
            if (path.getMovementMode() == MovementMode.GUIDE) {
                checkFollowDistance();
                return; // Wait for next_position action
            }

            // Done waiting, continue to next
            isWaitingAtPosition = false;
            advanceToNextPosition(path);
        }

        // Get current and target positions
        PathPosition targetPos = positions.get(currentPathIndex);
        Location currentLoc = data.getLocation();
        Location targetLoc = new Location(
            currentLoc.getWorld(),
            targetPos.x(),
            targetPos.y(),
            targetPos.z(),
            targetPos.yaw(),
            targetPos.pitch()
        );

        // Calculate movement
        double distance = currentLoc.distance(targetLoc);
        double speed = getSegmentSpeed(path, currentPathIndex);

        // Get current pace and apply visuals
        MovementPace currentPace = getCurrentPace(path, currentPathIndex);
        applyPaceVisuals(currentPace);

        if (distance <= speed) {
            // Arrived at position
            onArriveAtPosition(path, currentPathIndex, targetLoc);
        } else {
            // Move towards target
            moveTowardsPosition(path, currentLoc, targetLoc, speed, targetPos);
        }

        // Stuck detection
        checkIfStuck(currentLoc);
    }

    /**
     * Handles arrival at a position
     */
    private void onArriveAtPosition(MovementPath path, int positionIndex, Location targetLoc) {
        data.setLocation(targetLoc);
        moveForAll(false);

        PathPosition currentPos = path.getPositions().get(positionIndex);

        // Waypoints: pass through without stopping
        if (currentPos.isWaypoint()) {
            // Skip to next position immediately
            advanceToNextPosition(path);
            return;
        }

        // Regular positions: rotate, execute actions, and wait
        // Rotate on arrival if needed
        if (path.getRotationMode() == RotationMode.ON_ARRIVAL) {
            // Rotation already set in targetLoc
            updateForAll();
        }

        // Execute position-specific action (old trigger-based system - deprecated)
        ActionTrigger posAction = path.getPositionAction(positionIndex);
        if (posAction != null) {
            // Execute all actions for this trigger
            for (Player player : Bukkit.getOnlinePlayers()) {
                ActionExecutor.execute(posAction, this, player);
            }
        }

        // Execute position-specific action list (new system)
        List<NpcAction.NpcActionData> positionActions = path.getActionsAtPosition(positionIndex);
        if (!positionActions.isEmpty()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                for (NpcAction.NpcActionData actionData : positionActions) {
                    // Create a minimal context for this action
                    ActionExecutionContext context = new ActionExecutionContext(ActionTrigger.CUSTOM, this, player.getUniqueId());
                    actionData.action().execute(context, actionData.value());
                }
            }
        }

        // Start wait period
        isWaitingAtPosition = true;
        waitStartTime = System.currentTimeMillis();
    }

    /**
     * Moves NPC towards a target position
     */
    private void moveTowardsPosition(MovementPath path, Location currentLoc, Location targetLoc, double speed, PathPosition targetPos) {
        Vector direction = targetLoc.toVector().subtract(currentLoc.toVector()).normalize();
        Vector movement = direction.multiply(speed);

        Location newLoc = currentLoc.clone().add(movement);

        // Apply physics if enabled (either globally or for this path)
        if (data.usePhysics() || path.usePhysics()) {
            newLoc = applyPhysics(currentLoc, newLoc, movement);
        }

        // Handle rotation mode
        if (path.getRotationMode() == RotationMode.SMOOTH) {
            // Calculate yaw and pitch towards target
            Vector dir = targetLoc.toVector().subtract(newLoc.toVector());
            Location lookAt = newLoc.clone().setDirection(dir);
            newLoc.setYaw(lookAt.getYaw());
            newLoc.setPitch(lookAt.getPitch());
        } else if (path.getRotationMode() == RotationMode.ON_ARRIVAL) {
            // Look in the direction of movement while walking, rotate to target yaw/pitch on arrival
            Vector dir = targetLoc.toVector().subtract(newLoc.toVector());
            Location lookAt = newLoc.clone().setDirection(dir);
            newLoc.setYaw(lookAt.getYaw());
            newLoc.setPitch(0); // Keep pitch level while walking
        } else if (path.getRotationMode() == RotationMode.NONE) {
            // Keep current rotation
            newLoc.setYaw(currentLoc.getYaw());
            newLoc.setPitch(currentLoc.getPitch());
        }

        data.setLocation(newLoc);
        moveForAll(false);
    }

    /**
     * Applies player-like physics: gravity, collision, and step-up
     */
    private Location applyPhysics(Location from, Location to, Vector movement) {
        Location result = to.clone();

        // Check if the target horizontal position has a solid block
        Location horizontalTarget = from.clone();
        horizontalTarget.setX(to.getX());
        horizontalTarget.setZ(to.getZ());

        // Try to step up blocks (players can step up 0.6 blocks, but we use 1.0 for full blocks)
        boolean canMove = true;
        double stepHeight = 0.0;

        // Check if there's a block at the target position
        if (isSolidBlock(horizontalTarget)) {
            // Check up to 1 block higher for step-up
            boolean foundStepUp = false;
            for (double height = 0.1; height <= 1.0; height += 0.1) {
                Location stepTest = horizontalTarget.clone().add(0, height, 0);
                if (!isSolidBlock(stepTest)) {
                    // Found valid step height
                    stepHeight = height;
                    foundStepUp = true;
                    break;
                }
            }

            if (!foundStepUp) {
                // Can't step up, block movement
                canMove = false;
                result.setX(from.getX());
                result.setZ(from.getZ());
            } else {
                // Apply step up
                result.add(0, stepHeight, 0);
            }
        }

        if (canMove) {
            // Check if there's ground below the new position
            boolean onGround = isOnGround(result);

            if (onGround) {
                // Snap to ground level
                Location groundLevel = result.clone();
                while (isOnGround(groundLevel) && !isSolidBlock(groundLevel)) {
                    groundLevel.subtract(0, 0.1, 0);
                }
                groundLevel.add(0, 0.1, 0); // Move back up to just above ground

                // Set Y to ground level
                result.setY(groundLevel.getY());
                verticalVelocity = 0.0;
                wasOnGround = true;
            } else {
                // Apply gravity acceleration (Minecraft uses -0.08 per tick, with drag)
                if (!wasOnGround) {
                    // Continue falling with acceleration
                    verticalVelocity -= 0.08; // Gravity acceleration
                    verticalVelocity *= 0.98; // Air resistance
                } else {
                    // Just left ground, start falling
                    verticalVelocity = -0.08;
                }

                // Limit terminal velocity (Minecraft terminal velocity is around -3.92)
                if (verticalVelocity < -3.92) {
                    verticalVelocity = -3.92;
                }

                // Apply vertical velocity
                result.add(0, verticalVelocity, 0);

                // Check if we hit ground while falling
                if (isSolidBlock(result) || isOnGround(result)) {
                    // Hit ground, snap to surface
                    while (isSolidBlock(result)) {
                        result.add(0, 0.1, 0);
                    }
                    verticalVelocity = 0.0;
                    wasOnGround = true;
                } else {
                    wasOnGround = false;
                }
            }
        }

        return result;
    }

    /**
     * Checks if location has solid ground below (within 0.1 blocks)
     */
    private boolean isOnGround(Location loc) {
        Location below = loc.clone().subtract(0, 0.1, 0);
        return below.getBlock().getType().isSolid();
    }

    /**
     * Checks if location has a solid block
     */
    private boolean isSolidBlock(Location loc) {
        return loc.getBlock().getType().isSolid();
    }

    /**
     * Applies the visual pose and sprinting state for the current pace
     */
    private void applyPaceVisuals(MovementPace pace) {
        setPoseForAll(pace.getPose());
        setSprintingForAll(pace.isSprinting());
    }

    /**
     * Gets the pace for current segment (respects per-segment pacing)
     */
    private MovementPace getCurrentPace(MovementPath path, int index) {
        if (index > 0) {
            return path.getSegmentPace(index - 1, index);
        }
        return path.getPace();
    }

    /**
     * Gets the speed for current segment
     */
    private double getSegmentSpeed(MovementPath path, int index) {
        if (index > 0) {
            return path.getSegmentPace(index - 1, index).getSpeed();
        }
        return path.getPace().getSpeed();
    }

    /**
     * Checks if player is too far in GUIDE mode
     */
    private void checkFollowDistance() {
        MovementPath path = data.getCurrentPath();
        if (path.getFollowDistance() <= 0) return;
        if (guidingPlayer == null) return;

        Player player = Bukkit.getPlayer(guidingPlayer);
        if (player == null || !player.isOnline()) {
            guidingPlayer = null;
            return;
        }

        double distance = player.getLocation().distance(data.getLocation());
        // NPC waits if player is too far, continuing in next update when player is close
    }

    /**
     * Detects if NPC is stuck and teleports if needed
     */
    private void checkIfStuck(Location currentLoc) {
        long now = System.currentTimeMillis();
        if (now - stuckCheckTime > 5000) { // Check every 5 seconds
            if (lastStuckCheckLocation != null && lastStuckCheckLocation.distance(currentLoc) < 0.5) {
                // Stuck! Teleport to next position
                MovementPath path = data.getCurrentPath();
                if (path != null && currentPathIndex < path.getPositions().size()) {
                    PathPosition nextPos = path.getPositions().get(currentPathIndex);
                    Location unstuckLoc = new Location(
                        data.getLocation().getWorld(),
                        nextPos.x(),
                        nextPos.y(),
                        nextPos.z(),
                        nextPos.yaw(),
                        nextPos.pitch()
                    );
                    data.setLocation(unstuckLoc);
                    moveForAll(false);
                }
            }
            lastStuckCheckLocation = currentLoc.clone();
            stuckCheckTime = now;
        }
    }

    /**
     * Sets the guiding player for GUIDE mode
     */
    public void setGuidingPlayer(Player player) {
        this.guidingPlayer = player != null ? player.getUniqueId() : null;
    }

    /**
     * Checks if the NPC is currently moving
     */
    public boolean isMoving() {
        return isMoving;
    }
}
