package de.oliver.fancynpcs.api.actions.types;

import de.oliver.fancynpcs.api.actions.NpcAction;
import de.oliver.fancynpcs.api.actions.executor.ActionExecutionContext;

/**
 * Action that sends the NPC home to the first position on its path
 */
public class HomeAction extends NpcAction {

    public HomeAction() {
        super("home", false);
    }

    @Override
    public void execute(ActionExecutionContext context, String value) {
        if (context.getNpc() == null) {
            return;
        }

        // Return NPC to the first position
        context.getNpc().returnToStart();
    }
}
