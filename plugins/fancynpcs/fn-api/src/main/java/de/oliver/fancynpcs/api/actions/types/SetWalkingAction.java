package de.oliver.fancynpcs.api.actions.types;

import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;

/**
 * Action that sets the NPC's walking state
 * Value: "true" or "false"
 */
public class SetWalkingAction extends NpcAction {

    public SetWalkingAction() {
        super("set_walking", true);
    }

    @Override
    public void execute(ActionExecutionContext context, String value) {
        if (context.getNpc() == null || value == null) {
            return;
        }

        // Set the walking state based on value
        boolean enabled = Boolean.parseBoolean(value);
        if (enabled) {
            context.getNpc().startMovement();
        } else {
            context.getNpc().stopMovement();
        }
    }
}
