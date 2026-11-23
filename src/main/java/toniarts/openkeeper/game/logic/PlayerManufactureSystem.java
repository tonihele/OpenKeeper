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
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.AndFilter;
import com.simsilica.es.filter.FieldFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ManufactureOrders;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Keeps tabs on the trap and door blueprints and their crates, and tells each
 * keeper's workshops what to manufacture next. Trap and door blueprints are
 * orders in the same list, in the order they were placed, as in the original
 * where placing either adds to the one manufacture list.<br>
 * While none of a keeper's creatures works in a workshop, the keeper's
 * manufacture points drop by MANUFACTURE_POINT_LOSS_WHILE_NO_WORKERS_PER_SECOND
 * once a second, down to zero, as in the original.
 */
public final class PlayerManufactureSystem implements IGameLogicUpdatable {

    /**
     * Blueprints and crates are matched by this, a trap crate doesn't do for a
     * door of the same id
     */
    private record Item(ResearchableType type, short typeId) {

    }

    private final IKwdFile kwdFile;
    private final EntitySet trapBlueprintEntities;
    private final EntitySet doorBlueprintEntities;
    private final EntitySet crateEntities;
    private final EntitySet manufacturerEntities;
    private final Map<Short, PlayerManufactureControl> manufactureControlsByPlayerId;
    private final IGameTimer gameTimer;
    private final int pointLossPerSecond;

    public PlayerManufactureSystem(EntityData entityData, IKwdFile kwdFile, Collection<IPlayerController> playerControllers,
            IGameTimer gameTimer) {
        this.kwdFile = kwdFile;
        this.gameTimer = gameTimer;
        pointLossPerSecond = (int) kwdFile.getVariables().get(Variable.MiscVariable.MiscType.MANUFACTURE_POINT_LOSS_WHILE_NO_WORKERS_PER_SECOND).getValue();
        manufactureControlsByPlayerId = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController playerController : playerControllers) {
            if (playerController.getManufactureControl() != null) {
                manufactureControlsByPlayerId.put(playerController.getKeeper().getId(), playerController.getManufactureControl());
            }
        }

        trapBlueprintEntities = entityData.getEntities(new FieldFilter(TrapViewState.class, "blueprint", true), TrapComponent.class, TrapViewState.class, Owner.class);
        doorBlueprintEntities = entityData.getEntities(new FieldFilter(DoorComponent.class, "blueprint", true), DoorComponent.class, Owner.class);
        crateEntities = entityData.getEntities(Crate.class, Owner.class);
        manufacturerEntities = entityData.getEntities(new AndFilter(TaskComponent.class,
                new FieldFilter(TaskComponent.class, "taskStarted", true),
                new FieldFilter(TaskComponent.class, "taskType", TaskType.MANUFACTURE)), TaskComponent.class, Owner.class);
        updateOrders();
    }

    @Override
    public void processTick(float tpf) {
        boolean trapBlueprintsChanged = trapBlueprintEntities.applyChanges();
        boolean doorBlueprintsChanged = doorBlueprintEntities.applyChanges();
        boolean cratesChanged = crateEntities.applyChanges();
        if (trapBlueprintsChanged || doorBlueprintsChanged || cratesChanged) {
            updateOrders();
        }

        // Once a second, on the tick that starts a new second of game time
        manufacturerEntities.applyChanges();
        double gameTime = gameTimer.getGameTime();
        if (Math.floor(gameTime) == Math.floor(gameTime - tpf)) {
            return;
        }
        for (Map.Entry<Short, PlayerManufactureControl> entry : manufactureControlsByPlayerId.entrySet()) {
            if (manufacturerEntities.stream().noneMatch(entity -> entity.get(Owner.class).ownerId == entry.getKey())) {
                entry.getValue().losePoints(pointLossPerSecond);
            }
        }
    }

    private void updateOrders() {
        for (Map.Entry<Short, PlayerManufactureControl> entry : manufactureControlsByPlayerId.entrySet()) {
            Item item = ManufactureOrders.next(getBlueprints(entry.getKey()), getCrates(entry.getKey()));
            entry.getValue().setOrder(item != null ? toOrder(item) : null);
        }
    }

    /**
     * @return the trap or door of each of the keeper's blueprints, in the
     * order they were placed
     */
    private List<Item> getBlueprints(short playerId) {
        List<Entity> blueprints = new ArrayList<>();
        for (Entity entity : trapBlueprintEntities) {
            if (entity.get(Owner.class).ownerId == playerId) {
                blueprints.add(entity);
            }
        }
        for (Entity entity : doorBlueprintEntities) {
            if (entity.get(Owner.class).ownerId == playerId) {
                blueprints.add(entity);
            }
        }
        blueprints.sort(Comparator.comparingLong(entity -> entity.getId().getId()));
        List<Item> items = new ArrayList<>(blueprints.size());
        for (Entity blueprint : blueprints) {
            DoorComponent doorComponent = blueprint.get(DoorComponent.class);
            items.add(doorComponent != null
                    ? new Item(ResearchableType.DOOR, doorComponent.doorId)
                    : new Item(ResearchableType.TRAP, blueprint.get(TrapComponent.class).trapId));
        }
        return items;
    }

    /**
     * @return the number of the keeper's crates by trap or door, in the
     * workshop or on their way
     */
    private Map<Item, Integer> getCrates(short playerId) {
        Map<Item, Integer> crates = new HashMap<>();
        for (Entity entity : crateEntities) {
            if (entity.get(Owner.class).ownerId == playerId) {
                Crate crate = entity.get(Crate.class);
                crates.merge(new Item(crate.type, crate.typeId), 1, Integer::sum);
            }
        }
        return crates;
    }

    private ManufactureOrders.Order toOrder(Item item) {
        if (item.type() == ResearchableType.DOOR) {
            Door door = kwdFile.getDoorById(item.typeId());
            return new ManufactureOrders.Order(item.type(), item.typeId(), door.getManufToBuild(), door.getManufCrateObjectId());
        }
        Trap trap = kwdFile.getTrapById(item.typeId());
        return new ManufactureOrders.Order(item.type(), item.typeId(), trap.getManufToBuild(), trap.getManufCrateObjectId());
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        trapBlueprintEntities.release();
        doorBlueprintEntities.release();
        crateEntities.release();
        manufacturerEntities.release();
    }
}
