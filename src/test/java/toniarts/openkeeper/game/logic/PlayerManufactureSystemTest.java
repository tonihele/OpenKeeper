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

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerManufactureSystemTest {

    private static final short PLAYER = 3;
    private static final short ENEMY = 4;
    private static final short SENTRY = 1;
    private static final short BOULDER = 2;

    private final DefaultEntityData entityData = new DefaultEntityData();
    private final PlayerManufactureControl manufactureControl = new PlayerManufactureControl();

    @Test
    void oldestBlueprintWithoutACrateIsNext() {
        addBlueprint(SENTRY, PLAYER);
        addBlueprint(BOULDER, PLAYER);
        PlayerManufactureSystem system = createSystem();

        assertEquals(SENTRY, getTrapToManufacture());

        addCrate(ResearchableType.TRAP, SENTRY, PLAYER);
        system.processTick(0);
        assertEquals(BOULDER, getTrapToManufacture());

        addCrate(ResearchableType.TRAP, BOULDER, PLAYER);
        system.processTick(0);
        assertNull(manufactureControl.getTrapToManufacture());
    }

    @Test
    void armedTrapIsNoOrder() {
        EntityId blueprint = addBlueprint(SENTRY, PLAYER);
        PlayerManufactureSystem system = createSystem();

        entityData.setComponent(blueprint, new TrapViewState(SENTRY, false));
        system.processTick(0);

        assertNull(manufactureControl.getTrapToManufacture());
    }

    @Test
    void removedCrateIsManufacturedAgain() {
        addBlueprint(SENTRY, PLAYER);
        EntityId crate = addCrate(ResearchableType.TRAP, SENTRY, PLAYER);
        PlayerManufactureSystem system = createSystem();
        assertNull(manufactureControl.getTrapToManufacture());

        entityData.removeEntity(crate);
        system.processTick(0);

        assertEquals(SENTRY, getTrapToManufacture());
    }

    @Test
    void onlyTheKeepersOwnTrapCratesCount() {
        addBlueprint(BOULDER, ENEMY);
        addBlueprint(SENTRY, PLAYER);
        addCrate(ResearchableType.TRAP, SENTRY, ENEMY);
        addCrate(ResearchableType.DOOR, SENTRY, PLAYER);
        createSystem();

        assertEquals(SENTRY, getTrapToManufacture());
    }

    private PlayerManufactureSystem createSystem() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTrapById" -> TrapFixtures.trap(((Integer) args[0]).shortValue(), 0);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        Keeper keeper = new Keeper(false, PLAYER);
        IPlayerController player = (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getKeeper" -> keeper;
                    case "getManufactureControl" -> manufactureControl;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new PlayerManufactureSystem(entityData, kwdFile, List.of(player));
    }

    private short getTrapToManufacture() {
        Trap trap = manufactureControl.getTrapToManufacture();
        return trap != null ? trap.getTrapId() : -1;
    }

    private EntityId addBlueprint(short trapId, short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new TrapComponent(trapId), new TrapViewState(trapId, true), new Owner(ownerId, ownerId));
        return entity;
    }

    private EntityId addCrate(ResearchableType type, short typeId, short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new Crate(type, typeId), new Owner(ownerId, ownerId));
        return entity;
    }

}
