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
import java.util.List;
import java.util.Map;
import java.util.Random;
import toniarts.openkeeper.game.component.CasinoBigWinner;
import toniarts.openkeeper.game.component.CasinoDisco;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The Casino's jackpot party. When a gambler becomes the big winner, it
 * celebrates and its Casino goes into disco mode, which the keeper's creatures
 * come to dance to. The disco lasts 90 seconds, or until nobody is left in the
 * Casino. If the big winner doesn't collect its winnings in time, all of the
 * keeper's creatures get angrier.
 */
public final class CasinoSystem implements IGameLogicUpdatable {

    /**
     * How long the disco lasts, as in DK II
     */
    private static final double DISCO_DURATION_SECONDS = 90;

    /**
     * How often the fireworks go off during the disco, as in DK II
     */
    private static final double FIREWORKS_INTERVAL_SECONDS = 12;

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final IGameTimer gameTimer;
    private final GameEventService gameEventService;
    private final Random random = new Random();
    private final int payTimeMissedAngerPercentage;
    private final EntitySet bigWinners;
    private final EntitySet discos;

    public CasinoSystem(EntityData entityData, ICreaturesController creaturesController, IMapController mapController,
            IEntityPositionLookup entityPositionLookup, IGameTimer gameTimer, GameEventService gameEventService,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;
        this.gameTimer = gameTimer;
        this.gameEventService = gameEventService;
        payTimeMissedAngerPercentage = (int) gameSettings.get(Variable.MiscVariable.MiscType.GAMBLING_JACKPOT_PAY_TIME_LIMIT_MISSED_ANGER_PERCENTAGE_MODIFIER).getValue();

        bigWinners = entityData.getEntities(CasinoBigWinner.class, Owner.class, Position.class);
        discos = entityData.getEntities(CasinoDisco.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        if (bigWinners.applyChanges()) {
            for (Entity bigWinner : bigWinners.getAddedEntities()) {
                startDisco(bigWinner);
            }
        }

        // The big winners' time to collect their winnings
        for (Entity bigWinner : bigWinners) {
            CasinoBigWinner casinoBigWinner = bigWinner.get(CasinoBigWinner.class);
            double timeLeft = casinoBigWinner.timeLeft - tpf;
            if (timeLeft > 0) {
                bigWinner.set(new CasinoBigWinner(timeLeft, casinoBigWinner.winnings, casinoBigWinner.slapCount));
            } else {
                payTimeMissed(bigWinner);
            }
        }

        // The disco is over after its time, or when nobody is left in the Casino
        discos.applyChanges();
        double gameTime = gameTimer.getGameTime();
        for (Entity disco : discos) {
            double discoTime = gameTime - disco.get(CasinoDisco.class).startTime;
            if (discoTime > DISCO_DURATION_SECONDS || !hasCreatures(disco.getId())) {
                entityData.removeComponent(disco.getId(), CasinoDisco.class);
                RoomComponent room = entityData.getComponent(disco.getId(), RoomComponent.class);
                gameEventService.emit(GameEventType.CASINO_DISCO_END, ThingFlag.ROOM, room.roomId, room.location);
            } else if ((long) (discoTime / FIREWORKS_INTERVAL_SECONDS) > (long) ((discoTime - tpf) / FIREWORKS_INTERVAL_SECONDS)) {
                fireworks(disco.getId());
            }
        }
    }

    private void startDisco(Entity bigWinner) {
        creaturesController.createController(bigWinner.getId()).getStateMachine().changeState(CreatureState.CELEBRATING);
        gameEventService.emit(GameEventType.CASINO_JACKPOT, bigWinner.get(Position.class).position);
        gameEventService.emitToPlayer(GameEventType.CASINO_JACKPOT_ANNOUNCED, bigWinner.get(Owner.class).ownerId);

        IRoomController casino = mapController.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(bigWinner.get(Position.class).position));
        if (casino != null && casino.hasObjectControl(ObjectType.GAMBLER)
                && entityData.getComponent(casino.getEntityId(), CasinoDisco.class) == null) {
            entityData.setComponent(casino.getEntityId(), new CasinoDisco(gameTimer.getGameTime()));
            RoomComponent room = entityData.getComponent(casino.getEntityId(), RoomComponent.class);
            gameEventService.emit(GameEventType.CASINO_DISCO_START, ThingFlag.ROOM, room.roomId, room.location);

            // As in DK II (0x4D6AA0), the keeper's other creatures in the Casino start dancing at once, the tired ones too
            for (Point p : casino.getRoomInstance().getCoordinates()) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(p)) {
                    Owner owner = entityData.getComponent(entityId, Owner.class);
                    if (!entityId.equals(bigWinner.getId()) && owner != null && owner.ownerId == bigWinner.get(Owner.class).ownerId
                            && entityData.getComponent(entityId, CreatureComponent.class) != null) {
                        ICreatureController creature = creaturesController.createController(entityId);
                        if (creature.goToDisco()) {
                            creature.getStateMachine().changeState(CreatureState.DANCING);
                        }
                    }
                }
            }
        }
    }

    /**
     * Fireworks go off over a random tile of the Casino
     */
    private void fireworks(EntityId casinoId) {
        List<Point> tiles = mapController.getRoomController(casinoId).getRoomInstance().getCoordinates();
        gameEventService.emit(GameEventType.CASINO_FIREWORKS, tiles.get(random.nextInt(tiles.size())));
    }

    private void payTimeMissed(Entity bigWinner) {
        entityData.removeComponent(bigWinner.getId(), CasinoBigWinner.class);

        // All of the keeper's creatures get angrier
        FieldFilter<Owner> keeper = new FieldFilter<>(Owner.class, "ownerId", bigWinner.get(Owner.class).ownerId);
        for (EntityId creature : entityData.findEntities(keeper, Owner.class, CreatureMood.class)) {
            CreatureMood mood = entityData.getComponent(creature, CreatureMood.class);
            entityData.setComponent(creature, mood.add(CreatureMood.REASON_OTHER, mood.other * payTimeMissedAngerPercentage / 100));
        }
    }

    private boolean hasCreatures(EntityId casinoId) {
        IRoomController casino = mapController.getRoomController(casinoId);
        if (casino == null) {
            return false;
        }
        for (Point p : casino.getRoomInstance().getCoordinates()) {
            for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(p)) {
                if (entityData.getComponent(entityId, CreatureComponent.class) != null) {
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
        bigWinners.release();
        discos.release();
    }

}
