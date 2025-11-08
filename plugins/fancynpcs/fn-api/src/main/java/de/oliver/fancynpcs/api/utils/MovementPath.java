package de.oliver.fancynpcs.api.utils;

import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.api.actions.NpcAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a named movement path with all its configuration
 */
public class MovementPath {
    private final String name;
    private final List<PathPosition> positions;
    private MovementPace pace;
    private boolean loop;
    private WalkingOrder walkingOrder;
    private RotationMode rotationMode;
    private MovementMode movementMode;
    private double followDistance;
    private boolean usePhysics; // Whether to apply player-like physics (gravity, collision, step-up)
    private final Map<Integer, Float> waitTimes; // position index -> wait seconds
    private final Map<Integer, ActionTrigger> positionActions; // position index -> trigger to execute (DEPRECATED - use positionActionList)
    private final Map<Integer, List<NpcAction.NpcActionData>> positionActionList; // position index -> list of actions to execute
    private final Map<String, MovementPace> segmentPaces; // "from-to" -> pace

    public MovementPath(String name) {
        this.name = name;
        this.positions = new ArrayList<>();
        this.pace = MovementPace.WALK;
        this.loop = false;
        this.walkingOrder = WalkingOrder.NORMAL; // Default to NORMAL mode
        this.rotationMode = RotationMode.SMOOTH;
        this.movementMode = MovementMode.CONTINUOUS;
        this.followDistance = -1; // -1 means disabled
        this.usePhysics = false; // Disabled by default for backwards compatibility
        this.waitTimes = new HashMap<>();
        this.positionActions = new HashMap<>();
        this.positionActionList = new HashMap<>();
        this.segmentPaces = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public List<PathPosition> getPositions() {
        return positions;
    }

    public MovementPace getPace() {
        return pace;
    }

    public void setPace(MovementPace pace) {
        this.pace = pace;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public WalkingOrder getWalkingOrder() {
        return walkingOrder;
    }

    public void setWalkingOrder(WalkingOrder walkingOrder) {
        this.walkingOrder = walkingOrder;
    }

    public RotationMode getRotationMode() {
        return rotationMode;
    }

    public void setRotationMode(RotationMode rotationMode) {
        this.rotationMode = rotationMode;
    }

    public MovementMode getMovementMode() {
        return movementMode;
    }

    public void setMovementMode(MovementMode movementMode) {
        this.movementMode = movementMode;
    }

    public double getFollowDistance() {
        return followDistance;
    }

    public void setFollowDistance(double followDistance) {
        this.followDistance = followDistance;
    }

    public boolean usePhysics() {
        return usePhysics;
    }

    public void setUsePhysics(boolean usePhysics) {
        this.usePhysics = usePhysics;
    }

    public Map<Integer, Float> getWaitTimes() {
        return waitTimes;
    }

    public float getWaitTime(int index) {
        return waitTimes.getOrDefault(index, 0f);
    }

    public void setWaitTime(int index, float seconds) {
        if (seconds <= 0) {
            waitTimes.remove(index);
        } else {
            waitTimes.put(index, seconds);
        }
    }

    public Map<Integer, ActionTrigger> getPositionActions() {
        return positionActions;
    }

    public ActionTrigger getPositionAction(int index) {
        return positionActions.get(index);
    }

    public void setPositionAction(int index, ActionTrigger trigger) {
        if (trigger == null) {
            positionActions.remove(index);
        } else {
            positionActions.put(index, trigger);
        }
    }

    public Map<Integer, List<NpcAction.NpcActionData>> getPositionActionList() {
        return positionActionList;
    }

    public List<NpcAction.NpcActionData> getActionsAtPosition(int index) {
        return positionActionList.getOrDefault(index, new ArrayList<>());
    }

    public void addActionAtPosition(int index, NpcAction.NpcActionData action) {
        positionActionList.computeIfAbsent(index, k -> new ArrayList<>()).add(action);
    }

    public void removeActionAtPosition(int index, int actionIndex) {
        List<NpcAction.NpcActionData> actions = positionActionList.get(index);
        if (actions != null && actionIndex >= 0 && actionIndex < actions.size()) {
            actions.remove(actionIndex);
            if (actions.isEmpty()) {
                positionActionList.remove(index);
            }
        }
    }

    public void clearActionsAtPosition(int index) {
        positionActionList.remove(index);
    }

    public Map<String, MovementPace> getSegmentPaces() {
        return segmentPaces;
    }

    public MovementPace getSegmentPace(int from, int to) {
        return segmentPaces.getOrDefault(from + "-" + to, pace);
    }

    public void setSegmentPace(int from, int to, MovementPace segmentPace) {
        if (segmentPace == null) {
            segmentPaces.remove(from + "-" + to);
        } else {
            segmentPaces.put(from + "-" + to, segmentPace);
        }
    }

    public void addPosition(PathPosition position) {
        positions.add(position);
    }

    public void removePosition(int index) {
        if (index >= 0 && index < positions.size()) {
            positions.remove(index);
            // Clean up associated data
            waitTimes.remove(index);
            positionActions.remove(index);
        }
    }

    public void setPosition(int index, PathPosition position) {
        if (index >= 0 && index < positions.size()) {
            positions.set(index, position);
        }
    }

    public void clear() {
        positions.clear();
        waitTimes.clear();
        positionActions.clear();
        segmentPaces.clear();
    }
}
