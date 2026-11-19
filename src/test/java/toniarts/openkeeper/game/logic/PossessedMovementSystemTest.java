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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A possessed creature moves at the speed its keeper asks for, changed by a
 * haste, slow or slap while that runs
 */
class PossessedMovementSystemTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void movesFasterWhileItsSpeedIsChanged() {
        EntityId us = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.speed = 1;
        entityData.setComponents(us, creatureComponent, new Possessed(0, 0), new Position(0, new Vector3f(10, 0, 10)),
                new PossessedMovement(new Vector2f(0, 1), 0, 0, 2), new Mobile(false, false, false, 1.5f), new SpeedChange(100));
        PossessedMovementSystem system = createSystem();

        // Hasted to 150 %
        system.processTick(0.1f);
        assertEquals(10.3f, entityData.getComponent(us, Position.class).position.z, 0.0001f);

        // The haste has run out
        entityData.removeComponent(us, SpeedChange.class);
        entityData.setComponent(us, new Mobile(false, false, false, 1));
        system.processTick(0.1f);
        assertEquals(10.5f, entityData.getComponent(us, Position.class).position.z, 0.0001f);
    }

    private PossessedMovementSystem createSystem() {
        IMapTileInformation tile = stub(IMapTileInformation.class, name -> null);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, name -> tile);
        IMapController mapController = stub(IMapController.class, name -> "getMapData".equals(name) ? mapData : null);
        ICreatureController navigable = stub(ICreatureController.class, name -> "getCost".equals(name) ? 1f : null);
        ICreaturesController creaturesController = stub(ICreaturesController.class, name -> navigable);
        return new PossessedMovementSystem(entityData, mapController, null, creaturesController);
    }

    private static <T> T stub(Class<T> type, Function<String, Object> answer) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.apply(method.getName())));
    }

}
