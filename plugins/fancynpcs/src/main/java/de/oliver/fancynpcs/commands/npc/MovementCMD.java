package de.oliver.fancynpcs.commands.npc;

import de.oliver.fancylib.translations.Translator;
import de.oliver.fancynpcs.FancyNpcs;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.utils.MovementMode;
import de.oliver.fancynpcs.api.utils.MovementPace;
import de.oliver.fancynpcs.api.utils.MovementPath;
import de.oliver.fancynpcs.api.utils.PathPosition;
import de.oliver.fancynpcs.api.utils.RotationMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public enum MovementCMD {
    INSTANCE;

    private final Translator translator = FancyNpcs.getInstance().getTranslator();

    /* ========== PATH MANAGEMENT ========== */

    @Command("npc movement <npc> create_path <path_name>")
    @Permission("fancynpcs.command.npc.movement.createPath")
    public void onCreatePath(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @NotNull String path_name
    ) {
        if (npc.getData().getPath(path_name) != null) {
            translator.translate("npc_movement_path_already_exists")
                    .replaceStripped("path", path_name)
                    .send(sender);
            return;
        }

        npc.getData().createPath(path_name);
        translator.translate("npc_movement_path_created")
                .replaceStripped("path", path_name)
                .send(sender);
    }

    @Command("npc movement <npc> select_path <path_name>")
    @Permission("fancynpcs.command.npc.movement.selectPath")
    public void onSelectPath(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/path_names") String path_name
    ) {
        if (npc.getData().getPath(path_name) == null) {
            translator.translate("npc_movement_path_not_found")
                    .replaceStripped("path", path_name)
                    .send(sender);
            return;
        }

        npc.getData().setCurrentPath(path_name);
        translator.translate("npc_movement_path_selected")
                .replaceStripped("path", path_name)
                .send(sender);
    }

    @Command("npc movement <npc> remove_path <path_name>")
    @Permission("fancynpcs.command.npc.movement.removePath")
    public void onRemovePath(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/path_names") String path_name
    ) {
        if (path_name.equals("default")) {
            translator.translate("npc_movement_cannot_remove_default").send(sender);
            return;
        }

        if (npc.getData().getPath(path_name) == null) {
            translator.translate("npc_movement_path_not_found")
                    .replaceStripped("path", path_name)
                    .send(sender);
            return;
        }

        npc.getData().removePath(path_name);
        translator.translate("npc_movement_path_removed")
                .replaceStripped("path", path_name)
                .send(sender);
    }

    @Command("npc movement <npc> list_paths")
    @Permission("fancynpcs.command.npc.movement.listPaths")
    public void onListPaths(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc
    ) {
        if (npc.getData().getMovementPaths().isEmpty()) {
            translator.translate("npc_movement_no_paths").send(sender);
            return;
        }

        translator.translate("npc_movement_paths_header")
                .replaceStripped("current", npc.getData().getCurrentPathName())
                .send(sender);

        for (String pathName : npc.getData().getMovementPaths().keySet()) {
            MovementPath path = npc.getData().getPath(pathName);
            boolean isCurrent = pathName.equals(npc.getData().getCurrentPathName());
            translator.translate("npc_movement_paths_entry")
                    .replaceStripped("path", pathName)
                    .replaceStripped("positions", String.valueOf(path.getPositions().size()))
                    .replaceStripped("current", isCurrent ? "✓" : "")
                    .send(sender);
        }

        translator.translate("npc_movement_paths_footer")
                .replaceStripped("total", String.valueOf(npc.getData().getMovementPaths().size()))
                .send(sender);
    }

    /* ========== POSITION MANAGEMENT ========== */

    @Command(value = "npc movement <npc> add_position [x] [y] [z] [yaw] [pitch]", requiredSender = Player.class)
    @Permission("fancynpcs.command.npc.movement.addPosition")
    public void onAddPosition(
            final @NotNull Player sender,
            final @NotNull Npc npc,
            final Double x,
            final Double y,
            final Double z,
            final Float yaw,
            final Float pitch
    ) {
        MovementPath path = npc.getData().getCurrentPath();
        Location loc = sender.getLocation();

        double finalX = x != null ? x : loc.getX();
        double finalY = y != null ? y : loc.getY();
        double finalZ = z != null ? z : loc.getZ();
        float finalYaw = yaw != null ? yaw : loc.getYaw();
        float finalPitch = pitch != null ? pitch : loc.getPitch();

        PathPosition position = new PathPosition(finalX, finalY, finalZ, finalYaw, finalPitch);
        path.addPosition(position);

        translator.translate("npc_movement_position_added")
                .replaceStripped("total", String.valueOf(path.getPositions().size()))
                .replaceStripped("x", String.format("%.2f", finalX))
                .replaceStripped("y", String.format("%.2f", finalY))
                .replaceStripped("z", String.format("%.2f", finalZ))
                .send(sender);
    }

    @Command("npc movement <npc> remove_position <index>")
    @Permission("fancynpcs.command.npc.movement.removePosition")
    public void onRemovePosition(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int index
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (index < 1 || index > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        path.removePosition(index - 1);
        translator.translate("npc_movement_position_removed")
                .replaceStripped("index", String.valueOf(index))
                .replaceStripped("total", String.valueOf(path.getPositions().size()))
                .send(sender);
    }

    @Command(value = "npc movement <npc> set_position <index> [x] [y] [z] [yaw] [pitch]", requiredSender = Player.class)
    @Permission("fancynpcs.command.npc.movement.setPosition")
    public void onSetPosition(
            final @NotNull Player sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int index,
            final Double x,
            final Double y,
            final Double z,
            final Float yaw,
            final Float pitch
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (index < 1 || index > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        PathPosition oldPos = path.getPositions().get(index - 1);
        Location loc = sender.getLocation();

        double finalX = x != null ? x : oldPos.x();
        double finalY = y != null ? y : oldPos.y();
        double finalZ = z != null ? z : oldPos.z();
        float finalYaw = yaw != null ? yaw : oldPos.yaw();
        float finalPitch = pitch != null ? pitch : oldPos.pitch();

        PathPosition newPos = new PathPosition(finalX, finalY, finalZ, finalYaw, finalPitch, oldPos.isWaypoint());
        path.setPosition(index - 1, newPos);

        translator.translate("npc_movement_position_set")
                .replaceStripped("index", String.valueOf(index))
                .replaceStripped("x", String.format("%.2f", finalX))
                .replaceStripped("y", String.format("%.2f", finalY))
                .replaceStripped("z", String.format("%.2f", finalZ))
                .send(sender);
    }

    @Command(value = "npc movement <npc> add_waypoint [x] [y] [z] [yaw] [pitch]", requiredSender = Player.class)
    @Permission("fancynpcs.command.npc.movement.addWaypoint")
    public void onAddWaypoint(
            final @NotNull Player sender,
            final @NotNull Npc npc,
            final Double x,
            final Double y,
            final Double z,
            final Float yaw,
            final Float pitch
    ) {
        MovementPath path = npc.getData().getCurrentPath();
        Location loc = sender.getLocation();

        double finalX = x != null ? x : loc.getX();
        double finalY = y != null ? y : loc.getY();
        double finalZ = z != null ? z : loc.getZ();
        float finalYaw = yaw != null ? yaw : loc.getYaw();
        float finalPitch = pitch != null ? pitch : loc.getPitch();

        PathPosition waypoint = new PathPosition(finalX, finalY, finalZ, finalYaw, finalPitch, true);
        path.addPosition(waypoint);

        translator.translate("npc_movement_waypoint_added")
                .replaceStripped("total", String.valueOf(path.getPositions().size()))
                .replaceStripped("x", String.format("%.2f", finalX))
                .replaceStripped("y", String.format("%.2f", finalY))
                .replaceStripped("z", String.format("%.2f", finalZ))
                .send(sender);
    }

    @Command("npc movement <npc> toggle_waypoint <index>")
    @Permission("fancynpcs.command.npc.movement.toggleWaypoint")
    public void onToggleWaypoint(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int index
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (index < 1 || index > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        PathPosition oldPos = path.getPositions().get(index - 1);
        PathPosition newPos = new PathPosition(oldPos.x(), oldPos.y(), oldPos.z(), oldPos.yaw(), oldPos.pitch(), !oldPos.isWaypoint());
        path.setPosition(index - 1, newPos);

        translator.translate("npc_movement_waypoint_toggled")
                .replaceStripped("index", String.valueOf(index))
                .replaceStripped("type", newPos.isWaypoint() ? "waypoint" : "position")
                .send(sender);
    }

    @Command("npc movement <npc> clear")
    @Permission("fancynpcs.command.npc.movement.clear")
    public void onClear(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc
    ) {
        npc.getData().getCurrentPath().clear();
        translator.translate("npc_movement_cleared").send(sender);
    }

    @Command("npc movement <npc> list")
    @Permission("fancynpcs.command.npc.movement.list")
    public void onList(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (path.getPositions().isEmpty()) {
            translator.translate("npc_movement_list_empty").send(sender);
            return;
        }

        translator.translate("npc_movement_list_header")
                .replaceStripped("path", npc.getData().getCurrentPathName())
                .replaceStripped("pace", path.getPace().name())
                .replaceStripped("loop", String.valueOf(path.isLoop()))
                .replaceStripped("mode", path.getMovementMode().name())
                .replaceStripped("rotation", path.getRotationMode().name())
                .send(sender);

        for (int i = 0; i < path.getPositions().size(); i++) {
            PathPosition pos = path.getPositions().get(i);
            String translationKey = pos.isWaypoint() ? "npc_movement_list_entry_waypoint" : "npc_movement_list_entry";
            translator.translate(translationKey)
                    .replaceStripped("index", String.valueOf(i + 1))
                    .replaceStripped("x", String.format("%.2f", pos.x()))
                    .replaceStripped("y", String.format("%.2f", pos.y()))
                    .replaceStripped("z", String.format("%.2f", pos.z()))
                    .replaceStripped("yaw", String.format("%.1f", pos.yaw()))
                    .replaceStripped("pitch", String.format("%.1f", pos.pitch()))
                    .send(sender);
        }

        translator.translate("npc_movement_list_footer")
                .replaceStripped("total", String.valueOf(path.getPositions().size()))
                .send(sender);
    }

    /* ========== BASIC SETTINGS ========== */

    @Command("npc movement <npc> pace <pace>")
    @Permission("fancynpcs.command.npc.movement.pace")
    public void onPace(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @NotNull MovementPace pace
    ) {
        npc.getData().getCurrentPath().setPace(pace);
        translator.translate("npc_movement_pace_set")
                .replaceStripped("pace", pace.name())
                .send(sender);
    }

    @Command("npc movement <npc> loop <loop>")
    @Permission("fancynpcs.command.npc.movement.loop")
    public void onLoop(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final boolean loop
    ) {
        npc.getData().getCurrentPath().setLoop(loop);
        translator.translate("npc_movement_loop_set")
                .replaceStripped("loop", String.valueOf(loop))
                .send(sender);
    }

    @Command("npc movement <npc> rotation_mode <mode>")
    @Permission("fancynpcs.command.npc.movement.rotationMode")
    public void onRotationMode(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @NotNull RotationMode mode
    ) {
        npc.getData().getCurrentPath().setRotationMode(mode);
        translator.translate("npc_movement_rotation_mode_set")
                .replaceStripped("mode", mode.name())
                .send(sender);
    }

    @Command("npc movement <npc> mode <mode>")
    @Permission("fancynpcs.command.npc.movement.mode")
    public void onMode(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @NotNull MovementMode mode
    ) {
        npc.getData().getCurrentPath().setMovementMode(mode);
        translator.translate("npc_movement_mode_set")
                .replaceStripped("mode", mode.name())
                .send(sender);
    }

    /* ========== ADVANCED FEATURES ========== */

    @Command("npc movement <npc> set_wait <index> <seconds>")
    @Permission("fancynpcs.command.npc.movement.setWait")
    public void onSetWait(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int index,
            final float seconds
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (index < 1 || index > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        path.setWaitTime(index - 1, seconds);
        translator.translate("npc_movement_wait_set")
                .replaceStripped("index", String.valueOf(index))
                .replaceStripped("seconds", String.format("%.1f", seconds))
                .send(sender);
    }

    @Command("npc movement <npc> set_action <index> <trigger>")
    @Permission("fancynpcs.command.npc.movement.setAction")
    public void onSetAction(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int index,
            final @NotNull ActionTrigger trigger
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (index < 1 || index > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        path.setPositionAction(index - 1, trigger);
        translator.translate("npc_movement_action_set")
                .replaceStripped("index", String.valueOf(index))
                .replaceStripped("trigger", trigger.name())
                .send(sender);
    }

    @Command("npc movement <npc> add_position_action <index> <action> [value]")
    @Permission("fancynpcs.command.npc.movement.addPositionAction")
    public void onAddPositionAction(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int index,
            final @NotNull @Argument("action") String actionName,
            final @Nullable @Greedy String value
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (index < 1 || index > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        NpcAction action = FancyNpcs.getInstance().getActionManager().getActionByName(actionName);
        if (action == null) {
            translator.translate("npc_action_add_failure_not_found")
                    .replaceStripped("action", actionName)
                    .send(sender);
            return;
        }

        List<NpcAction.NpcActionData> existingActions = path.getActionsAtPosition(index - 1);
        int order = existingActions.size();
        NpcAction.NpcActionData actionData = new NpcAction.NpcActionData(order, action, value);

        path.addActionAtPosition(index - 1, actionData);
        translator.translate("npc_movement_position_action_added")
                .replaceStripped("index", String.valueOf(index))
                .replaceStripped("action", action.getName())
                .send(sender);
    }

    @Command("npc movement <npc> remove_position_action <index> <action_index>")
    @Permission("fancynpcs.command.npc.movement.removePositionAction")
    public void onRemovePositionAction(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int index,
            final @Argument("action_index") int actionIndex
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (index < 1 || index > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        path.removeActionAtPosition(index - 1, actionIndex);
        translator.translate("npc_movement_position_action_removed")
                .replaceStripped("index", String.valueOf(index))
                .replaceStripped("action_index", String.valueOf(actionIndex))
                .send(sender);
    }

    @Command("npc movement <npc> clear_position_actions <index>")
    @Permission("fancynpcs.command.npc.movement.clearPositionActions")
    public void onClearPositionActions(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int index
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (index < 1 || index > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        path.clearActionsAtPosition(index - 1);
        translator.translate("npc_movement_position_actions_cleared")
                .replaceStripped("index", String.valueOf(index))
                .send(sender);
    }

    @Command("npc movement <npc> list_position_actions <index>")
    @Permission("fancynpcs.command.npc.movement.listPositionActions")
    public void onListPositionActions(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int index
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (index < 1 || index > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        List<NpcAction.NpcActionData> actions = path.getActionsAtPosition(index - 1);
        if (actions.isEmpty()) {
            translator.translate("npc_movement_position_actions_empty")
                    .replaceStripped("index", String.valueOf(index))
                    .send(sender);
            return;
        }

        translator.translate("npc_movement_position_actions_header")
                .replaceStripped("index", String.valueOf(index))
                .send(sender);

        for (int i = 0; i < actions.size(); i++) {
            NpcAction.NpcActionData actionData = actions.get(i);
            translator.translate("npc_movement_position_actions_entry")
                    .replaceStripped("action_index", String.valueOf(i))
                    .replaceStripped("action", actionData.action().getName())
                    .replaceStripped("value", actionData.value() != null ? actionData.value() : "none")
                    .send(sender);
        }
    }

    @Command("npc movement <npc> set_pace_segment <from> <to> <pace>")
    @Permission("fancynpcs.command.npc.movement.setPaceSegment")
    public void onSetPaceSegment(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final @Argument(suggestions = "MovementCMD/position_range") int from,
            final @Argument(suggestions = "MovementCMD/position_range") int to,
            final @NotNull MovementPace pace
    ) {
        MovementPath path = npc.getData().getCurrentPath();

        if (from < 1 || from > path.getPositions().size() || to < 1 || to > path.getPositions().size()) {
            translator.translate("npc_movement_position_invalid_index")
                    .replaceStripped("index", String.valueOf(from) + " or " + to)
                    .send(sender);
            return;
        }

        path.setSegmentPace(from - 1, to - 1, pace);
        translator.translate("npc_movement_segment_pace_set")
                .replaceStripped("from", String.valueOf(from))
                .replaceStripped("to", String.valueOf(to))
                .replaceStripped("pace", pace.name())
                .send(sender);
    }

    @Command("npc movement <npc> follow_distance <distance>")
    @Permission("fancynpcs.command.npc.movement.followDistance")
    public void onFollowDistance(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final double distance
    ) {
        npc.getData().getCurrentPath().setFollowDistance(distance);
        translator.translate("npc_movement_follow_distance_set")
                .replaceStripped("distance", String.format("%.1f", distance))
                .send(sender);
    }

    @Command("npc movement <npc> physics <enabled>")
    @Permission("fancynpcs.command.npc.movement.physics")
    public void onPhysicsPath(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final boolean enabled
    ) {
        npc.getData().getCurrentPath().setUsePhysics(enabled);
        translator.translate("npc_movement_physics_set")
                .replaceStripped("enabled", String.valueOf(enabled))
                .replaceStripped("scope", "path")
                .send(sender);
    }

    @Command("npc movement <npc> physics_global <enabled>")
    @Permission("fancynpcs.command.npc.movement.physicsGlobal")
    public void onPhysicsGlobal(
            final @NotNull CommandSender sender,
            final @NotNull Npc npc,
            final boolean enabled
    ) {
        npc.getData().setUsePhysics(enabled);
        translator.translate("npc_movement_physics_set")
                .replaceStripped("enabled", String.valueOf(enabled))
                .replaceStripped("scope", "NPC")
                .send(sender);
    }

    /* ========== SUGGESTIONS ========== */

    @Suggestions("MovementCMD/position_range")
    public List<String> suggestPosition(final CommandContext<CommandSender> context, final CommandInput input) {
        final Npc npc = context.getOrDefault("npc", null);
        if (npc == null) return Collections.emptyList();

        MovementPath path = npc.getData().getCurrentPath();
        List<String> suggestions = new ArrayList<>();
        int pathSize = path.getPositions().size();

        for (int i = 1; i <= pathSize; i++) {
            suggestions.add(String.valueOf(i));
        }

        return suggestions;
    }

    @Suggestions("MovementCMD/path_names")
    public List<String> suggestPathNames(final CommandContext<CommandSender> context, final CommandInput input) {
        final Npc npc = context.getOrDefault("npc", null);
        if (npc == null) return Collections.emptyList();

        return new ArrayList<>(npc.getData().getMovementPaths().keySet());
    }
}
