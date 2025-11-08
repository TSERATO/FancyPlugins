package de.oliver.fancynpcs.api.actions.types;

import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;

/**
 * Action that starts the NPC's movement along a path
 * Value can specify path name, or uses current path if not specified
 */
public class StartMovementAction extends NpcAction {

    public StartMovementAction() {
        super("start_movement", false);
    }

    @Override
    public void execute(ActionExecutionContext context, String value) {
        if (context.getNpc() == null) {
            return;
        }

        // Start the movement path (optionally with specified path name)
        if (value != null && !value.isEmpty()) {
            context.getNpc().startMovement(value);
        } else {
            context.getNpc().startMovement();
        }
    }
}
