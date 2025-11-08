package de.oliver.fancynpcs.api.actions.types;

import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;

/**
 * Action that stops the NPC's current movement
 */
public class StopMovementAction extends NpcAction {

    public StopMovementAction() {
        super("stop_movement", false);
    }

    @Override
    public void execute(ActionExecutionContext context, String value) {
        if (context.getNpc() == null) {
            return;
        }

        context.getNpc().stopMovement();
    }
}
