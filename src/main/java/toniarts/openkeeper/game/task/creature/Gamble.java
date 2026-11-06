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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import java.util.Map;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import toniarts.openkeeper.game.component.CasinoBigWinner;
import toniarts.openkeeper.game.component.CasinoDisco;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.CasinoController.PayOut;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

/**
 * Gambles in the Casino, the room for the Drink job. Once a second the
 * gambler's mood and gold change by the pay out setting of the casino and it
 * may hit the jackpot. Every four seconds the keeper pays out the casino's
 * losses or gets its takings.
 */
public final class Gamble extends AbstractCapacityCriticalRoomTask {

    /**
     * The number the jackpot roll has to hit, as in the original
     */
    public static final int JACKPOT_NUMBER = 42;
    /**
     * How often the keeper pays or gets the casino's share, in seconds
     */
    public static final int CASINO_SHARE_INTERVAL_SECONDS = 4;

    private final EntityData entityData;
    private final IGameWorldController gameWorldController;
    private final IObjectsController objectsController;
    private final Supplier<PayOut> payOut;
    private final RandomGenerator random;
    private final int smilesGoldPerSecond;
    private final int moneyGoldPerSecond;
    private final int smilesAngerPerSecond;
    private final int moneyAngerPerSecond;
    private final int smilesJackpotChance;
    private final int moneyJackpotChance;
    private final int jackpotAmount;
    private final int jackpotPayTimeLimit;
    private final int maxGoldPile;
    private int secondsGambled = 0;

