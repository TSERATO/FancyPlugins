package de.oliver.fancynpcs;

import de.oliver.fancyanalytics.logger.ExtendedFancyLogger;
import de.oliver.fancyanalytics.logger.properties.ThrowableProperty;
import de.oliver.fancylib.serverSoftware.ServerSoftware;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcAttribute;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.NpcManager;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.events.NpcsLoadedEvent;
import de.oliver.fancynpcs.api.skins.SkinData;
import de.oliver.fancynpcs.api.skins.SkinLoadException;
import de.oliver.fancynpcs.api.utils.MovementMode;
import de.oliver.fancynpcs.api.utils.MovementPace;
import de.oliver.fancynpcs.api.utils.MovementPath;
import de.oliver.fancynpcs.api.utils.NpcEquipmentSlot;
import de.oliver.fancynpcs.api.utils.PathPosition;
import de.oliver.fancynpcs.api.utils.RotationMode;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class NpcManagerImpl implements NpcManager {

    private final JavaPlugin plugin;
    private final ExtendedFancyLogger logger;
    private final Function<NpcData, Npc> npcAdapter;
    private final File npcConfigFile;
    private final Map<String, Npc> npcs; // npc id -> npc
    private boolean isLoaded;

    public NpcManagerImpl(JavaPlugin plugin, Function<NpcData, Npc> npcAdapter) {
        this.plugin = plugin;
        this.logger = FancyNpcs.getInstance().getFancyLogger();
        this.npcAdapter = npcAdapter;
        npcs = new ConcurrentHashMap<>();
        npcConfigFile = new File("plugins" + File.separator + "FancyNpcs" + File.separator + "npcs.yml");
        isLoaded = false;
    }

    public void registerNpc(Npc npc) {
        if (!FancyNpcs.PLAYER_NPCS_FEATURE_FLAG.isEnabled() && getAllNpcs().stream().anyMatch(npc1 -> npc1.getData().getName().equals(npc.getData().getName()))) {
            throw new IllegalStateException("An NPC with this name already exists");
        } else {
            npcs.put(npc.getData().getId(), npc);
        }
    }

    public void removeNpc(Npc npc) {
        npcs.remove(npc.getData().getId());

        YamlConfiguration npcConfig = YamlConfiguration.loadConfiguration(npcConfigFile);
        npcConfig.set("npcs." + npc.getData().getId(), null);
        try {
            npcConfig.save(npcConfigFile);
        } catch (IOException e) {
            logger.error("Could not save npc config file", ThrowableProperty.of(e));
        }
    }

    @ApiStatus.Internal
    @Override
    public Npc getNpc(int entityId) {
        for (Npc npc : getAllNpcs()) {
            if (npc.getEntityId() == entityId) {
                return npc;
            }
        }

        return null;
    }

    @Override
    public Npc getNpc(String name) {
        for (Npc npc : getAllNpcs()) {
            if (npc.getData().getName().equalsIgnoreCase(name)) {
                return npc;
            }
        }

        return null;
    }

    @Override
    public Npc getNpcById(String id) {
        for (Npc npc : getAllNpcs()) {
            if (npc.getData().getId().equals(id)) {
                return npc;
            }
        }

        return null;
    }

    @Override
    public Npc getNpc(String name, UUID creator) {
        for (Npc npc : getAllNpcs()) {
            if (npc.getData().getCreator().equals(creator) && npc.getData().getName().equalsIgnoreCase(name)) {
                return npc;
            }
        }

        return null;
    }

    public Collection<Npc> getAllNpcs() {
        return new ArrayList<>(npcs.values());
    }

    public void saveNpcs(boolean force) {
        if (!isLoaded) {
            return;
        }

        if (!npcConfigFile.exists()) {
            try {
                npcConfigFile.createNewFile();
            } catch (IOException e) {
                logger.error("Could not create npc config file", ThrowableProperty.of(e));
                return;
            }
        }

        YamlConfiguration npcConfig = YamlConfiguration.loadConfiguration(npcConfigFile);

        for (Npc npc : getAllNpcs()) {
            if (!npc.isSaveToFile()) {
                continue;
            }

            boolean shouldSave = force || npc.isDirty();
            if (!shouldSave) {
                continue;
            }

            NpcData data = npc.getData();

            npcConfig.set("npcs." + data.getId() + ".message", null); //TODO: remove in when new interaction system is added
            npcConfig.set("npcs." + data.getId() + ".playerCommand", null); //TODO: remove in when new interaction system is added
            npcConfig.set("npcs." + data.getId() + ".serverCommand", null); //TODO: remove in when new interaction system is added
            npcConfig.set("npcs." + data.getId() + ".mirrorSkin", null); //TODO: remove in next version
            npcConfig.set("npcs." + data.getId() + ".skin.value", null); //TODO: remove in next version
            npcConfig.set("npcs." + data.getId() + ".skin.signature", null); //TODO: remove in next version

            npcConfig.set("npcs." + data.getId() + ".name", data.getName());
            npcConfig.set("npcs." + data.getId() + ".creator", data.getCreator().toString());
            npcConfig.set("npcs." + data.getId() + ".displayName", data.getDisplayName());
            npcConfig.set("npcs." + data.getId() + ".type", data.getType().name());
            npcConfig.set("npcs." + data.getId() + ".location.world", data.getLocation().getWorld().getName());
            npcConfig.set("npcs." + data.getId() + ".location.x", data.getLocation().getX());
            npcConfig.set("npcs." + data.getId() + ".location.y", data.getLocation().getY());
            npcConfig.set("npcs." + data.getId() + ".location.z", data.getLocation().getZ());
            npcConfig.set("npcs." + data.getId() + ".location.yaw", data.getLocation().getYaw());
            npcConfig.set("npcs." + data.getId() + ".location.pitch", data.getLocation().getPitch());
            npcConfig.set("npcs." + data.getId() + ".showInTab", data.isShowInTab());
            npcConfig.set("npcs." + data.getId() + ".spawnEntity", data.isSpawnEntity());
            npcConfig.set("npcs." + data.getId() + ".collidable", data.isCollidable());
            npcConfig.set("npcs." + data.getId() + ".usePhysics", data.usePhysics());
            npcConfig.set("npcs." + data.getId() + ".glowing", data.isGlowing());
            npcConfig.set("npcs." + data.getId() + ".glowingColor", data.getGlowingColor().toString());
            npcConfig.set("npcs." + data.getId() + ".turnToPlayer", data.isTurnToPlayer());
            npcConfig.set("npcs." + data.getId() + ".turnToPlayerDistance", data.getTurnToPlayerDistance());
            npcConfig.set("npcs." + data.getId() + ".messages", null);
            npcConfig.set("npcs." + data.getId() + ".playerCommands", null);
            npcConfig.set("npcs." + data.getId() + ".serverCommands", null);
            npcConfig.set("npcs." + data.getId() + ".sendMessagesRandomly", null);
            npcConfig.set("npcs." + data.getId() + ".interactionCooldown", data.getInteractionCooldown());
            npcConfig.set("npcs." + data.getId() + ".scale", data.getScale());
            npcConfig.set("npcs." + data.getId() + ".visibility_distance", data.getVisibilityDistance());

            if (data.getSkinData() != null) {
                npcConfig.set("npcs." + data.getId() + ".skin.identifier", data.getSkinData().getIdentifier());
                npcConfig.set("npcs." + data.getId() + ".skin.variant", data.getSkinData().getVariant().name());
            } else {
                npcConfig.set("npcs." + data.getId() + ".skin.identifier", null);
            }
            npcConfig.set("npcs." + data.getId() + ".skin.mirrorSkin", data.isMirrorSkin());

            if (data.getEquipment() != null) {
                for (Map.Entry<NpcEquipmentSlot, ItemStack> entry : data.getEquipment().entrySet()) {
                    npcConfig.set("npcs." + data.getId() + ".equipment." + entry.getKey().name(), entry.getValue());
                }
            }

            for (NpcAttribute attribute : FancyNpcs.getInstance().getAttributeManager().getAllAttributesForEntityType(data.getType())) {
                String value = data.getAttributes().getOrDefault(attribute, null);
                npcConfig.set("npcs." + data.getId() + ".attributes." + attribute.getName(), value);
            }

            npcConfig.set("npcs." + data.getId() + ".actions", null);
            for (Map.Entry<ActionTrigger, List<NpcAction.NpcActionData>> entry : npc.getData().getActions().entrySet()) {
                for (NpcAction.NpcActionData actionData : entry.getValue()) {
                    if (actionData == null) {
                        continue;
                    }

                    npcConfig.set("npcs." + data.getId() + ".actions." + entry.getKey().name() + "." + actionData.order() + ".action", actionData.action().getName());
                    npcConfig.set("npcs." + data.getId() + ".actions." + entry.getKey().name() + "." + actionData.order() + ".value", actionData.value());
                }
            }

            // Save movement paths
            npcConfig.set("npcs." + data.getId() + ".movement.currentPath", data.getCurrentPathName());
            npcConfig.set("npcs." + data.getId() + ".movement.paths", null);

            for (Map.Entry<String, MovementPath> pathEntry : data.getMovementPaths().entrySet()) {
                String pathName = pathEntry.getKey();
                MovementPath path = pathEntry.getValue();
                String pathPrefix = "npcs." + data.getId() + ".movement.paths." + pathName;

                npcConfig.set(pathPrefix + ".pace", path.getPace().name());
                npcConfig.set(pathPrefix + ".loop", path.isLoop());
                npcConfig.set(pathPrefix + ".rotationMode", path.getRotationMode().name());
                npcConfig.set(pathPrefix + ".movementMode", path.getMovementMode().name());
                npcConfig.set(pathPrefix + ".followDistance", path.getFollowDistance());
                npcConfig.set(pathPrefix + ".usePhysics", path.usePhysics());

                // Save positions
                npcConfig.set(pathPrefix + ".positions", null);
                for (int i = 0; i < path.getPositions().size(); i++) {
                    PathPosition pos = path.getPositions().get(i);
                    npcConfig.set(pathPrefix + ".positions." + i + ".x", pos.x());
                    npcConfig.set(pathPrefix + ".positions." + i + ".y", pos.y());
                    npcConfig.set(pathPrefix + ".positions." + i + ".z", pos.z());
                    npcConfig.set(pathPrefix + ".positions." + i + ".yaw", pos.yaw());
                    npcConfig.set(pathPrefix + ".positions." + i + ".pitch", pos.pitch());
                    npcConfig.set(pathPrefix + ".positions." + i + ".waypoint", pos.isWaypoint());
                }

                // Save wait times
                for (Map.Entry<Integer, Float> waitEntry : path.getWaitTimes().entrySet()) {
                    npcConfig.set(pathPrefix + ".waitTimes." + waitEntry.getKey(), waitEntry.getValue());
                }

                // Save position actions
                for (Map.Entry<Integer, ActionTrigger> actionEntry : path.getPositionActions().entrySet()) {
                    npcConfig.set(pathPrefix + ".positionActions." + actionEntry.getKey(), actionEntry.getValue().name());
                }

                // Save position action lists (new system)
                for (Map.Entry<Integer, List<NpcAction.NpcActionData>> entry : path.getPositionActionList().entrySet()) {
                    int posIndex = entry.getKey();
                    List<NpcAction.NpcActionData> actions = entry.getValue();
                    for (int i = 0; i < actions.size(); i++) {
                        NpcAction.NpcActionData actionData = actions.get(i);
                        npcConfig.set(pathPrefix + ".positionActionList." + posIndex + "." + i + ".action", actionData.action().getName());
                        npcConfig.set(pathPrefix + ".positionActionList." + posIndex + "." + i + ".value", actionData.value());
                    }
                }

                // Save segment paces
                for (Map.Entry<String, MovementPace> segmentEntry : path.getSegmentPaces().entrySet()) {
                    npcConfig.set(pathPrefix + ".segmentPaces." + segmentEntry.getKey(), segmentEntry.getValue().name());
                }
            }

            npc.setDirty(false);
        }

        try {
            npcConfig.save(npcConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadNpcs() {
        npcs.clear();
        YamlConfiguration npcConfig = YamlConfiguration.loadConfiguration(npcConfigFile);

        if (!npcConfig.isConfigurationSection("npcs")) {
            this.setLoaded();
            return;
        }

        for (String id : npcConfig.getConfigurationSection("npcs").getKeys(false)) {
            String name = npcConfig.getString("npcs." + id + ".name");
            if (name == null) name = id;

            String creatorStr = npcConfig.getString("npcs." + id + ".creator");
            UUID creator = creatorStr == null ? null : UUID.fromString(creatorStr);

            String displayName = npcConfig.getString("npcs." + id + ".displayName", "<empty>");
            EntityType type = EntityType.valueOf(npcConfig.getString("npcs." + id + ".type", "PLAYER").toUpperCase());

            Location location = null;

            try {
                location = npcConfig.getLocation("npcs." + id + ".location");
            } catch (Exception ignored) {
                logger.warn("Could not load location for npc '" + id + "'");
            }

            if (location == null) {
                String worldName = npcConfig.getString("npcs." + id + ".location.world");
                World world = Bukkit.getWorld(worldName);

                if (world == null) {
                    world = (!ServerSoftware.isFolia()) ? new WorldCreator(worldName).createWorld() : null;
                }

                if (world == null) {
                    logger.info("Could not load npc '" + id + "', because the world '" + worldName + "' is not loaded");
                    continue;
                }

                double x = npcConfig.getDouble("npcs." + id + ".location.x");
                double y = npcConfig.getDouble("npcs." + id + ".location.y");
                double z = npcConfig.getDouble("npcs." + id + ".location.z");
                float yaw = (float) npcConfig.getDouble("npcs." + id + ".location.yaw");
                float pitch = (float) npcConfig.getDouble("npcs." + id + ".location.pitch");

                location = new Location(world, x, y, z, yaw, pitch);
            }

            SkinData skin = null;
            String skinIdentifier = npcConfig.getString("npcs." + id + ".skin.identifier", npcConfig.getString("npcs." + id + ".skin.uuid", ""));
            String skinVariantStr = npcConfig.getString("npcs." + id + ".skin.variant", SkinData.SkinVariant.AUTO.name());
            SkinData.SkinVariant skinVariant = SkinData.SkinVariant.valueOf(skinVariantStr);
            if (!skinIdentifier.isEmpty()) {
                try {
                    skin = FancyNpcs.getInstance().getSkinManagerImpl().getByIdentifier(skinIdentifier, skinVariant);
                    skin.setIdentifier(skinIdentifier);
                } catch (final SkinLoadException e) {
                    logger.error("NPC named '" + name + "' identified by '" + id + "' could not have their skin loaded.");
                    logger.error("  " + e.getReason() + " " + e.getMessage());
                }
            }


            if (npcConfig.isSet("npcs." + id + ".skin.value") && npcConfig.isSet("npcs." + id + ".skin.signature")) {
                // using old skin system --> take backup
                takeBackup(npcConfig);

                String value = npcConfig.getString("npcs." + id + ".skin.value");
                String signature = npcConfig.getString("npcs." + id + ".skin.signature");

                if (value != null && !value.isEmpty() && signature != null && !signature.isEmpty()) {
                    SkinData oldSkin = new SkinData(skinIdentifier, SkinData.SkinVariant.AUTO, value, signature);
                    FancyNpcs.getInstance().getSkinManagerImpl().getFileCache().addSkin(oldSkin);
                    FancyNpcs.getInstance().getSkinManagerImpl().getMemCache().addSkin(oldSkin);
                }
            }

            boolean oldMirrorSkin = npcConfig.getBoolean("npcs." + id + ".mirrorSkin"); //TODO: remove in next version
            boolean mirrorSkin = oldMirrorSkin || npcConfig.getBoolean("npcs." + id + ".skin.mirrorSkin");

            boolean showInTab = npcConfig.getBoolean("npcs." + id + ".showInTab");
            boolean spawnEntity = npcConfig.getBoolean("npcs." + id + ".spawnEntity");
            boolean collidable = npcConfig.getBoolean("npcs." + id + ".collidable", true);
            boolean usePhysics = npcConfig.getBoolean("npcs." + id + ".usePhysics", false);
            boolean glowing = npcConfig.getBoolean("npcs." + id + ".glowing");
            NamedTextColor glowingColor = NamedTextColor.NAMES.value(npcConfig.getString("npcs." + id + ".glowingColor", "white"));
            boolean turnToPlayer = npcConfig.getBoolean("npcs." + id + ".turnToPlayer");
            int turnToPlayerDistance = npcConfig.getInt("npcs." + id + ".turnToPlayerDistance", -1);

            Map<ActionTrigger, List<NpcAction.NpcActionData>> actions = new ConcurrentHashMap<>();

            //TODO: remove these fields next version
            boolean sendMessagesRandomly = npcConfig.getBoolean("npcs." + id + ".sendMessagesRandomly", false);
            List<String> playerCommands = npcConfig.getStringList("npcs." + id + ".playerCommands");
            List<String> messages = npcConfig.getStringList("npcs." + id + ".messages");
            List<String> serverCommands = npcConfig.getStringList("npcs." + id + ".serverCommands");

            List<NpcAction.NpcActionData> migrateActionList = new ArrayList<>();
            int actionOrder = 0;

            for (String playerCommand : playerCommands) {
                migrateActionList.add(new NpcAction.NpcActionData(++actionOrder, FancyNpcs.getInstance().getActionManager().getActionByName("player_command"), playerCommand));
            }

            for (String serverCommand : serverCommands) {
                migrateActionList.add(new NpcAction.NpcActionData(++actionOrder, FancyNpcs.getInstance().getActionManager().getActionByName("console_command"), serverCommand));
            }

            if (sendMessagesRandomly && !messages.isEmpty()) {
                migrateActionList.add(new NpcAction.NpcActionData(++actionOrder, FancyNpcs.getInstance().getActionManager().getActionByName("execute_random_action"), ""));
            }

            for (String message : messages) {
                migrateActionList.add(new NpcAction.NpcActionData(++actionOrder, FancyNpcs.getInstance().getActionManager().getActionByName("message"), message));
            }

            if (!migrateActionList.isEmpty()) {
                takeBackup(npcConfig);
                actions.put(ActionTrigger.ANY_CLICK, migrateActionList);
            }

            ConfigurationSection actiontriggerSection = npcConfig.getConfigurationSection("npcs." + id + ".actions");
            if (actiontriggerSection != null) {
                actiontriggerSection.getKeys(false).forEach(trigger -> {
                    ActionTrigger actionTrigger = ActionTrigger.getByName(trigger);
                    if (actionTrigger == null) {
                        logger.warn("Could not find action trigger: " + trigger);
                        return;
                    }

                    List<NpcAction.NpcActionData> actionList = new ArrayList<>();
                    ConfigurationSection actionsSection = npcConfig.getConfigurationSection("npcs." + id + ".actions." + trigger);
                    if (actionsSection != null) {
                        actionsSection.getKeys(false).forEach(order -> {
                            String actionName = npcConfig.getString("npcs." + id + ".actions." + trigger + "." + order + ".action");
                            String value = npcConfig.getString("npcs." + id + ".actions." + trigger + "." + order + ".value");
                            NpcAction action = FancyNpcs.getInstance().getActionManager().getActionByName(actionName);
                            if (action == null) {
                                logger.warn("Could not find action: " + actionName);
                                return;
                            }

                            try {
                                actionList.add(new NpcAction.NpcActionData(Integer.parseInt(order), action, value));
                            } catch (NumberFormatException e) {
                                logger.warn("Could not parse order: " + order);
                            }
                        });

                        actions.put(actionTrigger, actionList);
                    }
                });
            }

            //TODO: add migration for sendMessagesRandomly

            float interactionCooldown = (float) npcConfig.getDouble("npcs." + id + ".interactionCooldown", 0);
            float scale = (float) npcConfig.getDouble("npcs." + id + ".scale", 1);
            int visibilityDistance = npcConfig.getInt("npcs." + id + ".visibility_distance", -1);

            Map<NpcAttribute, String> attributes = new HashMap<>();
            if (npcConfig.isConfigurationSection("npcs." + id + ".attributes")) {
                for (String attrName : npcConfig.getConfigurationSection("npcs." + id + ".attributes").getKeys(false)) {
                    NpcAttribute attribute = FancyNpcs.getInstance().getAttributeManager().getAttributeByName(type, attrName);
                    if (attribute == null) {
                        logger.warn("Could not find attribute: " + attrName);
                        continue;
                    }

                    String value = npcConfig.getString("npcs." + id + ".attributes." + attrName);
                    if (!attribute.isValidValue(value)) {
                        logger.warn("Invalid value for attribute: " + attrName);
                        continue;
                    }

                    attributes.put(attribute, value);
                }
            }

            NpcData data = new NpcData(
                    id,
                    name,
                    creator,
                    displayName,
                    skin,
                    location,
                    showInTab,
                    spawnEntity,
                    collidable,
                    glowing,
                    glowingColor,
                    type,
                    new HashMap<>(),
                    turnToPlayer,
                    turnToPlayerDistance,
                    null,
                    actions,
                    interactionCooldown,
                    scale,
                    visibilityDistance,
                    attributes,
                    mirrorSkin
            );

            Npc npc = npcAdapter.apply(data);

            // Set global physics setting
            data.setUsePhysics(usePhysics);

            // Load movement paths
            String currentPathName = npcConfig.getString("npcs." + id + ".movement.currentPath", "default");
            ConfigurationSection pathsSection = npcConfig.getConfigurationSection("npcs." + id + ".movement.paths");

            if (pathsSection != null) {
                for (String pathName : pathsSection.getKeys(false)) {
                    String pathPrefix = "npcs." + id + ".movement.paths." + pathName;
                    MovementPath path = new MovementPath(pathName);

                    // Load basic settings
                    try {
                        String paceStr = npcConfig.getString(pathPrefix + ".pace", "WALK");
                        path.setPace(MovementPace.valueOf(paceStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid movement pace for npc '" + id + "' path '" + pathName + "'");
                    }

                    path.setLoop(npcConfig.getBoolean(pathPrefix + ".loop", false));

                    try {
                        String rotationModeStr = npcConfig.getString(pathPrefix + ".rotationMode", "SMOOTH");
                        path.setRotationMode(RotationMode.valueOf(rotationModeStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid rotation mode for npc '" + id + "' path '" + pathName + "'");
                    }

                    try {
                        String movementModeStr = npcConfig.getString(pathPrefix + ".movementMode", "CONTINUOUS");
                        path.setMovementMode(MovementMode.valueOf(movementModeStr));
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid movement mode for npc '" + id + "' path '" + pathName + "'");
                    }

                    path.setFollowDistance(npcConfig.getDouble(pathPrefix + ".followDistance", -1));
                    path.setUsePhysics(npcConfig.getBoolean(pathPrefix + ".usePhysics", false));

                    // Load positions
                    ConfigurationSection positionsSection = npcConfig.getConfigurationSection(pathPrefix + ".positions");
                    if (positionsSection != null) {
                        for (String posIndex : positionsSection.getKeys(false)) {
                            double x = npcConfig.getDouble(pathPrefix + ".positions." + posIndex + ".x");
                            double y = npcConfig.getDouble(pathPrefix + ".positions." + posIndex + ".y");
                            double z = npcConfig.getDouble(pathPrefix + ".positions." + posIndex + ".z");
                            float yaw = (float) npcConfig.getDouble(pathPrefix + ".positions." + posIndex + ".yaw");
                            float pitch = (float) npcConfig.getDouble(pathPrefix + ".positions." + posIndex + ".pitch");
                            boolean isWaypoint = npcConfig.getBoolean(pathPrefix + ".positions." + posIndex + ".waypoint", false);
                            path.addPosition(new PathPosition(x, y, z, yaw, pitch, isWaypoint));
                        }
                    }

                    // Load wait times
                    ConfigurationSection waitTimesSection = npcConfig.getConfigurationSection(pathPrefix + ".waitTimes");
                    if (waitTimesSection != null) {
                        for (String waitIndex : waitTimesSection.getKeys(false)) {
                            try {
                                int index = Integer.parseInt(waitIndex);
                                float waitTime = (float) npcConfig.getDouble(pathPrefix + ".waitTimes." + waitIndex);
                                path.setWaitTime(index, waitTime);
                            } catch (NumberFormatException e) {
                                logger.warn("Invalid wait time index for npc '" + id + "'");
                            }
                        }
                    }

                    // Load position actions
                    ConfigurationSection posActionSection = npcConfig.getConfigurationSection(pathPrefix + ".positionActions");
                    if (posActionSection != null) {
                        for (String actionIndex : posActionSection.getKeys(false)) {
                            try {
                                int index = Integer.parseInt(actionIndex);
                                String triggerName = npcConfig.getString(pathPrefix + ".positionActions." + actionIndex);
                                ActionTrigger trigger = ActionTrigger.getByName(triggerName);
                                if (trigger != null) {
                                    path.setPositionAction(index, trigger);
                                }
                            } catch (NumberFormatException e) {
                                logger.warn("Invalid position action index for npc '" + id + "'");
                            }
                        }
                    }

                    // Load position action lists (new system)
                    ConfigurationSection posActionListSection = npcConfig.getConfigurationSection(pathPrefix + ".positionActionList");
                    if (posActionListSection != null) {
                        for (String posIndex : posActionListSection.getKeys(false)) {
                            try {
                                int index = Integer.parseInt(posIndex);
                                ConfigurationSection actionsSection = npcConfig.getConfigurationSection(pathPrefix + ".positionActionList." + posIndex);
                                if (actionsSection != null) {
                                    for (String actionIndex : actionsSection.getKeys(false)) {
                                        String actionName = npcConfig.getString(pathPrefix + ".positionActionList." + posIndex + "." + actionIndex + ".action");
                                        String value = npcConfig.getString(pathPrefix + ".positionActionList." + posIndex + "." + actionIndex + ".value");
                                        NpcAction action = FancyNpcs.getInstance().getActionManager().getActionByName(actionName);
                                        if (action != null) {
                                            int order = Integer.parseInt(actionIndex);
                                            path.addActionAtPosition(index, new NpcAction.NpcActionData(order, action, value));
                                        }
                                    }
                                }
                            } catch (NumberFormatException e) {
                                logger.warn("Invalid position action list index for npc '" + id + "'");
                            }
                        }
                    }

                    // Load segment paces
                    ConfigurationSection segmentPacesSection = npcConfig.getConfigurationSection(pathPrefix + ".segmentPaces");
                    if (segmentPacesSection != null) {
                        for (String segment : segmentPacesSection.getKeys(false)) {
                            try {
                                String paceStr = npcConfig.getString(pathPrefix + ".segmentPaces." + segment);
                                MovementPace segmentPace = MovementPace.valueOf(paceStr);
                                String[] parts = segment.split("-");
                                if (parts.length == 2) {
                                    int from = Integer.parseInt(parts[0]);
                                    int to = Integer.parseInt(parts[1]);
                                    path.setSegmentPace(from, to, segmentPace);
                                }
                            } catch (Exception e) {
                                logger.warn("Invalid segment pace for npc '" + id + "'");
                            }
                        }
                    }

                    data.getMovementPaths().put(pathName, path);
                }
                data.setCurrentPath(currentPathName);
            }

            if (npcConfig.isConfigurationSection("npcs." + id + ".equipment")) {
                for (String equipmentSlotStr : npcConfig.getConfigurationSection("npcs." + id + ".equipment").getKeys(false)) {
                    NpcEquipmentSlot equipmentSlot = NpcEquipmentSlot.parse(equipmentSlotStr);
                    ItemStack item = npcConfig.getItemStack("npcs." + id + ".equipment." + equipmentSlotStr);
                    npc.getData().addEquipment(equipmentSlot, item);
                }
            }

            npc.create();
            registerNpc(npc);
        }
        this.setLoaded();
    }

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    private void setLoaded() {
        isLoaded = true;
        new NpcsLoadedEvent().callEvent();
    }

    public void reloadNpcs() {
        Collection<Npc> npcCopy = new ArrayList<>(getAllNpcs());
        npcs.clear();
        for (Npc npc : npcCopy) {
            npc.removeForAll();
        }

        loadNpcs();
    }

    private void takeBackup(YamlConfiguration npcConfig) {
        String folderPath = "plugins" + File.separator + "FancyNpcs" + File.separator + "/backups";
        File backupDir = new File(folderPath);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String backupFileName = "npcs-" + formatter.format(now) + ".yml";
        File backupFile = new File(folderPath + File.separator + backupFileName);
        if (backupFile.exists()) {
            backupFile.delete();
        }

        try {
            backupFile.createNewFile();
        } catch (IOException e) {
            logger.error("Could not create backup file for NPCs");
        }

        try {
            npcConfig.save(backupFile);
        } catch (IOException e) {
            logger.error("Could not save backup file for NPCs");
        }
    }
}