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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that a stunned creature that gets hit stays down longer
 */
class HealthSystemStunTest {

    // The fixture creature stands in for the level's Imp
    private static final Creature IMP = CreatureSleepFixtures.sleeper(1);
    private static final short KNIGHT_ID = 2;
    private static final short BILE_DEMON_ID = 3;

    private DefaultEntityData entityData;
    private HealthSystem healthSystem;
    private double gameTime;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
        for (MiscType type : List.of(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,
                MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
            gameSettings.put(type, CreatureSleepFixtures.variable(type, 0));
        }
        gameSettings.put(MiscType.STUN_TIME_PERCENTAGE_INCREASE_WHEN_HIT,
                CreatureSleepFixtures.variable(MiscType.STUN_TIME_PERCENTAGE_INCREASE_WHEN_HIT, 25));

        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> method.equals("getImp") ? IMP : null);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> method.equals("getLevelData") ? kwdFile : null);
        IGameTimer gameTimer = proxy(IGameTimer.class, (method, args) -> method.equals("getGameTime") ? gameTime : null);
        healthSystem = new HealthSystem(entityData, proxy(IEntityPositionLookup.class, (method, args) -> null),
                gameSettings, proxy(ICreaturesController.class, (method, args) -> null), levelInfo, List.of(),
                proxy(IMapController.class, (method, args) -> null), gameTimer,
                proxy(GameEventService.class, (method, args) -> null));
    }

    @Test
    void hitWhileStunnedAddsAQuarterOfTheStunLeft() {
        EntityId knight = stunnedKnight(0);
        attacker(BILE_DEMON_ID, knight);

        hitAt(knight, 2);

        // 2 seconds of the 4 second stun were left, a quarter of that is added
        assertEquals(0.5, entityData.getComponent(knight, CreatureAi.class).stateStartTime, 0.0001);
        assertEquals(CreatureState.FALLEN, entityData.getComponent(knight, CreatureAi.class).getCreatureState());
    }

    @Test
    void stunNeverGrowsPastAFullStun() {
        EntityId knight = stunnedKnight(0);
        attacker(BILE_DEMON_ID, knight);

        hitAt(knight, 0.4);

        assertEquals(0.4, entityData.getComponent(knight, CreatureAi.class).stateStartTime, 0.0001);
    }

    @Test
    void impsFightingDoNotKeepAStunnedCreatureDown() {
        EntityId knight = stunnedKnight(0);
        attacker(IMP.getCreatureId(), knight);

        hitAt(knight, 2);

        assertEquals(0, entityData.getComponent(knight, CreatureAi.class).stateStartTime, 0.0001);
    }

    @Test
    void hitWhenNotStunnedLeavesTheStateAlone() {
        EntityId knight = stunnedKnight(0);
        entityData.setComponent(knight, new CreatureAi(0, CreatureState.FIGHT, KNIGHT_ID));
        attacker(BILE_DEMON_ID, knight);

        hitAt(knight, 2);

        assertEquals(0, entityData.getComponent(knight, CreatureAi.class).stateStartTime, 0.0001);
    }

    private EntityId stunnedKnight(double fellAt) {
        EntityId knight = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = KNIGHT_ID;
        creatureComponent.stunDuration = 4;
        entityData.setComponents(knight, creatureComponent, new Health(100, 100),
                new CreatureAi(fellAt, CreatureState.FALLEN, KNIGHT_ID));
        return knight;
    }

    private void attacker(short creatureId, EntityId target) {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(entityData.createEntity(), creatureComponent, new AttackTarget(target));
    }

    private void hitAt(EntityId target, double time) {
        gameTime = time;
        entityData.setComponent(target, new Damage(10));
        healthSystem.processTick(0.25f);
    }

    private interface Handler {

        Object handle(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            case "toString" ->
                type.getSimpleName();
            default ->
                handler.handle(method.getName(), args);
        });
    }
}
