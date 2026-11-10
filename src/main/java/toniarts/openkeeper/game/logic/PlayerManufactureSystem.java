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
import com.simsilica.es.filter.FieldFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ManufactureOrders;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Keeps tabs on the trap blueprints and the trap crates, and tells each
 * keeper's workshops which trap to manufacture next
 */
public final class PlayerManufactureSystem implements IGameLogicUpdatable {

    private final IKwdFile kwdFile;
    private final EntitySet blueprintEntities;
    private final EntitySet crateEntities;
    private final Map<Short, PlayerManufactureControl> manufactureControlsByPlayerId;

    public PlayerManufactureSystem(EntityData entityData, IKwdFile kwdFile, Collection<IPlayerController> playerControllers) {
        this.kwdFile = kwdFile;
        manufactureControlsByPlayerId = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController playerController : playerControllers) {
            if (playerController.getManufactureControl() != null) {
                manufactureControlsByPlayerId.put(playerController.getKeeper().getId(), playerController.getManufactureControl());
            }
        }

        blueprintEntities = entityData.getEntities(new FieldFilter(TrapViewState.class, "blueprint", true), TrapComponent.class, TrapViewState.class, Owner.class);
        crateEntities = entityData.getEntities(new FieldFilter(Crate.class, "type", ResearchableType.TRAP), Crate.class, Owner.class);
        updateOrders();
    }

    @Override
    public void processTick(float tpf) {
        boolean blueprintsChanged = blueprintEntities.applyChanges();
        boolean cratesChanged = crateEntities.applyChanges();
        if (blueprintsChanged || cratesChanged) {
            updateOrders();
        }
    }

    private void updateOrders() {
        for (Map.Entry<Short, PlayerManufactureControl> entry : manufactureControlsByPlayerId.entrySet()) {
            Short trapId = ManufactureOrders.next(getBlueprints(entry.getKey()), getCrates(entry.getKey()));
            entry.getValue().setTrapToManufacture(trapId != null ? kwdFile.getTrapById(trapId) : null);
        }
    }

    /**
     * @return the trap of each of the keeper's blueprints, in the order they
     * were placed
     */
    private List<Short> getBlueprints(short playerId) {
        List<Entity> blueprints = new ArrayList<>();
        for (Entity entity : blueprintEntities) {
            if (entity.get(Owner.class).ownerId == playerId) {
                blueprints.add(entity);
            }
        }
        blueprints.sort(Comparator.comparingLong(entity -> entity.getId().getId()));
        List<Short> trapIds = new ArrayList<>(blueprints.size());
        for (Entity blueprint : blueprints) {
            trapIds.add(blueprint.get(TrapComponent.class).trapId);
        }
        return trapIds;
    }

    /**
     * @return the number of the keeper's trap crates by trap, in the workshop
     * or on their way
     */
    private Map<Short, Integer> getCrates(short playerId) {
        Map<Short, Integer> crates = new HashMap<>();
        for (Entity entity : crateEntities) {
            if (entity.get(Owner.class).ownerId == playerId) {
                crates.merge(entity.get(Crate.class).typeId, 1, Integer::sum);
            }
        }
        return crates;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        blueprintEntities.release();
        crateEntities.release();
    }
}
