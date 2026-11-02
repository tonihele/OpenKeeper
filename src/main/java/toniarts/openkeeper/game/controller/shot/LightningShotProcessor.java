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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Lightning, the keeper's Thunderbolt. Strikes the target at once: deals the
 * shot data 1 as damage, knocks a creature off its feet and leaves the bolt
 * for the view to draw where the target stands.
 * <p>
 * Every cast is a separate strike, nothing is shared between casts, so the
 * player can fire as fast as they can click and pay for it.
 */
public final class LightningShotProcessor implements IShotProcessor {

    private static final Logger logger = System.getLogger(LightningShotProcessor.class.getName());

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final IKwdFile kwdFile;
    private final IMapController mapController;
    private final ICreaturesController creaturesController;

    public LightningShotProcessor(EntityData entityData, IGameTimer gameTimer, IKwdFile kwdFile, IMapController mapController,
            ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.kwdFile = kwdFile;
        this.mapController = mapController;
        this.creaturesController = creaturesController;
    }

    @Override
    public void process(ShotRequest request) {
        EntityId target = request.target();
        Position targetPosition = target != null ? entityData.getComponent(target, Position.class) : null;
        if (targetPosition == null) {

            // The cast validation requires a target, so it is gone already
            logger.log(Level.DEBUG, "{0} has nothing to strike, target {1}", request.shot().getName(), target);
            return;
        }

        // As in DKII.EXE (found by scottyhutch in #597): the shot data is the damage as it is, doubled on water.
        // A few creatures (King Reginald, Stone Knight, Jack) are neither hurt nor knocked down. Possessed and
        // imprisoned creatures and the Horned Reaper are hurt but stay on their feet
        CreatureComponent creatureComponent = entityData.getComponent(target, CreatureComponent.class);
        Creature creature = creatureComponent != null ? kwdFile.getCreature(creatureComponent.creatureId) : null;
        if (creature == null || !creature.getFlags3().contains(Creature.CreatureFlag3.IS_IMMUNE_TO_LIGHTNING)) {
            int damage = request.shotData1();
            if (isOnWater(targetPosition)) {
                damage *= 2;
            }
            EntityController.setDamage(entityData, target, damage);

            if (creature != null && canBeKnockedDown(target, creature)) {
                creaturesController.createController(target).knockDown();
            }
        }

        // Strike where the target stands, not where the cursor happened to be
        ShotEntityFactory.create(entityData, request.shot(), targetPosition.position, gameTimer.getGameTime());
    }

    private boolean isOnWater(Position position) {
        IMapTileController tile = mapController.getMapData().getTile(WorldUtils.vectorToPoint(position.position));
        return tile != null && mapController.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.WATER);
    }

    private boolean canBeKnockedDown(EntityId target, Creature creature) {
        return entityData.getComponent(target, Possessed.class) == null
                && entityData.getComponent(target, CreatureImprisoned.class) == null
                && !creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY);
    }

}
