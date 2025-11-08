package de.oliver.fancynpcs.api.actions.types;

import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;

/**
 * Action that makes the NPC return to the first position of the path
 */
public class ReturnToStartAction extends NpcAction {

    public ReturnToStartAction() {
        super("return_to_start", false);
    }

    @Override
    public void execute(ActionExecutionContext context, String value) {
        if (context.getNpc() == null) {
            return;
        }

        context.getNpc().returnToStart();
    }
}
