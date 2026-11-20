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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.HealthGain;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.trap.ITrapController;
import toniarts.openkeeper.game.controller.trap.TrapController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * This is a controller that controls all the traps in the world TODO:
 * Hmm, should this be more a factory/loader maybe, or if this offers the
 * ability to load / save, then it is fine
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TrapsController implements ITrapsController {
    
    private static final Logger logger = System.getLogger(TrapsController.class.getName());

    private IKwdFile kwdFile;
    private EntityData entityData;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private IGameController gameController;
    private ILevelInfo levelInfo;

    public TrapsController() {
        // For serialization
    }

    /**
     * Load trap from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param entityData the entity controller
     * @param gameSettings the game settings
     * @param gameController
     * @param levelInfo
     */
    public TrapsController(IKwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            IGameController gameController, ILevelInfo levelInfo) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameController = gameController;
        this.levelInfo = levelInfo;

        // Load traps
        loadTraps();
    }

    private void loadTraps() {
        for (Thing.Trap trap : kwdFile.getThings(Thing.Trap.class)) {
            try {
                if (levelInfo.getPlayer(trap.getPlayerId()) == null) {
                    continue;
                }
                loadTrap(trap);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Could not load Thing.", ex);
            }
        }
    }

    private EntityId loadTrap(Thing.Trap trap) {
        return loadTrap(trap.getPosX(), trap.getPosY(), trap.getTrapId(), trap.getPlayerId(), trap.getNumberOfShots() == 0);
    }

    private EntityId loadTrap(int x, int y, short trapId, short ownerId, boolean blueprint) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new TrapComponent(trapId));
        entityData.setComponent(entity, new Owner(ownerId, ownerId));

        // Move to the center of the tile
        Vector3f pos = WorldUtils.pointToVector3f(x, y);
        pos.y = WorldUtils.FLOOR_HEIGHT;
        entityData.setComponent(entity, new Position(0, pos));

        // Health, traps only threaten after they fire
        if (!blueprint) {
            setArmed(entity, kwdFile.getTrapById(trapId));
        }

        // Add some interaction properties
        entityData.setComponent(entity, new Interaction(true, false, false, false, false));

        // The visual state, invisible traps are hidden from the other players until they are revealed
        entityData.setComponent(entity, new TrapViewState(trapId, blueprint,
                kwdFile.getTrapById(trapId).getFlags().contains(Trap.TrapFlag.INVISIBLE)));

        return entity;
    }

    private void setArmed(EntityId entity, Trap trap) {
        entityData.setComponent(entity, new Health(trap.getHealth(), trap.getHealth()));

        // A trap heals itself every second, wherever it stands
        if (trap.getHealthGain() > 0) {
            entityData.setComponent(entity, new HealthGain(trap.getHealthGain(), null));
        }

        // An armed trap costs its owner mana every second, whether it needs mana to fire or not (DKII.EXE 0x4A96D0,
        // 0x4C1B60). The upkeep goes with the trap entity
        if (trap.getManaUsage() > 0) {
            entityData.setComponent(entity, new Mana(-trap.getManaUsage()));
        }
    }

    @Override
    public void armTrap(EntityId blueprint) {
        TrapViewState viewState = entityData.getComponent(blueprint, TrapViewState.class);
        if (viewState == null || !viewState.blueprint) {
            return;
        }

        setArmed(blueprint, kwdFile.getTrapById(viewState.trapId));
        entityData.setComponent(blueprint, new TrapViewState(viewState.trapId, false, viewState.hidden));
    }

    @Override
    public EntityId addTrapBlueprint(short trapId, Point tile, short ownerId) {
        return loadTrap(tile.x, tile.y, trapId, ownerId, true);
    }

    @Override
    public ITrapController createController(EntityId entityId) {
        TrapComponent trapComponent = entityData.getComponent(entityId, TrapComponent.class);
        if (trapComponent == null) {
            throw new RuntimeException("Entity " + entityId + " doesn't represent a trap!");
        }
        return new TrapController(entityId, entityData, kwdFile.getTrapById(trapComponent.trapId), gameController.getGameWorldController().getObjectsController(), gameController.getGameWorldController().getMapController());
    }

    @Override
    public boolean isValidEntity(EntityId entityId) {
        return entityData.getComponent(entityId, TrapComponent.class) != null;
    }

}
