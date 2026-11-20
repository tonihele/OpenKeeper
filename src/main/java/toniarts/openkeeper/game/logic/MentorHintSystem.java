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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.ReaperAdvised;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * The mentor's advice that no event brings up, given from each keeper's own
 * tick in DK II (DKII.EXE 0x4B9740). A keeper who is out of the game gets
 * none.
 * <br>
 * Once a level, a keeper with a Horned Reaper whose mana, less what its Imps
 * and traps take a second, is under 25000 is told about it.
 * <br>
 * The timed hints go by the level's own time: 50 minutes in, a keeper with
 * fewer than 5 creatures that are not workers hears that its dungeon is
 * empty, and 2 hours in every keeper hears that it should have won by now.
 * Every 40 minutes each keeper gets one of 13 general hints at random, and
 * every 25 minutes one of 8 others, unless one of its creatures is fighting or
 * lies knocked out.
 */
public final class MentorHintSystem implements IGameLogicUpdatable {

    /**
     * Not in the level data, DKII.EXE has the number in the check itself
     */
    private static final int REAPER_ADVICE_MANA = 25000;

    /**
     * The times of the hints in seconds, and which of the hints of DK II's
     * messages 0x59 and 0x58 they are
     */
    private static final int EMPTY_DUNGEON_SECONDS = 3000;
    private static final int EMPTY_DUNGEON_HINT = 1;
    private static final int EMPTY_DUNGEON_CREATURES = 5;
    private static final int SLOW_KEEPER_SECONDS = 7200;
    private static final int SLOW_KEEPER_HINT = 26;
    private static final int GENERAL_HINT_SECONDS = 2400;
    private static final int FIRST_GENERAL_HINT = 12;
    private static final int GENERAL_HINTS = 13;
    private static final int QUIET_HINT_SECONDS = 1500;
    private static final int QUIET_HINTS = 8;

    private final EntityData entityData;
    private final RoomAttackMessages messages;
    private final IGameTimer gameTimer;
    private final int ticksPerSecond;
    private final List<Keeper> keepers = new ArrayList<>();
    private final List<EntitySet> reapers = new ArrayList<>();

    /**
     * The mentor hint system
     *
     * @param entityData the entity data
     * @param kwdFile the level, for the Horned Reaper
     * @param playerControllers the players, the ones with mana get the advice
     * @param messages for the mentor's messages
     * @param gameTimer the game time
     */
    public MentorHintSystem(EntityData entityData, IKwdFile kwdFile, Collection<IPlayerController> playerControllers,
            RoomAttackMessages messages, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.messages = messages;
        this.gameTimer = gameTimer;
        ticksPerSecond = kwdFile.getGameLevel().getTicksPerSec();
        for (IPlayerController playerController : playerControllers) {
            if (playerController.getManaControl() != null) {
                keepers.add(playerController.getKeeper());
            }
        }
        for (Creature creature : kwdFile.getCreatureList()) {
            if (creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
                reapers.add(entityData.getEntities(new FieldFilter<>(CreatureComponent.class, "creatureId", creature.getCreatureId()),
                        CreatureComponent.class, Owner.class, Health.class));
            }
        }
    }

    @Override
    public void processTick(float tpf) {
        for (EntitySet reaperSet : reapers) {
            reaperSet.applyChanges();
        }
        long tick = Math.round(gameTimer.getGameTime() * ticksPerSecond);
        for (Keeper keeper : keepers) {
            if (!keeper.isDestroyed()) {
                adviseOnReaper(keeper);
                giveTimedHints(keeper.getId(), tick);
            }
        }
    }

    private void giveTimedHints(short playerId, long tick) {
        if (tick == (long) SLOW_KEEPER_SECONDS * ticksPerSecond) {
            messages.hint(playerId, SLOW_KEEPER_HINT);
        }
        if (tick == (long) EMPTY_DUNGEON_SECONDS * ticksPerSecond && countCreatures(playerId, false) < EMPTY_DUNGEON_CREATURES) {
            messages.hint(playerId, EMPTY_DUNGEON_HINT);
        }
        if (tick > 0 && tick % ((long) GENERAL_HINT_SECONDS * ticksPerSecond) == 0) {
            messages.hint(playerId, FIRST_GENERAL_HINT + ThreadLocalRandom.current().nextInt(GENERAL_HINTS));
        }
        if (tick > 0 && tick % ((long) QUIET_HINT_SECONDS * ticksPerSecond) == 0 && countCreatures(playerId, true) == 0) {
            messages.quietHint(playerId, 1 + ThreadLocalRandom.current().nextInt(QUIET_HINTS));
        }
    }

    /**
     * Counts a keeper's creatures
     *
     * @param playerId the keeper
     * @param busy true for the ones that fight or lie knocked out, false for
     * the ones that are not workers
     * @return the number of them
     */
    private int countCreatures(short playerId, boolean busy) {
        int count = 0;
        for (EntityId creature : entityData.findEntities(new FieldFilter<>(Owner.class, "ownerId", playerId), Owner.class, CreatureComponent.class)) {
            if (busy ? entityData.getComponent(creature, AttackTarget.class) != null || entityData.getComponent(creature, Unconscious.class) != null
                    : !entityData.getComponent(creature, CreatureComponent.class).worker) {
                count++;
            }
        }
        return count;
    }

    private void adviseOnReaper(Keeper keeper) {
        if (keeper.getMana() - keeper.getManaUpkeep() >= REAPER_ADVICE_MANA || !hasReaper(keeper.getId())) {
            return;
        }
        EntityId playerEntity = entityData.findEntity(new FieldFilter<>(KeeperComponent.class, "playerId", keeper.getId()), KeeperComponent.class);
        if (playerEntity != null && entityData.getComponent(playerEntity, ReaperAdvised.class) == null) {
            entityData.setComponent(playerEntity, new ReaperAdvised());
            messages.reaperAdvice(keeper.getId());
        }
    }

    private boolean hasReaper(short playerId) {
        for (EntitySet reaperSet : reapers) {
            for (Entity reaper : reaperSet) {
                if (reaper.get(Owner.class).ownerId == playerId) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        for (EntitySet reaperSet : reapers) {
            reaperSet.release();
        }
    }

}
