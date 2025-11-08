package de.oliver.fancynpcs.api.actions.types;

import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;

/**
 * Action that moves the NPC to the next position in the path
 * (for GUIDE and MANUAL modes)
 */
public class NextPositionAction extends NpcAction {

    public NextPositionAction() {
        super("next_position", false);
    }

    @Override
    public void execute(ActionExecutionContext context, String value) {
        if (context.getNpc() == null) {
            return;
        }

        context.getNpc().moveToNextPosition();
    }
}
