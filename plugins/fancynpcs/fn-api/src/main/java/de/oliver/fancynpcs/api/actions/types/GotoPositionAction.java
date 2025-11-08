package de.oliver.fancynpcs.api.actions.types;

import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;

/**
 * Action that makes the NPC walk to a specific position and stop there
 * Value: position index (e.g., "0", "1", "2")
 */
public class GotoPositionAction extends NpcAction {

    public GotoPositionAction() {
        super("goto_position", true);
    }

    @Override
    public void execute(ActionExecutionContext context, String value) {
        if (context.getNpc() == null || value == null) {
            return;
        }

        try {
            int position = Integer.parseInt(value);
            context.getNpc().goToPosition(position);
        } catch (NumberFormatException e) {
            // Invalid position number, ignore
        }
    }
}
