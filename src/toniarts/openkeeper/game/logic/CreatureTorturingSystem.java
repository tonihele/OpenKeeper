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
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Handles creatures torturing. Essentially just decreases the health as we go.
 * When enough... persuation has been received... join the player army
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureTorturingSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final EntitySet torturedEntities;
    private final ILevelInfo levelInfo;

    public CreatureTorturingSystem(EntityData entityData, ILevelInfo levelInfo, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.entityData = entityData;
        this.levelInfo = levelInfo;

        // Have the position also here, since the player may move tortured entities between torture rooms, kinda still tortured but not counting towards death at the time
        torturedEntities = entityData.getEntities(CreatureTortured.class, Health.class, CreatureComponent.class, Position.class);
    }

    @Override
    public void processTick(float tpf, double gameTime) {

        // Add new & remove old
        torturedEntities.applyChanges();

        // Process ticks
        for (Entity entity : torturedEntities) {
            Health health = entity.get(Health.class);

            // TODO: Join the persuating player army!!

            // Health
            CreatureTortured tortured = entity.get(CreatureTortured.class);
            if (gameTime - tortured.healthCheckTime >= 1) {
                int healthRegeneratePerSecond = levelInfo.getLevelData().getCreature(entity.get(CreatureComponent.class).creatureId).getAttributes().getTortureHpChange();
                entityData.setComponent(entity.getId(), new CreatureImprisoned(tortured.startTime, tortured.healthCheckTime + 1));
                entityData.setComponent(entity.getId(), new Health(Math.min(health.health + healthRegeneratePerSecond, health.maxHealth), health.maxHealth));
            }

            // TODO: Mood
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
