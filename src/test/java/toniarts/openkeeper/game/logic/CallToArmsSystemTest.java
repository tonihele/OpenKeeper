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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CallToArms;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.PlayerSpell;
import toniarts.openkeeper.tools.convert.map.ChickenSpawnFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpellFixtures;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Call to Arms as in the original: who answers the banner and what it costs.
 * The spell values are the original's, 10000 to cast and 90 a second
 */
class CallToArmsSystemTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final short SPELL_ID = 3;
    private static final short SHOT_ID = 5;

    private final EntityData entityData = new DefaultEntityData();
    private final Keeper keeper = new Keeper(false, PLAYER_ID);
    private final List<EntityId> answered = new ArrayList<>();
    private double gameTime;
    private CallToArmsSystem system;

    @BeforeEach
    void setUp() {
        keeper.setMana(100_000);
        PlayerSpell spell = new PlayerSpell(SPELL_ID, true);
        keeper.getAvailableSpells().add(spell);

        IKwdFile kwdFile = stub(IKwdFile.class, name -> switch (name) {
            case "getKeeperSpells" -> List.of(KeeperSpellFixtures.keeperSpell(SPELL_ID, 10000, 90, SHOT_ID, ObjectsController.OBJECT_CALL_TO_ARMS_ID));
            case "getShotById" -> ShotFixtures.shot(SHOT_ID, Shot.ProcessType.CREATE_OBJECT, 1, EnumSet.noneOf(Shot.ShotProcessFlag.class));
            case "getVariables" -> Map.of(MiscType.UPGRADED_CALL_TO_ARMS_COST_PERCENTAGE,
                    ChickenSpawnFixtures.miscVariable(MiscType.UPGRADED_CALL_TO_ARMS_COST_PERCENTAGE, 70));
            default -> throw new UnsupportedOperationException(name);
        });
        IPlayerController playerController = stub(IPlayerController.class, name -> keeper);
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    EntityId creature = (EntityId) args[0];
                    return stub(ICreatureController.class, name -> answered.add(creature));
                });
        system = new CallToArmsSystem(entityData, kwdFile, List.of(playerController), creaturesController, () -> gameTime);
    }

    @Test
    void theKeepersFreeFightersAnswer() {
        EntityId idle = creature(PLAYER_ID, false, CreatureState.IDLE);
        EntityId working = creature(PLAYER_ID, false, CreatureState.WORK);
        EntityId sleeping = creature(PLAYER_ID, false, CreatureState.SLEEPING);
        creature(PLAYER_ID, true, CreatureState.IDLE); // Imps don't
        creature(ENEMY_ID, false, CreatureState.IDLE);
        creature(PLAYER_ID, false, CreatureState.FIGHT);
        creature(PLAYER_ID, false, CreatureState.ENTERING_DUNGEON);
        creature(PLAYER_ID, false, CreatureState.IMPRISONED);
        creature(PLAYER_ID, false, CreatureState.CALL_TO_ARMS);
        banner();

        system.processTick(0.25f);

        assertEquals(List.of(idle, working, sleeping), answered.stream().sorted().toList());
    }

    @Test
    void theBannerCallsEverySecond() {
        creature(PLAYER_ID, false, CreatureState.IDLE);
        banner();

        system.processTick(0.25f);
        gameTime = 0.5;
        system.processTick(0.25f);
        assertEquals(1, answered.size());

        gameTime = 1;
        system.processTick(0.25f);
        assertEquals(2, answered.size());
    }

    @Test
    void theCastingCostPaysForTheFirst111Seconds() {
        EntityId banner = banner();
        system.processTick(0.25f);

        gameTime = 110.75;
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, Mana.class));

        gameTime = 111;
        system.processTick(0.25f);
        assertEquals(-90, entityData.getComponent(banner, Mana.class).manaGeneration);
    }

    @Test
    void theUpgradedBannerCosts70Percent() {
        keeper.getAvailableSpells().get(0).setUpgraded(true);
        EntityId banner = banner();
        system.processTick(0.25f);

        gameTime = 111;
        system.processTick(0.25f);
        assertEquals(-63, entityData.getComponent(banner, Mana.class).manaGeneration);
    }

    @Test
    void theBannerInTheHandCostsNothing() {
        EntityId banner = banner();
        system.processTick(0.25f);
        gameTime = 111;
        system.processTick(0.25f);

        entityData.removeComponent(banner, Position.class);
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, Mana.class));
    }

    @Test
    void theBannerFallsWhenTheManaRunsOut() {
        EntityId banner = banner();
        system.processTick(0.25f);

        keeper.setMana(89);
        gameTime = 111;
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, ObjectComponent.class));
    }

    @Test
    void aSlapTakesTheBannerDown() {
        EntityId banner = banner();
        system.processTick(0.25f);

        entityData.setComponent(banner, new Slapped(gameTime));
        system.processTick(0.25f);
        assertNull(entityData.getComponent(banner, ObjectComponent.class));
    }

    @Test
    void castingAgainMovesTheBannerWithoutResettingTheUpkeep() {
        EntityId first = banner();
        system.processTick(0.25f);

        gameTime = 50;
        EntityId second = banner();
        system.processTick(0.25f);

        assertNull(entityData.getComponent(first, ObjectComponent.class));
        assertEquals(111, entityData.getComponent(second, CallToArms.class).upkeepTime);
    }

    @Test
    void theBannerInTheHandStaysWhenCastingAgain() {
        EntityId first = banner();
        system.processTick(0.25f);
        entityData.removeComponent(first, Position.class);
        entityData.setComponent(first, new InHand());

        EntityId second = banner();
        system.processTick(0.25f);

        assertNotNull(entityData.getComponent(first, CallToArms.class));
        assertNull(entityData.getComponent(second, ObjectComponent.class));
    }

    private EntityId banner() {
        EntityId banner = entityData.createEntity();
        entityData.setComponents(banner, new ObjectComponent(ObjectsController.OBJECT_CALL_TO_ARMS_ID, null),
                new Owner(PLAYER_ID, PLAYER_ID), new Position(0, new Vector3f(5, 0, 5)));
        return banner;
    }

    private EntityId creature(short ownerId, boolean worker, CreatureState state) {
        EntityId creature = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.worker = worker;
        entityData.setComponents(creature, creatureComponent, new Owner(ownerId, ownerId),
                new CreatureAi(0, state, (short) 1));
        return creature;
    }

    private static <T> T stub(Class<T> type, Function<String, Object> answer) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.apply(method.getName())));
    }

}
