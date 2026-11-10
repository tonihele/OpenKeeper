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
import java.util.Random;
import java.util.function.Function;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.navigation.INavigationService;

/**
 * Keeps the summoned Horned Reapers. The keeper pays nothing for the first 50
 * seconds, after that the Reaper costs the mana drain every second. When the
 * keeper has less mana than that, the Reaper leaves. A Reaper summoned for a
 * set time leaves when the time is up, and costs nothing. Slapping the Reaper
 * sends him back too, and so does running out of things to destroy. A leaving
 * Reaper stops what he is doing and is gone six seconds later.
 */
public final class SummonedReaperSystem implements IGameLogicUpdatable {

    /**
     * How long the Reaper is free of charge after he has been summoned
     */
    public static final double UPKEEP_DELAY_SECONDS = 50;

    /**
     * How long the Reaper takes to leave
     */
    public static final double LEAVE_DURATION_SECONDS = 6;

    private static final double MANA_CHECK_INTERVAL_SECONDS = 1;

    /**
     * Decides what a Reaper does next
     */
    @FunctionalInterface
    interface ReaperDecisions {

        /**
         * Lets the Reaper decide what to do next, when he isn't busy
         *
         * @param reaper the Reaper
         * @param ownerId the keeper who summoned him
         * @return {@code false} if there is nothing left for the Reaper to do
         */
        boolean think(EntityId reaper, short ownerId);

        /**
         * Releases the resources
         */
        default void stop() {
        }

    }

    private final EntityData entityData;
    private final EntitySet reapers;
    private final Map<Short, Keeper> keepers;
    private final ICreaturesController creaturesController;
    private final IGameTimer gameTimer;
    private final ReaperDecisions decisions;

    public SummonedReaperSystem(EntityData entityData, Collection<IPlayerController> playerControllers,
            ICreaturesController creaturesController, IGameTimer gameTimer, INavigationService navigationService) {
        this(entityData, playerControllers, creaturesController, gameTimer,
                keepers -> new ReaperAi(entityData, keepers, creaturesController, navigationService, new Random()));
    }

    SummonedReaperSystem(EntityData entityData, Collection<IPlayerController> playerControllers,
            ICreaturesController creaturesController, IGameTimer gameTimer, ReaperDecisions decisions) {
        this(entityData, playerControllers, creaturesController, gameTimer, keepers -> decisions);
    }

    private SummonedReaperSystem(EntityData entityData, Collection<IPlayerController> playerControllers,
            ICreaturesController creaturesController, IGameTimer gameTimer, Function<Map<Short, Keeper>, ReaperDecisions> decisionsFactory) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        this.gameTimer = gameTimer;
        keepers = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController playerController : playerControllers) {
            Keeper keeper = playerController.getKeeper();
            keepers.put(keeper.getId(), keeper);
        }
        decisions = decisionsFactory.apply(keepers);

        reapers = entityData.getEntities(SummonedReaper.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        reapers.applyChanges();

        double gameTime = gameTimer.getGameTime();
        for (Entity entity : reapers) {
            EntityId entityId = entity.getId();
            SummonedReaper summonedReaper = entity.get(SummonedReaper.class);
            short ownerId = entity.get(Owner.class).ownerId;

            if (summonedReaper.isLeaving()) {
                if (gameTime >= summonedReaper.leaveTime + LEAVE_DURATION_SECONDS) {
                    creaturesController.createController(entityId).remove();
                }
            } else if (isDismissed(entityId, ownerId, summonedReaper, gameTime)) {
                leave(entityId, summonedReaper, gameTime);
            } else if (entityData.getComponent(entityId, CreatureAi.class) != null && !decisions.think(entityId, ownerId)) {

                // He only thinks when he is on his feet, not while still falling from the sky
                leave(entityId, summonedReaper, gameTime);
            }
        }
    }

    private boolean isDismissed(EntityId entityId, short ownerId, SummonedReaper summonedReaper, double gameTime) {

        // A slap sends him back
        Slapped slapped = entityData.getComponent(entityId, Slapped.class);
        if (slapped != null && slapped.startTime >= summonedReaper.summonTime) {
            return true;
        }

        if (summonedReaper.lifetime > 0) {
            return gameTime >= summonedReaper.summonTime + summonedReaper.lifetime;
        }
        if (summonedReaper.manaDrain > 0 && gameTime >= summonedReaper.summonTime + UPKEEP_DELAY_SECONDS) {
            return !payUpkeep(entityId, ownerId, summonedReaper, gameTime);
        }

        return false;
    }

    /**
     * Charges the upkeep
     *
     * @return {@code false} if the keeper can't pay
     */
    private boolean payUpkeep(EntityId entityId, short ownerId, SummonedReaper summonedReaper, double gameTime) {
        if (!summonedReaper.isPaying()) {
            addManaGeneration(entityId, -summonedReaper.manaDrain);
            setSummonedReaper(entityId, summonedReaper, gameTime, summonedReaper.leaveTime);
            return true;
        }
        if (gameTime - summonedReaper.manaCheckTime < MANA_CHECK_INTERVAL_SECONDS) {
            return true;
        }

        // The drain is already in the mana loss, he leaves if the keeper couldn't pay another second
        Keeper keeper = keepers.get(ownerId);
        if (keeper == null || keeper.getMana() < summonedReaper.manaDrain) {
            return false;
        }
        setSummonedReaper(entityId, summonedReaper, gameTime, summonedReaper.leaveTime);
        return true;
    }

    private void leave(EntityId entityId, SummonedReaper summonedReaper, double gameTime) {
        if (summonedReaper.isPaying()) {
            addManaGeneration(entityId, summonedReaper.manaDrain);
        }
        setSummonedReaper(entityId, summonedReaper, SummonedReaper.NOT_YET, gameTime);

        // He's done, nothing more to do or go to
        ICreatureController creatureController = creaturesController.createController(entityId);
        creatureController.unassingCurrentTask();
        entityData.removeComponent(entityId, CreatureAi.class);
        entityData.removeComponent(entityId, Navigation.class);
    }

    private void setSummonedReaper(EntityId entityId, SummonedReaper summonedReaper, double manaCheckTime, double leaveTime) {
        entityData.setComponent(entityId, new SummonedReaper(summonedReaper.summonTime, summonedReaper.lifetime,
                summonedReaper.manaDrain, manaCheckTime, leaveTime));
    }

    private void addManaGeneration(EntityId entityId, int amount) {
        Mana mana = entityData.getComponent(entityId, Mana.class);
        int manaGeneration = (mana != null ? mana.manaGeneration : 0) + amount;
        if (manaGeneration == 0) {
            entityData.removeComponent(entityId, Mana.class);
        } else {
            entityData.setComponent(entityId, new Mana(manaGeneration));
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        reapers.release();
        decisions.stop();
    }

}
