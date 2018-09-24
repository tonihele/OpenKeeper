/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.trap.ITrapController;
import toniarts.openkeeper.game.controller.trap.TrapController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.MapLoader;

/**
 * This is a controller that controls all the traps in the world TODO:
 * Hmm, should this be more a factory/loader maybe, or if this offers the
 * ability to load / save, then it is fine
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TrapsController implements ITrapsController {

    private KwdFile kwdFile;
    private EntityData entityData;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;

    private static final Logger LOGGER = Logger.getLogger(TrapsController.class.getName());

    public TrapsController() {
        // For serialization
    }

    /**
     * Load trap from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param entityData the entity controller
     * @param gameSettings the game settings
     */
    public TrapsController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;

        // Load traps
        loadTraps();
    }

    private void loadTraps() {
        for (Thing.Trap trap : kwdFile.getThings(Thing.Trap.class)) {
            try {
                loadTrap(trap);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load Thing.", ex);
            }
        }
    }

    private EntityId loadTrap(Thing.Trap trap) {
        return loadTrap(trap.getPosX(), trap.getPosY(), trap.getTrapId(), trap.getPlayerId(), trap.getNumberOfShots() == 0);
    }

    private EntityId loadTrap(int x, int y, short trapId, short ownerId, boolean blueprint) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new TrapComponent(trapId));
        entityData.setComponent(entity, new Owner(ownerId));

        // Move to the center of the tile
        Vector3f pos = WorldUtils.pointToVector3f(x, y);
        pos.y = MapLoader.FLOOR_HEIGHT;
        entityData.setComponent(entity, new Position(0, pos));

        Trap trap = kwdFile.getTrapById(trapId);

        // Health and threat
        if (!blueprint) {
            entityData.setComponent(entity, new Health(trap.getHealthGain(), trap.getHealth(), trap.getHealth(), false));
            entityData.setComponent(entity, new Threat(trap.getThreat()));
        }

        // Add some interaction properties
        entityData.setComponent(entity, new Interaction(true, false, false, false));

        // The visual state
        entityData.setComponent(entity, new TrapViewState(trapId, blueprint));

        return entity;
    }

    @Override
    public ITrapController createController(EntityId entityId) {
        TrapComponent trapComponent = entityData.getComponent(entityId, TrapComponent.class);
        if (trapComponent == null) {
            throw new RuntimeException("Entity " + entityId + " doesn't represent a trap!");
        }
        return new TrapController(entityId, entityData, kwdFile.getTrapById(trapComponent.trapId));
    }

    @Override
    public boolean isValidEntity(EntityId entityId) {
        return entityData.getComponent(entityId, TrapComponent.class) != null;
    }

}
