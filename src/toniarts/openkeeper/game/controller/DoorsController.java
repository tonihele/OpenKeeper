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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Regeneration;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.door.DoorController;
import toniarts.openkeeper.game.controller.door.IDoorController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.MapLoader;

/**
 * This is a controller that controls all the doors in the world TODO:
 * Hmm, should this be more a factory/loader maybe, or if this offers the
 * ability to load / save, then it is fine
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DoorsController implements IDoorsController {

    private KwdFile kwdFile;
    private EntityData entityData;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private IMapController mapController;
    private IGameController gameController;
    private ILevelInfo levelInfo;

    private static final Logger LOGGER = Logger.getLogger(DoorsController.class.getName());

    public DoorsController() {
        // For serialization
    }

    /**
     * Load doors from a KWD file straight (new game)
     *
     * @param kwdFile the KWD file
     * @param entityData the entity controller
     * @param gameSettings the game settings
     * @param mapController the map controller
     * @param gameController
     * @param levelInfo
     */
    public DoorsController(KwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            IMapController mapController, IGameController gameController, ILevelInfo levelInfo) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.mapController = mapController;
        this.gameController = gameController;
        this.levelInfo = levelInfo;

        // Load doors
        loadDoors();
    }

    private void loadDoors() {
        for (Thing.Door door : kwdFile.getThings(Thing.Door.class)) {
            try {
                if (levelInfo.getPlayer(door.getPlayerId()) == null) {
                    continue;
                }
                loadDoor(door);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load Thing.", ex);
            }
        }
    }

    private EntityId loadDoor(Thing.Door door) {
        return loadDoor(door.getPosX(), door.getPosY(), door.getDoorId(), door.getPlayerId(), door.getTriggerId() != 0 ? door.getTriggerId() : null, door.getFlag() == Thing.Door.DoorFlag.LOCKED, door.getFlag() == Thing.Door.DoorFlag.BLUEPRINT);
    }

    private EntityId loadDoor(int x, int y, short doorId, short ownerId, Integer triggerId, boolean locked, boolean blueprint) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new DoorComponent(doorId, locked, blueprint));
        entityData.setComponent(entity, new Owner(ownerId));

        // Move to the center of the tile
        Vector3f pos = WorldUtils.pointToVector3f(x, y);
        pos.y = MapLoader.FLOOR_HEIGHT;
        float rotation = 0;
        if (canTileSupportDoor(x, y - 1, ownerId) && canTileSupportDoor(x, y + 1, ownerId)) {
            rotation = -FastMath.HALF_PI;
        }
        entityData.setComponent(entity, new Position(rotation, pos));

        // Add additional components
        Door door = kwdFile.getDoorById(doorId);
        if (door.getTrapTypeId() > 0) {
            entityData.setComponent(entity, new TrapComponent(door.getTrapTypeId()));
        }

        // Health
        entityData.setComponent(entity, new Health(door.getHealth(), door.getHealth()));

        // Regeneration
        if (door.getHealthGain() > 0) {
            entityData.setComponent(entity, new Regeneration(door.getHealthGain(), null));
        }

        // Trigger
        if (triggerId != null) {
            entityData.setComponent(entity, new Trigger(triggerId));
        }

        // Add some interaction properties
        entityData.setComponent(entity, new Interaction(true, false, false, false, false));

        // The visual state
        entityData.setComponent(entity, new DoorViewState(doorId, locked, blueprint, false));

        return entity;
    }

    private boolean canTileSupportDoor(int x, int y, short ownerId) {
        IMapTileInformation tile = mapController.getMapData().getTile(x, y);
        Terrain terrain = mapController.getTerrain(tile);

        return (tile != null && terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && ((!terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE))
                || (tile.getOwnerId() == ownerId && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE))));
    }

    @Override
    public IDoorController createController(EntityId entityId) {
        DoorComponent doorComponent = entityData.getComponent(entityId, DoorComponent.class);
        if (doorComponent == null) {
            throw new RuntimeException("Entity " + entityId + " doesn't represent a door!");
        }
        return new DoorController(entityId, entityData, kwdFile.getDoorById(doorComponent.doorId), gameController.getGameWorldController().getObjectsController(), gameController.getGameWorldController().getMapController()
        );
    }

    @Override
    public boolean isValidEntity(EntityId entityId) {
        return entityData.getComponent(entityId, DoorComponent.class) != null;
    }

}
