/*
 * Copyright (C) 2014-2025 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.game.component.GameEvent;
import toniarts.openkeeper.game.sound.GameEventSoundPlayer;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Brings the {@link GameEvent}s the game logic creates to the client: the ones meant for us are published to the
 * {@link com.simsilica.event.EventBus}. What happened is the logic's business, what to make of it is up to the listeners of the bus.
 * For now the sounds are played.
 */
public final class GameEventState extends AbstractAppState {

    private static final Logger logger = System.getLogger(GameEventState.class.getName());

    private final EntityData entityData;
    private final short playerId;
    private final GameEventSoundPlayer soundPlayer;

    private EntitySet eventEntities;

    /**
     * @param entityData the entity data of the game
     * @param kwdFile the map data, the events refer to its thing types
     * @param playerId the player we play for, the events meant for the other players are left out
     */
    public GameEventState(EntityData entityData, IKwdFile kwdFile, short playerId) {
        this.entityData = entityData;
        this.playerId = playerId;
        soundPlayer = new GameEventSoundPlayer(kwdFile);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        // The events that are already there are old news, only the ones created from now on are handled
        eventEntities = entityData.getEntities(GameEvent.class);
        soundPlayer.register();
    }

    @Override
    public void update(float tpf) {
        if (!eventEntities.applyChanges()) {
            return;
        }
        for (Entity entity : eventEntities.getAddedEntities()) {
            publish(entity.get(GameEvent.class));
        }
    }

    private void publish(GameEvent event) {
        if (event.playerId != GameEvent.ALL_PLAYERS && event.playerId != playerId) {
            logger.log(Level.DEBUG, "Event {0} is for player {1}, we are {2}",
                    new Object[]{event.type, event.playerId, playerId});
            return;
        }
        logger.log(Level.DEBUG, "Event {0} at {1}", new Object[]{event.type, event.position});
        GameEvent.publish(event);
    }

    @Override
    public void cleanup() {
        soundPlayer.unregister();
        if (eventEntities != null) {
            eventEntities.release();
            eventEntities = null;
        }
        super.cleanup();
    }
}
