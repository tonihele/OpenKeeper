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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreaturePay;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

/**
 * Pays the keepers' creatures, like DKII does:
 * <ul>
 * <li>A keeper's pay day clock starts when the first creature that is not a
 * worker joins the keeper. The first pay day is the level's time before the
 * first pay day after that, the next ones follow at the pay day
 * frequency.</li>
 * <li>On pay day every creature of the keeper is owed its wage, the pay of its
 * level, on top of what it is still owed. Prisoners and the tortured are left
 * out, and so is a creature that joined less than the pay day cut off time
 * before the pay day.</li>
 * <li>A creature that is owed walks to one of the keeper's gold rooms to
 * collect it, see {@link toniarts.openkeeper.game.task.creature.CollectPay}.
 * After two tries that got nothing it waits for the next pay day.</li>
 * <li>While a creature is owed anything, its anger for not being paid grows
 * every tick, by its anger for no pay. It goes away when the creature is paid
 * in full.</li>
 * </ul>
 */
public final class PayDaySystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameTimer gameTimer;
    private final Map<Short, IPlayerController> keepers = new HashMap<>();
    private final Map<Short, Double> nextPayDays = new HashMap<>();
    private final double timeBeforeFirstPayDay;
    private final double payDayFrequency;
    private final double payDayCutOffTime;
    private final EntitySet creatureEntities;

    /**
     * The pay day system
     *
     * @param entityData the entity data
     * @param kwdFile the level data, for the creatures' anger
     * @param gameTimer the game time
     * @param playerControllers the players, only the keepers have pay days
     * @param gameSettings the level variables
     */
    public PayDaySystem(EntityData entityData, IKwdFile kwdFile, IGameTimer gameTimer,
            Collection<IPlayerController> playerControllers, Map<MiscType, MiscVariable> gameSettings) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameTimer = gameTimer;
        for (IPlayerController playerController : playerControllers) {
            short playerId = playerController.getKeeper().getId();
            if (playerId >= Player.KEEPER1_ID) {
                keepers.put(playerId, playerController);
            }
        }
        timeBeforeFirstPayDay = gameSettings.get(MiscType.TIME_BEFORE_FIRST_PAY_DAY_SECONDS).getValue();
        payDayFrequency = gameSettings.get(MiscType.PAY_DAY_FREQUENCY_SECONDS).getValue();
        // Despite the name the cut off time is in ticks, like in DKII.EXE (0x4BE180 returns ticks)
        payDayCutOffTime = gameSettings.get(MiscType.PAY_DAY_CUT_OFF_TIME_SECONDS).getValue() / kwdFile.getGameLevel().getTicksPerSec();

        creatureEntities = entityData.getEntities(CreatureComponent.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        creatureEntities.applyChanges();
        double gameTime = gameTimer.getGameTime();
        for (Entity entity : creatureEntities.getAddedEntities()) {
            skipPayDayIfTooLate(entity, gameTime);
        }

        for (Map.Entry<Short, IPlayerController> keeper : keepers.entrySet()) {
            Double nextPayDay = nextPayDays.get(keeper.getKey());
            if (nextPayDay == null) {
                startPayDayClock(keeper.getKey(), gameTime);
            } else if (gameTime >= nextPayDay) {
                payDay(keeper.getKey(), keeper.getValue(), gameTime);
            }
        }

        for (Entity entity : creatureEntities) {
            updateAnger(entity);
        }
    }

    /**
     * A creature that joins a keeper less than the cut off time before the
     * keeper's pay day isn't paid on it
     */
    private void skipPayDayIfTooLate(Entity entity, double gameTime) {
        Double nextPayDay = nextPayDays.get(entity.get(Owner.class).ownerId);
        if (nextPayDay != null && nextPayDay - gameTime < payDayCutOffTime
                && entityData.getComponent(entity.getId(), CreaturePay.class) == null) {
            entityData.setComponent(entity.getId(), new CreaturePay(0, 0, 0, true));
        }
    }

    private void startPayDayClock(short keeperId, double gameTime) {
        for (Entity entity : creatureEntities) {
            if (entity.get(Owner.class).ownerId == keeperId && !entity.get(CreatureComponent.class).worker) {
                nextPayDays.put(keeperId, gameTime + timeBeforeFirstPayDay);
                return;
            }
        }
    }

    private void payDay(short keeperId, IPlayerController keeper, double gameTime) {
        for (Entity entity : creatureEntities) {
            if (entity.get(Owner.class).ownerId == keeperId && isPaid(entity.getId())) {
                addWage(entity, gameTime);
            }
        }
        keeper.getStatsControl().payDay();
        nextPayDays.put(keeperId, gameTime + payDayFrequency);
    }

    /**
     * Prisoners and the tortured don't get paid
     */
    private boolean isPaid(EntityId entityId) {
        return entityData.getComponent(entityId, CreatureImprisoned.class) == null
                && entityData.getComponent(entityId, CreatureTortured.class) == null;
    }

    private void addWage(Entity entity, double gameTime) {
        EntityId entityId = entity.getId();
        CreaturePay pay = entityData.getComponent(entityId, CreaturePay.class);
        int unpaid = pay != null ? pay.unpaid : 0;
        if (pay != null && pay.skipNextPayDay) {
            setPay(entityId, unpaid, 0, gameTime);
            return;
        }

        setPay(entityId, unpaid + entity.get(CreatureComponent.class).pay, 0, gameTime);
    }

    private void setPay(EntityId entityId, int unpaid, int attempts, double nextAttemptTime) {
        if (unpaid > 0) {
            entityData.setComponent(entityId, new CreaturePay(unpaid, attempts, nextAttemptTime, false));
        } else {
            entityData.removeComponent(entityId, CreaturePay.class);
        }
    }

    /**
     * A creature is angry for as long as it is owed pay
     */
    private void updateAnger(Entity entity) {
        CreatureMood mood = entityData.getComponent(entity.getId(), CreatureMood.class);
        if (mood == null) {
            return;
        }

        CreaturePay pay = entityData.getComponent(entity.getId(), CreaturePay.class);
        if (pay == null || pay.unpaid <= 0) {
            if (mood.noPay != 0) {
                entityData.setComponent(entity.getId(), mood.clear(CreatureMood.REASON_NO_PAY));
            }
            return;
        }

        Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
        int anger = creature != null ? CreatureMood.toRuntimeValue(creature.getAttributes().getAngerNoPay()) : 0;
        if (anger != 0) {
            entityData.setComponent(entity.getId(), mood.add(CreatureMood.REASON_NO_PAY, anger));
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        creatureEntities.release();
        nextPayDays.clear();
    }

}
