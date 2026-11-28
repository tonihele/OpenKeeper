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
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Lands the creatures that the {@link CreatureFallSystem} has brought down to the floor. The fall is animated
 * on the animation loop, but the landing changes the AI state of the creature, so it is done here on the game
 * logic thread, like everything else that touches the AI.
 */
public final class CreatureLandingSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final GameEventService gameEventService;
    private final ICreaturesController creaturesController;
    private final EntitySet fallEntities;

    public CreatureLandingSystem(EntityData entityData, IGameTimer gameTimer, GameEventService gameEventService,
            ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.gameEventService = gameEventService;
        this.creaturesController = creaturesController;

        fallEntities = entityData.getEntities(CreatureFall.class, Position.class, CreatureComponent.class);
    }

    @Override
    public void processTick(float tpf) {
        fallEntities.applyChanges();

        for (Entity entity : fallEntities) {
            Position position = entity.get(Position.class);
            CreatureFall.FallType fallType = entity.get(CreatureFall.class).fallType;

            // Like in DK II, the floor in the middle of the portal is a level lower, the creature falls through
            float floorHeight = fallType == CreatureFall.FallType.PORTAL_DROP ? WorldUtils.UNDERFLOOR_HEIGHT : WorldUtils.FLOOR_HEIGHT;
            if (position.position.y > floorHeight) {
                continue;
            }

            // We'll just remove this and add the AI
            entityData.removeComponent(entity.getId(), CreatureFall.class);

            // The landing, on touchdown (hand drops and conjured creatures alike)
            gameEventService.emit(GameEventType.CREATURE_LAND, entity.getId(), position.position);

            // A creature dropped on the middle of its keeper's portal doesn't get up, it leaves the dungeon
            if (fallType == CreatureFall.FallType.PORTAL_DROP) {
                creaturesController.leaveDungeon(entity.getId());
                continue;
            }

            // Preserve any requested AI target through landing and get-up.
            // The state, depending on the landing
            CreatureComponent creatureComponent = entity.get(CreatureComponent.class);
            boolean stunned = fallType == CreatureFall.FallType.DROPPED && creatureComponent.stunDuration > 0f;
            CreatureAi ai = entityData.getComponent(entity.getId(), CreatureAi.class);
            entityData.setComponent(entity.getId(), new CreatureAi(gameTimer.getGameTime(), stunned ? CreatureState.FALLEN : CreatureState.IDLE, creatureComponent.creatureId,
                    ai == null ? null : ai.getTargetState()));
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        fallEntities.release();
    }

}
