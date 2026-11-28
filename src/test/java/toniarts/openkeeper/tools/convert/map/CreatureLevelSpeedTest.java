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
package toniarts.openkeeper.tools.convert.map;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that a creature's speeds don't grow with its level, as in the
 * original. This package also permits parser-only fixture setters.
 */
class CreatureLevelSpeedTest {

    private static final short KNIGHT = 13;

    @Test
    void levelTenCreatureWalksAndRunsAsFastAsOnLevelOne() {
        Creature knight = new Creature();
        knight.setCreatureId(KNIGHT);
        knight.setName("Knight");
        knight.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));
        knight.setMeleeAttackType(Creature.AttackType.MELEE_BLADE);
        knight.setSpells(List.of());
        knight.getAttributes().setHp(1000);
        knight.getAttributes().setSpeed(0.5f);
        knight.getAttributes().setRunSpeed(0.9f);
        Creature imp = new Creature();
        imp.setCreatureId((short) 1);

        // Every stat doubles on level 10, the speeds included
        Map<Variable.CreatureStats.StatType, Variable.CreatureStats> levelTen = new EnumMap<>(Variable.CreatureStats.StatType.class);
        for (Variable.CreatureStats.StatType statType : Variable.CreatureStats.StatType.values()) {
            Variable.CreatureStats stat = new Variable.CreatureStats();
            stat.setStatId(statType);
            stat.setLevel(10);
            stat.setValue(200);
            levelTen.put(statType, stat);
        }
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> switch (method) {
            case "getThings" -> List.of();
            case "getCreature" -> knight;
            case "getImp" -> imp;
            case "getCreatureStats" -> levelTen;
            default -> throw new AssertionError(method);
        });
        DefaultEntityData entityData = new DefaultEntityData();
        CreaturesController creaturesController = new CreaturesController(kwdFile, entityData, Map.of(),
                stub(IGameTimer.class, (method, args) -> 0.0), stub(IGameController.class, (method, args) -> null),
                stub(IMapController.class, (method, args) -> null), stub(ILevelInfo.class, (method, args) -> null));

        EntityId entity = creaturesController.spawnCreature(KNIGHT, Player.GOOD_PLAYER_ID, 10, new Vector2f(5, 5),
                ICreaturesController.SpawnType.PLACE);

        assertEquals(2000, entityData.getComponent(entity, Health.class).maxHealth);
        assertEquals(0.5f, entityData.getComponent(entity, Mobile.class).maxSpeed);
        assertEquals(0.5f, entityData.getComponent(entity, CreatureComponent.class).speed);
        assertEquals(0.9f, entityData.getComponent(entity, CreatureComponent.class).runSpeed);
    }

    @FunctionalInterface
    private interface Calls {

        Object call(String method, Object[] args);
    }

    private static <T> T stub(Class<T> type, Calls calls) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> calls.call(method.getName(), args)));
    }

}
