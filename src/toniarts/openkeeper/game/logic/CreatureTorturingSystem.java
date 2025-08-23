/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.utils.GameTimeCounter;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles creatures torturing. When enough... persuasion has been received... join the player army
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureTorturingSystem extends GameTimeCounter {

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final IMapInformation mapInformation;
    private final EntitySet torturedEntities;

    public CreatureTorturingSystem(EntityData entityData, ICreaturesController creaturesController, IMapInformation mapInformation) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        this.mapInformation = mapInformation;

        // Have the position also here, since the player may move tortured entities between torture rooms, kinda still tortured but not counting towards death at the time
        torturedEntities = entityData.getEntities(CreatureTortured.class, CreatureComponent.class, Position.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);
        // Add new & remove old
        torturedEntities.applyChanges();

        // Process ticks
        for (Entity entity : torturedEntities) {
            CreatureTortured creatureTortured = entity.get(CreatureTortured.class);
            if (creatureTortured.tortureCheckTime >= timeElapsed) {
                continue;
            }

            CreatureComponent creatureComponent = entity.get(CreatureComponent.class);
            if (creatureTortured.timeTortured + tpf >= creatureComponent.tortureTimeToConvert) {

                Owner owner = entity.get(Owner.class);
                short playerId = mapInformation.getMapData().getTile(WorldUtils.vectorToPoint(entity.get(Position.class).position)).getOwnerId();
                if (owner.ownerId != playerId) {

                    // Convert!
                    entityData.removeComponent(entity.getId(), CreatureTortured.class);
                    creaturesController.createController(entity.getId()).convertCreature(playerId);
                    continue;
                }
            }

            entity.set(new CreatureTortured(creatureTortured.timeTortured + tpf, timeElapsed, creatureTortured.healthCheckTime));
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        torturedEntities.release();
    }

}
