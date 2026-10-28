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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedAttack;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The possessed attack handling of the possessed system
 */
class PossessedSystemTest {

    private final EntityData entityData = new DefaultEntityData();

    private <T> T stub(Class<T> type, Function<String, Object> answer) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> answer.apply(method.getName())));
    }

    private PossessedSystem system(Runnable onSwing) {
        ICreatureController controller = stub(ICreatureController.class, name -> {
            onSwing.run();
            return null;
        });
        ICreaturesController creatures = stub(ICreaturesController.class, name -> controller);
        IGameController game = stub(IGameController.class, name -> null);
        return new PossessedSystem(List.of(), entityData, game, creatures);
    }

    @Test
    void releasedAttackIsRemovedAfterItsSwing() {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Possessed(0, 0), new PossessedAttack(false, 1));

        system(() -> { }).processTick(0.1f);

        assertNull(entityData.getComponent(creature, PossessedAttack.class));
    }

    @Test
    void newPressDuringSwingIsKept() {
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Possessed(0, 0), new PossessedAttack(false, 1));

        // The session thread writes a new press while the swing is processed
        system(() -> entityData.setComponent(creature, new PossessedAttack(true, 2))).processTick(0.1f);

        PossessedAttack attack = entityData.getComponent(creature, PossessedAttack.class);
        assertTrue(attack.held);
        assertEquals(2, attack.pressTime);
    }
}
