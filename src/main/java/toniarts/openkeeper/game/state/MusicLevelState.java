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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.sound.MusicLevel;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

/**
 * Requests the music level from what happens in the game, as DK II does every
 * update:
 * <ul>
 * <li>{@link MusicLevel#LEVEL_ONE} while the player is involved in a fight</li>
 * <li>levels two to four when the summed threat of the enemy creatures
 * fighting the player reaches the MUSIC_LEVEL_*_THREAT_THRESHOLD variables</li>
 * <li>{@link MusicLevel#LEVEL_FOUR} while a Horned Reaper is around</li>
 * </ul>
 * DK II groups the fights into battles, here an enemy counts when it fights the
 * player's creatures or is fought by them.
 *
 * @see <a href="https://github.com/Dethware/DK2/blob/master/docs/runtime-behaviour.md#what-requests-which-level">What requests which music level in DK II</a>
 */
public final class MusicLevelState extends AbstractPauseAwareState {

    private final EntityData entityData;
    private final Keeper player;
    private final IKwdFile kwdFile;
    private final int[] thresholds = new int[3];
    private EntitySet creatures;
    private EntitySet attackers;
    private MusicLevel musicLevel;

    public MusicLevelState(EntityData entityData, Keeper player, IKwdFile kwdFile) {
        this.entityData = entityData;
        this.player = player;
        this.kwdFile = kwdFile;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        musicLevel = stateManager.getState(SoundState.class).getMusicLevel();
        thresholds[0] = getVariable(MiscType.MUSIC_LEVEL_TWO_THREAT_THRESHOLD, 100);
        thresholds[1] = getVariable(MiscType.MUSIC_LEVEL_THREE_THREAT_THRESHOLD, 500);
        thresholds[2] = getVariable(MiscType.MUSIC_LEVEL_FOUR_THREAT_THRESHOLD, 1500);
        creatures = entityData.getEntities(CreatureAi.class, Owner.class, Threat.class);
        attackers = entityData.getEntities(AttackTarget.class);
    }

    private int getVariable(MiscType type, int defaultValue) {
        var variable = kwdFile.getVariables().get(type);
        return variable != null ? (int) variable.getValue() : defaultValue;
    }

    @Override
    public void update(float tpf) {
        creatures.applyChanges();
        attackers.applyChanges();

        Map<EntityId, EntityId> targets = HashMap.newHashMap(attackers.size());
        for (Entity entity : attackers) {
            targets.put(entity.getId(), entity.get(AttackTarget.class).entityId);
        }

        boolean fighting = false;
        Set<EntityId> enemies = new HashSet<>();
        for (Entity creature : creatures) {
            CreatureAi ai = creature.get(CreatureAi.class);
            if (isHorny(ai)) {
                musicLevel.raise(MusicLevel.LEVEL_FOUR);
            }
            if (ai.getCreatureState() != CreatureState.FIGHT) {
                continue;
            }

            EntityId targetId = targets.get(creature.getId());
            Entity target = targetId != null ? creatures.getEntity(targetId) : null;
            if (target == null) {
                continue;
            }
            short owner = creature.get(Owner.class).ownerId;
            short targetOwner = target.get(Owner.class).ownerId;
            if (player.isAlly(owner) && player.isEnemy(targetOwner)) {
                fighting = true;
                // Like in DK II, only enemies that fight back add to the threat
                if (isFighting(target)) {
                    enemies.add(target.getId());
                }
            } else if (player.isEnemy(owner) && player.isAlly(targetOwner)) {
                fighting = true;
                enemies.add(creature.getId());
            }
        }

        if (fighting) {
            musicLevel.raise(MusicLevel.LEVEL_ONE + getThreatLevel(enemies));
        }
    }

    private int getThreatLevel(Set<EntityId> enemies) {
        int threat = 0;
        for (EntityId enemy : enemies) {
            threat += creatures.getEntity(enemy).get(Threat.class).threat;
        }
        int level = 0;
        while (level < thresholds.length && threat >= thresholds[level]) {
            level++;
        }
        return level;
    }

    private static boolean isFighting(Entity creature) {
        return creature.get(CreatureAi.class).getCreatureState() == CreatureState.FIGHT;
    }

    private boolean isHorny(CreatureAi ai) {
        Creature creature = kwdFile.getCreature(ai.creatureId);
        return creature != null && creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY);
    }

    @Override
    public void cleanup() {
        creatures.release();
        attackers.release();

        super.cleanup();
    }

    @Override
    public boolean isPauseable() {
        return true;
    }
}
