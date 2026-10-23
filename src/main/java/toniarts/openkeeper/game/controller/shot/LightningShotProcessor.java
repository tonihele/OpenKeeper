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
import toniarts.openkeeper.game.component.KnockedDown;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.entity.EntityController;

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

    public LightningShotProcessor(EntityData entityData, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
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

        EntityController.setDamage(entityData, target, request.damage());

        if (entityData.getComponent(target, CreatureComponent.class) != null) {
            entityData.setComponent(target, new KnockedDown());
        }

        // Strike where the target stands, not where the cursor happened to be
        ShotEntityFactory.create(entityData, request.shot(), targetPosition.position, gameTimer.getGameTime());
    }

}
