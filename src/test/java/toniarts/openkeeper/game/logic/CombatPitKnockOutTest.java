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
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.CombatPitFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CombatPitKnockOutTest {

    private static final short FIGHTER = 1;

    @Test
    void knockedOutPitFighterLeavesTheFight() {
        EntityData entityData = new DefaultEntityData();
        EntityId fighter = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = FIGHTER;
        entityData.setComponents(fighter, creatureComponent, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID),
                new Health(10, 100), new Damage(20), new CombatPitFighter(entityData.createEntity()));

        HealthSystem healthSystem = new HealthSystem(entityData, null, Map.of(
                MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, CombatPitFixtures.pitVariable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 60),
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, CombatPitFixtures.pitVariable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, 0),
                MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, CombatPitFixtures.pitVariable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 0)),
                stub(ICreaturesController.class), levelInfo(CombatPitFixtures.pitCreature(FIGHTER)), List.of(), null, () -> 1.0,
                stub(GameEventService.class));
        healthSystem.processTick(1f);

        assertNotNull(entityData.getComponent(fighter, Unconscious.class));
        assertNull(entityData.getComponent(fighter, CombatPitFighter.class));
    }

    private static ILevelInfo levelInfo(Creature creature) {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> "getCreature".equals(method.getName()) ? creature : null);
        return (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> "getLevelData".equals(method.getName()) ? kwdFile : null);
    }

    /**
     * A stub that does nothing, and whose objects do nothing either
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) {
                return false;
            }
            if (returnType.isInterface()) {
                return stub(returnType);
            }
            return returnType.isPrimitive() && returnType != void.class ? 0 : null;
        });
    }

}
