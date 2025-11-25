package com.fancyinnovations.fancydialogs.listener;

import com.fancyinnovations.fancydialogs.FancyDialogsPlugin;
import com.fancyinnovations.fancydialogs.api.Dialog;
import com.fancyinnovations.fancydialogs.api.DialogAction;
import com.fancyinnovations.fancydialogs.api.data.DialogButton;
import com.fancyinnovations.fancydialogs.api.events.DialogButtonClickedEvent;
import de.oliver.fancysitula.api.packets.FS_ServerboundCustomClickActionPacket;
import de.oliver.fancysitula.api.packets.FS_ServerboundPacket;
import de.oliver.fancysitula.api.utils.FS_PacketListener;
import de.oliver.fancysitula.factories.FancySitula;

import java.util.Map;

public class CustomClickActionPacketListener {

    private static CustomClickActionPacketListener INSTANCE;

    private final FS_PacketListener packetListener;

    public CustomClickActionPacketListener() {
        packetListener = FancySitula.PACKET_LISTENER_FACTORY.createPacketListener(FS_ServerboundPacket.Type.CUSTOM_CLICK_ACTION);
        packetListener.addListener(this::onPacketReceived);
    }

    public static CustomClickActionPacketListener get() {
        if (INSTANCE == null) {
            INSTANCE = new CustomClickActionPacketListener();
        }
        return INSTANCE;
    }

    private void onPacketReceived(FS_PacketListener.PacketReceivedEvent event) {
        if (!(event.packet() instanceof FS_ServerboundCustomClickActionPacket packet)) {
            return; // Ignore if the packet is not of the expected type
        }

        FancyDialogsPlugin.get().getFancyLogger().info("Received custom click packet with ID: " + packet.getId());

        if (!packet.getId().namespace().equals("fancysitula") && !packet.getId().namespace().equals("fancydialogs_dialog_action")) {
            FancyDialogsPlugin.get().getFancyLogger().info("Ignoring packet - namespace is: " + packet.getId().namespace());
            return; // Ignore packets not related to FancyDialogs
        }

        packet.getPayload().forEach((key, value) -> {
            FancyDialogsPlugin.get().getFancyLogger().info("Click action data Key: " + key + " value: " + value.toString());
        });

        String dialogId = packet.getPayload().get("dialog_id");
        String buttonId = packet.getPayload().get("button_id");

        FancyDialogsPlugin.get().getFancyLogger().info("Dialog ID: " + dialogId + ", Button ID: " + buttonId);

        if (dialogId == null || buttonId == null) {
            return; // Missing necessary information
        }

        // Intercept quick_actions_trigger and open the FancyDialogs quick_actions dialog instead
        if ("quick_actions_trigger".equals(dialogId) && "quick_actions_button".equals(buttonId)) {
            FancyDialogsPlugin.get().getFancyLogger().debug("Intercepted quick_actions_trigger, opening FancyDialogs quick_actions dialog");
            String quickActionsDialogId = FancyDialogsPlugin.get().getFancyDialogsConfig().getQuickActionsDialogID();
            Dialog quickActionsDialog = FancyDialogsPlugin.get().getDialogRegistry().get(quickActionsDialogId);
            if (quickActionsDialog != null) {
                quickActionsDialog.open(event.player());
            } else {
                FancyDialogsPlugin.get().getFancyLogger().warn("Quick Actions dialog with ID '" + quickActionsDialogId + "' not found.");
            }
            return; // Don't process further
        }

        new DialogButtonClickedEvent(event.player(), dialogId, buttonId, packet.getPayload()).callEvent();

        if (dialogId.startsWith("confirmation_dialog_")) {
            return; // Ignore confirmation dialog actions, handled separately
        }

        Dialog dialog = FancyDialogsPlugin.get().getDialogRegistry().get(dialogId);
        if (dialog == null) {
            FancyDialogsPlugin.get().getFancyLogger().warn("Received action for unknown dialog: " + dialogId);
            return;
        }

        if (!dialog.isOpenedFor(event.player())) {
            FancyDialogsPlugin.get().getFancyLogger().warn("Received action for dialog: " + dialogId + " but it is not opened for player: " + event.player().getName());
            return;
        }

        DialogButton btn = dialog.getData().getButtonById(buttonId);
        if (btn == null) {
            FancyDialogsPlugin.get().getFancyLogger().warn("Received action for unknown button: " + buttonId + " in dialog: " + dialogId);
            return;
        }

        for (DialogButton.DialogAction btnAction : btn.actions()) {
            DialogAction action = FancyDialogsPlugin.get().getActionRegistry().getAction(btnAction.name());
            if (action == null) {
                FancyDialogsPlugin.get().getFancyLogger().warn("Received action for unknown action: " + btnAction.name() + " in button: " + buttonId);
                continue;
            }

            String data = btnAction.data();
            for (Map.Entry<String, String> entry : packet.getPayload().entrySet()) {
                data = data.replace("{" + entry.getKey() + "}", entry.getValue());
            }

            action.execute(event.player(), dialog, data);
        }
    }

    public FS_PacketListener getPacketListener() {
        return packetListener;
    }
}
