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
package toniarts.openkeeper.game.controller.creature;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CallToArms;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * While its keeper has a Call to Arms banner up, a creature only starts a
 * fight near that banner
 */
class CreatureCallToArmsFightTest {

    private static final short KEEPER = 3;
    private static final short ENEMY_KEEPER = 4;
    private static final int FIGHT_DISTANCE_TILES = 5;

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId creature = entityData.createEntity();
    private final CreatureController controller = controller();

    @Test
    void startsAFightWithoutABanner() {
        assertTrue(controller.shouldFleeOrAttack());
        assertEquals(CreatureState.FIGHT, controller.getStateMachine().getCurrentState());
    }

    @Test
    void startsNoFightFarFromItsKeepersBanner() {
        banner(KEEPER, FIGHT_DISTANCE_TILES + 2, CallToArms.UP);

        assertFalse(controller.shouldFleeOrAttack());
    }

    @Test
    void startsAFightNearItsKeepersBanner() {
        banner(KEEPER, FIGHT_DISTANCE_TILES - 1, CallToArms.UP);

        assertTrue(controller.shouldFleeOrAttack());
    }

    @Test
    void anotherKeepersBannerDoesNotHoldItBack() {
        banner(ENEMY_KEEPER, FIGHT_DISTANCE_TILES + 2, CallToArms.UP);

        assertTrue(controller.shouldFleeOrAttack());
    }

    @Test
    void aBannerGoingDownOrInTheHandDoesNotHoldItBack() {
        banner(KEEPER, FIGHT_DISTANCE_TILES + 2, 10);
        entityData.removeComponent(banner(KEEPER, FIGHT_DISTANCE_TILES + 2, CallToArms.UP), Position.class);

        assertTrue(controller.shouldFleeOrAttack());
    }

    private EntityId banner(short ownerId, int tilesAway, double removeTime) {
        EntityId banner = entityData.createEntity();
        entityData.setComponents(banner, new CallToArms(0, 0, 0, 0, removeTime), new Owner(ownerId, ownerId),
                new Position(0, new Vector3f((1 + tilesAway) * WorldUtils.TILE_WIDTH, WorldUtils.FLOOR_HEIGHT, WorldUtils.TILE_WIDTH)));
        return banner;
    }

    private CreatureController controller() {
        EntityId enemy = entityData.createEntity();
        entityData.setComponents(enemy, new Health(100, 100), new Owner(ENEMY_KEEPER, ENEMY_KEEPER));
        entityData.setComponents(creature, new CreatureComponent(), new Health(100, 100), new Owner(KEEPER, KEEPER),
                new Position(0, new Vector3f(WorldUtils.TILE_WIDTH, WorldUtils.FLOOR_HEIGHT, WorldUtils.TILE_WIDTH)),
                new Fearless(), new AttackTarget(enemy));

        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.CALL_TO_ARMS_FIGHT_DISTANCE_TILES,
                CreatureSleepFixtures.variable(MiscType.CALL_TO_ARMS_FIGHT_DISTANCE_TILES, FIGHT_DISTANCE_TILES));
        ICreaturesController creaturesController = proxy(ICreaturesController.class, "createController",
                proxy(ICreatureController.class, null, null));
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, "getSensedEntities", Set.of());
        return new CreatureController(creature, entityData, CreatureSleepFixtures.sleeper(9), null, null, () -> 0,
                settings, creaturesController, lookup, null, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, String answered, Object answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (object, method, args) -> {
            if (method.getName().equals(answered)) {
                return answer;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

}
