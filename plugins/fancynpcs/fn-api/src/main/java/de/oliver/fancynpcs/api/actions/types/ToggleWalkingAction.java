package de.oliver.fancynpcs.api.actions.types;

import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;

/**
 * Action that toggles the NPC's walking state
 */
public class ToggleWalkingAction extends NpcAction {

    public ToggleWalkingAction() {
        super("toggle_walking", false);
    }

    @Override
    public void execute(ActionExecutionContext context, String value) {
        if (context.getNpc() == null) {
            return;
        }

        // Toggle the walking state
        boolean currentState = context.getNpc().getData().isWalking();
        if (currentState) {
            context.getNpc().stopMovement();
        } else {
            context.getNpc().startMovement();
        }
    }
}
