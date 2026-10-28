package toniarts.openkeeper.game.event;

import com.simsilica.event.EventType;

/**
 * Holds client side event types
 */
public final class ClientEvents {

    /**
     * Game events
     */
    public static final EventType<GameEvent> GAME_EVENT = EventType.create("GameEvent", GameEvent.class);

    private ClientEvents() {
        // Just collection of events we can use with our event bus
    }

}
