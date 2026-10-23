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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;

/**
 * What the keeper's workshops manufacture next. Trap and door blueprints are
 * orders in the same list, in the order they were placed, as in the original
 * where placing either adds to the one manufacture list. The blueprints and
 * crates come from entity sets, no lookups over all entities.
 */
public final class WorkshopOrders {

    /**
     * An item to manufacture
     *
     * @param type trap or door
     * @param typeId the trap or door id
     * @param manufToBuild the manufacture points it needs
     * @param crateObjectId the object of its crate
     */
    public record Order(ResearchableType type, short typeId, int manufToBuild, short crateObjectId) {

    }

    /**
     * Blueprints and crates are matched by this, a trap crate doesn't do for a
     * door of the same id
     */
    private record Item(ResearchableType type, short typeId) {

    }

    private final IKwdFile kwdFile;
    private final EntitySet trapEntities;
    private final EntitySet doorEntities;
    private final EntitySet crateEntities;

    public WorkshopOrders(EntityData entityData, IKwdFile kwdFile) {
        this.kwdFile = kwdFile;

        trapEntities = entityData.getEntities(TrapComponent.class, TrapViewState.class, Owner.class);
        doorEntities = entityData.getEntities(DoorComponent.class, Owner.class);
        crateEntities = entityData.getEntities(Crate.class, Owner.class);
    }

    /**
     * The item the keeper's workshops manufacture next: the oldest blueprint,
     * trap or door, that doesn't have its crate yet
     *
     * @param playerId the keeper
     * @return the item, {@code null} if every blueprint has its crate
     */
    public Order getNext(short playerId) {
        trapEntities.applyChanges();
        doorEntities.applyChanges();
        crateEntities.applyChanges();

        // The blueprints in the order they were placed
        List<Entity> blueprints = new ArrayList<>();
        for (Entity entity : trapEntities) {
            if (entity.get(TrapViewState.class).blueprint && entity.get(Owner.class).ownerId == playerId) {
                blueprints.add(entity);
            }
        }
        for (Entity entity : doorEntities) {
            if (entity.get(DoorComponent.class).blueprint && entity.get(Owner.class).ownerId == playerId) {
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

        Map<Item, Integer> crates = new HashMap<>();
        for (Entity entity : crateEntities) {
            if (entity.get(Owner.class).ownerId == playerId) {
                Crate crate = entity.get(Crate.class);
                crates.merge(new Item(crate.type, crate.typeId), 1, Integer::sum);
            }
        }

        Item item = ManufactureOrders.next(items, crates);
        if (item == null) {
            return null;
        }
        if (item.type() == ResearchableType.DOOR) {
            Door door = kwdFile.getDoorById(item.typeId());
            return new Order(item.type(), item.typeId(), door.getManufToBuild(), door.getManufCrateObjectId());
        }
        Trap trap = kwdFile.getTrapById(item.typeId());
        return new Order(item.type(), item.typeId(), trap.getManufToBuild(), trap.getManufCrateObjectId());
    }

    /**
     * Releases the entity sets
     */
    public void release() {
        trapEntities.release();
        doorEntities.release();
        crateEntities.release();
    }

}
