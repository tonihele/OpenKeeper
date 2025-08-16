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
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureEfficiency;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.GameTimeCounter;

/**
 * Manages slapping of entities, the added effects etc
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class SlapSystem extends GameTimeCounter {

    private final static int EFFICIENCY_BONUS = 10;

    private final KwdFile kwdFile;
    private final EntitySet creatureEntities;
    private final EntitySet objectEntities;
    private final EntityData entityData;
    private final int maxSlapDuration;
    private final Map<Short, PlayerStatsControl> statControls;
    private final Map<EntityId, Double> slapStartTimesByEntityId = new HashMap<>();

    public SlapSystem(EntityData entityData, KwdFile kwdFile, Collection<IPlayerController> playerControllers,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        statControls = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController playerController : playerControllers) {
            statControls.put(playerController.getKeeper().getId(), playerController.getStatsControl());
        }
        maxSlapDuration = (int) gameSettings.get(Variable.MiscVariable.MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS).getValue();

        creatureEntities = entityData.getEntities(Slapped.class, CreatureComponent.class, Owner.class);
        objectEntities = entityData.getEntities(Slapped.class, ObjectComponent.class, Interaction.class);
        processAddedCreatureEntities(creatureEntities);
        processAddedObjectEntities(objectEntities);
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        if (creatureEntities.applyChanges()) {
            processDeletedEntities(creatureEntities.getRemovedEntities());
            processAddedCreatureEntities(creatureEntities.getAddedEntities());
            processChangedEntities(creatureEntities.getChangedEntities());
        }

        if (objectEntities.applyChanges()) {
            processDeletedEntities(objectEntities.getRemovedEntities());
            processAddedObjectEntities(objectEntities.getAddedEntities());
            processChangedEntities(objectEntities.getChangedEntities());
        }

        // Remove the slap effect from creatures
        // TODO: So many variables, for work efficiency and speeding up, so, figure out
        for (Map.Entry<EntityId, Double> entry : slapStartTimesByEntityId.entrySet()) {
            if (timeElapsed - entry.getValue() >= maxSlapDuration) {
                entityData.removeComponent(entry.getKey(), Slapped.class);
            }
        }
    }

    private void processAddedCreatureEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            double startTime = entity.get(Slapped.class).startTime;
            short creatureId = entity.get(CreatureComponent.class).creatureId;
            short ownerId = entity.get(Owner.class).ownerId;

            // Slap
            slapStartTimesByEntityId.put(entity.getId(), startTime);
            handleCreatureSlap(entity, creatureId, ownerId);

            // Efficiency, this doesn't stack, so only when added
            // TODO: there are several parameters, speed and work efficiency, and they look a bit weird to me, so just this now
            CreatureEfficiency efficiency = entityData.getComponent(entity.getId(), CreatureEfficiency.class);
            if (efficiency != null) {
                entityData.setComponent(entity.getId(), new CreatureEfficiency(efficiency.efficiencyPercentage + EFFICIENCY_BONUS));
            }
        }
    }

    private void processAddedObjectEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            double startTime = entity.get(Slapped.class).startTime;
            Interaction interaction = entity.get(Interaction.class);

            // Slap
            slapStartTimesByEntityId.put(entity.getId(), startTime);
            handleObjectSlap(entity, interaction);
        }
    }

    private void handleObjectSlap(Entity entity, Interaction interaction) {
        if (interaction.dieWhenSlapped) {
            EntityController.setDamage(entityData, entity.getId(), Integer.MAX_VALUE);
        }
    }

    private void handleCreatureSlap(Entity entity, short creatureId, short ownerId) {

        // Stats and health
        Creature creature = kwdFile.getCreature(creatureId);

        // Stat
        statControls.get(ownerId).creatureSlapped(creature);

        // Damage
        int damage = creature.getAttributes().getSlapDamage();
        if (damage != 0) {
            EntityController.setDamage(entityData, entity.getId(), damage);
        }

        // Mood
        short moodChange = creature.getAttributes().getAngerSlap();
        if (moodChange != 0) {
            CreatureMood mood = entityData.getComponent(entity.getId(), CreatureMood.class);
            if (mood != null) {
                entityData.setComponent(entity.getId(), new CreatureMood(mood.moodValue - moodChange));
            }
        }

        // TODO: Apply the force
    }

    private void processDeletedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            slapStartTimesByEntityId.remove(entity.getId());
            CreatureEfficiency efficiency = entityData.getComponent(entity.getId(), CreatureEfficiency.class);
            if (efficiency != null) {
                entityData.setComponent(entity.getId(), new CreatureEfficiency(efficiency.efficiencyPercentage - EFFICIENCY_BONUS));
            }
        }
    }

    private void processChangedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            double startTime = entity.get(Slapped.class).startTime;

            if (slapStartTimesByEntityId.get(entity.getId()) != startTime) {
                slapStartTimesByEntityId.put(entity.getId(), startTime);
                if (entity.get(CreatureComponent.class) != null) {
                    short creatureId = entity.get(CreatureComponent.class).creatureId;
                    short ownerId = entity.get(Owner.class).ownerId;

                    // Slap
                    handleCreatureSlap(entity, creatureId, ownerId);
                }
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        creatureEntities.release();
        objectEntities.release();
        slapStartTimesByEntityId.clear();
    }

}