    /**
     * Gambling in the Casino
     *
     * @param navigationService the navigation service
     * @param mapController the map controller
     * @param p the casino furniture to gamble at
     * @param playerId the owner of the casino
     * @param room the casino
     * @param taskManager the task manager
     * @param entityData the entity data
     * @param gameWorldController the game world controller, for the keeper's
     * gold
     * @param objectsController the objects controller, for the casino's
     * takings
     * @param gameSettings the level variables
     * @param payOut the pay out setting of the casino
     * @param random the random number generator
     */
    public Gamble(final INavigationService navigationService, final IMapController mapController, Point p, short playerId,
            IRoomController room, TaskManager taskManager, EntityData entityData, IGameWorldController gameWorldController,
            IObjectsController objectsController, Map<MiscType, MiscVariable> gameSettings, Supplier<PayOut> payOut,
            RandomGenerator random) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.entityData = entityData;
        this.gameWorldController = gameWorldController;
        this.objectsController = objectsController;
        this.payOut = payOut;
        this.random = random;
        smilesGoldPerSecond = getSetting(gameSettings, MiscType.GAMBLING_MODIFY_CREATURE_MONEY_SMILES_PER_SECOND);
        moneyGoldPerSecond = getSetting(gameSettings, MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND);
        smilesAngerPerSecond = getSetting(gameSettings, MiscType.GAMBLING_MODIFY_CREATURE_ANGER_SMILES_PER_SECOND);
        moneyAngerPerSecond = getSetting(gameSettings, MiscType.GAMBLING_MODIFY_CREATURE_ANGER_MONEY_PER_SECOND);
        smilesJackpotChance = getSetting(gameSettings, MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_SMILES_PER_SECOND);
        moneyJackpotChance = getSetting(gameSettings, MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_MONEY_PER_SECOND);
        jackpotAmount = getSetting(gameSettings, MiscType.GAMBLING_JACKPOT_BASIC_AMOUNT);
        jackpotPayTimeLimit = getSetting(gameSettings, MiscType.GAMBLING_JACKPOT_PAY_TIME_LIMIT);
        maxGoldPile = getSetting(gameSettings, MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY);
    }

    @Override
    public boolean isValid(ICreatureController creature) {

        // The creature must be able to cover the stake of the money mode. The
        // big winner and the gamblers of a casino in disco mode stop gambling
        return creature != null && canCoverStake(creature) && !isBigWinner(creature)
                && entityData.getComponent(getRoom().getEntityId(), CasinoDisco.class) == null && super.isValid(creature);
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return getAccessibleTargetNextToLocation(creature);
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.GAMBLER;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        while (secondsGambled + 1 <= executionDuration && canCoverStake(creature) && !isBigWinner(creature)) {
            secondsGambled++;
            gamble(creature, payOut.get());
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GAMBLE;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }

    private boolean canCoverStake(ICreatureController creature) {
        return creature.getGold() > Math.abs(moneyGoldPerSecond);
    }

    private void gamble(ICreatureController creature, PayOut mode) {
        boolean smiles = (mode == PayOut.SMILES);

        // The mood
        CreatureMood mood = entityData.getComponent(creature.getEntityId(), CreatureMood.class);
        if (mood != null) {
            int anger = (smiles ? smilesAngerPerSecond : moneyAngerPerSecond);
            entityData.setComponent(creature.getEntityId(), mood.add(CreatureMood.REASON_OTHER, anger)
                    .add(CreatureMood.REASON_GENERAL, anger)
                    .add(CreatureMood.REASON_NO_WORK, anger));
        }

        // The creature's gold
        if (smiles) {
            addGold(creature, smilesGoldPerSecond);
        } else {
            creature.substractGold(Math.min(creature.getGold(), Math.abs(moneyGoldPerSecond)));
        }

        // The casino's share
        if (secondsGambled % CASINO_SHARE_INTERVAL_SECONDS == 0) {
            if (smiles) {
                gameWorldController.substractGold(CASINO_SHARE_INTERVAL_SECONDS * smilesGoldPerSecond, playerId);
            } else {
                Point centre = getRoom().getRoomInstance().getCenter();
                objectsController.addLooseGold(playerId, centre.x, centre.y,
                        CASINO_SHARE_INTERVAL_SECONDS * Math.abs(moneyGoldPerSecond), maxGoldPile);
            }
        }

        // The jackpot
        int chance = (smiles ? smilesJackpotChance : moneyJackpotChance);
        if (chance > 0 && random.nextInt(chance) == JACKPOT_NUMBER) {
            hitJackpot(creature);
        }
    }

    /**
     * Hits the jackpot. The creature becomes the keeper's big winner, unless
     * the keeper has one already. It wins the basic jackpot or the keeper's
     * pay day wages, whichever is more, and has
     * GAMBLING_JACKPOT_PAY_TIME_LIMIT seconds to collect them from the keeper.
     *
     * @param creature the lucky gambler
     * @return true if the creature became the big winner
     */
    public boolean hitJackpot(ICreatureController creature) {
        FieldFilter<Owner> keeper = new FieldFilter<>(Owner.class, "ownerId", playerId);
        if (!entityData.findEntities(keeper, Owner.class, CasinoBigWinner.class).isEmpty()) {
            return false;
        }

        int wages = 0;
        for (EntityId entityId : entityData.findEntities(keeper, Owner.class, CreatureComponent.class)) {
            wages += entityData.getComponent(entityId, CreatureComponent.class).pay;
        }
        entityData.setComponent(creature.getEntityId(), new CasinoBigWinner(jackpotPayTimeLimit, Math.max(jackpotAmount, wages), 0));

        return true;
    }

    private boolean isBigWinner(ICreatureController creature) {
        return entityData.getComponent(creature.getEntityId(), CasinoBigWinner.class) != null;
    }

    /**
     * Adds gold to the creature, up to the gold it can carry. The rest is lost.
     */
    private static void addGold(ICreatureController creature, int amount) {
        int added = Math.min(amount, creature.getMaxGold() - creature.getGold());
        if (added > 0) {
            creature.addGold(added);
        }
    }

    private static int getSetting(Map<MiscType, MiscVariable> gameSettings, MiscType type) {
        return (int) gameSettings.get(type).getValue();
    }

}
