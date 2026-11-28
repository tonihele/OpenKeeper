/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.controller.shot;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Wind, the cyclone of the Fairy and the Firefly. Every turn of its flight it
 * spins the creatures around it and knocks them off their feet, all but the
 * creature that cast it (DKII.EXE 0x4AE990). The shot data 1 is the radius in
 * tiles. The shot data 2 is the damage to the creatures of other players, in
 * percents of the creature's health, the spell's own is 0.
 */
public final class WindShotProcessor implements IShotProcessor {

    /**
     * The spin of a creature in the middle of the wind in a turn, three
     * quarters of a circle
     */
    private static final float SPIN = FastMath.TWO_PI * 0.75f;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameController gameController;
    private final IMapController mapController;
    private final ICreaturesController creaturesController;

    public WindShotProcessor(EntityData entityData, IKwdFile kwdFile, IGameController gameController, IMapController mapController,
            ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameController = gameController;
        this.mapController = mapController;
        this.creaturesController = creaturesController;
    }

    @Override
    public void process(ShotRequest request) {
        int radius = request.shotData1();
        Point tile = WorldUtils.vectorToPoint(request.position());
        IMapDataInformation<?> mapData = mapController.getMapData();
        IEntityPositionLookup entityPositionLookup = gameController.getEntityLookupService();

        // The tiles are the original's: from the radius before the shot's tile to one short of the radius after it
        for (int x = Math.max(0, tile.x - radius); x < Math.min(mapData.getWidth(), tile.x + radius); x++) {
            for (int y = Math.max(0, tile.y - radius); y < Math.min(mapData.getHeight(), tile.y + radius); y++) {
                for (EntityId entity : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    if (!entity.equals(request.shooter())) {
                        blow(request, entity);
                    }
                }
            }
        }
    }

    private void blow(ShotRequest request, EntityId entity) {
        CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);
        Position position = entityData.getComponent(entity, Position.class);
        if (creatureComponent == null || position == null || entityData.getComponent(entity, Health.class) == null) {
            return;
        }

        Vector3f shotPosition = request.position();
        int radius = request.shotData1();
        float distance = FastMath.sqrt(FastMath.sqr(position.position.x - shotPosition.x) + FastMath.sqr(position.position.z - shotPosition.z));
        if (distance >= radius) {
            return;
        }

        // The closer to the wind, the faster the spin
        entityData.setComponent(entity, new Position(position.rotation - SPIN * radius / (distance + radius), position.position));

        // The damage grows with the distance, from half in the middle to all of it at the edge
        Owner owner = entityData.getComponent(entity, Owner.class);
        if (request.shotData2() > 0 && owner != null && owner.ownerId != request.playerId()) {
            int health = kwdFile.getCreature(creatureComponent.creatureId).getAttributes().getHp();
            EntityController.setDamage(entityData, entity, (int) (health * (distance + radius) / (radius * 2)) * request.shotData2() / 100);
        }

        creaturesController.createController(entity).knockDown();
    }

}
