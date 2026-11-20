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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.player.PlayerManaControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A possessed creature is never knocked out like the others. It dies, which
 * ends the possession and costs its keeper mana.
 */
class PossessedDeathTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final int MANA_REDUCTION = 100;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Boolean> possessionCalls = new ArrayList<>();
    private Keeper keeper;
    private HealthSystem healthSystem;
    private EntityId creature;

    @BeforeEach
    void setUp() {
        creature = entityData.createEntity();
        entityData.setComponents(creature, new CreatureComponent(), new Owner(KEEPER, KEEPER), new Health(500, 1000),
                new Position(0, new Vector3f(2, 0, 2)), new Possessed(0, 0));

        keeper = new Keeper(false, KEEPER);
        PlayerManaControl manaControl = new PlayerManaControl(keeper,
                Map.of(MiscType.MAXIMUM_MANA_THRESHOLD, TrapFiringFixtures.miscVariable(MiscType.MAXIMUM_MANA_THRESHOLD, 200000)));
        keeper.setMana(1000);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> switch (name) {
            case "getKeeper" -> keeper;
            case "getManaControl" -> manaControl;
            default -> null;
        });

        ICreatureController creatureController = stub(ICreatureController.class, (name, args) -> {
            if ("setPossession".equals(name)) {
                possessionCalls.add((Boolean) args[0]);
            }
            return null;
        });
        ICreaturesController creaturesController = stub(ICreaturesController.class,
                (name, args) -> "createController".equals(name) ? creatureController : null);

        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> "getCreature".equals(name)
                ? TrapFiringFixtures.creature((Short) args[0], Creature.CreatureFlag.GENERATE_DEAD_BODY) : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> "getLevelData".equals(name) ? kwdFile : null);

        healthSystem = new HealthSystem(entityData, null, Map.of(
                MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, TrapFiringFixtures.miscVariable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 10),
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, TrapFiringFixtures.miscVariable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, 0),
                MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, TrapFiringFixtures.miscVariable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 0),
                MiscType.DEATH_WHEN_POSSESSING_MANA_REDUCTION, TrapFiringFixtures.miscVariable(MiscType.DEATH_WHEN_POSSESSING_MANA_REDUCTION, MANA_REDUCTION)),
                creaturesController, levelInfo, List.of(player), null, () -> 0.0, stub(GameEventService.class, (name, args) -> null));
        healthSystem.processTick(0.25f);
    }

    @Test
    void aHitThatWouldKnockItOutKillsIt() {
        // A possessed creature takes half of the damage dealt
        EntityController.setDamage(entityData, creature, 1200);
        healthSystem.processTick(0.25f);

        assertNotNull(entityData.getComponent(creature, Death.class));
        assertNull(entityData.getComponent(creature, Unconscious.class));
    }

    @Test
    void itsDeathEndsThePossessionAndCostsMana() {
        // A possessed creature takes half of the damage dealt
        EntityController.setDamage(entityData, creature, 1200);
        healthSystem.processTick(0.25f);

        assertEquals(List.of(false), possessionCalls);
        assertEquals(1000 - MANA_REDUCTION, keeper.getMana());
    }

    @Test
    void aHitItSurvivesChangesNothing() {
        // A possessed creature takes half of the damage dealt
        EntityController.setDamage(entityData, creature, 800);
        healthSystem.processTick(0.25f);

        assertEquals(100, entityData.getComponent(creature, Health.class).health);
        assertEquals(List.of(), possessionCalls);
        assertEquals(1000, keeper.getMana());
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }

}
