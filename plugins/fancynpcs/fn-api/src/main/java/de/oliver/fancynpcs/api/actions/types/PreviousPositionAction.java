package de.oliver.fancynpcs.api.actions.types;

import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;

/**
 * Action that moves the NPC to the previous position in the path
 */
public class PreviousPositionAction extends NpcAction {

    public PreviousPositionAction() {
        super("previous_position", false);
    }

    @Override
    public void execute(ActionExecutionContext context, String value) {
        if (context.getNpc() == null) {
            return;
        }

        context.getNpc().moveToPreviousPosition();
    }
}
