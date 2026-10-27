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
import java.util.EnumSet;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.PossessionFixtures;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A possessed creature uses its first person abilities as its keeper presses
 * their keys
 *
 * @author OpenKeeper
 */
class PossessedAbilityTest {

    private static final short KEEPER = Player.KEEPER1_ID;

    private EntityData entityData;
    private Creature creature;
    private double gameTime;
    private EntityId us;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        gameTime = 10;

        // A Vampire, it doesn't fly on its own
        creature = ShotFixtures.creature((short) 8, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        PossessionFixtures.setAbilities(creature, Creature.SpecialAbility.TURN_TO_BAT, Creature.SpecialAbility.HYPNOTISE);
        us = createCreature(KEEPER, new Vector3f(10, 0, 2));
        entityData.setComponents(us, new Possessed(0, 0), new Mobile(false, false, false, 1));
    }

    @Test
    void vampireTurnsIntoABatThatFliesAndBack() {
        CreatureController controller = createController();

        controller.usePossessedAbility(1);

        assertEquals(Creature.SpecialAbility.TURN_TO_BAT, entityData.getComponent(us, PossessedAbility.class).ability);
        assertTrue(entityData.getComponent(us, Mobile.class).canFly);

        controller.usePossessedAbility(1);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
        assertFalse(entityData.getComponent(us, Mobile.class).canFly);
    }

    @Test
    void secondAbilityDoesNotTurnIntoABat() {
        createController().usePossessedAbility(2);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
    }

    @Test
    void batTurnsBackWhenThePossessionEnds() {
        CreatureController controller = createController();
        controller.usePossessedAbility(1);

        controller.setPossession(false);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
        assertFalse(entityData.getComponent(us, Mobile.class).canFly);
    }

    private EntityId createCreature(short ownerId, Vector3f position) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new CreatureComponent(), new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(0, position), new CreatureMeleeAttack(0, 10, 1, 0.5f));
        return entity;
    }

    private CreatureController createController() {
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? gameTime : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class, (name, args) -> null);
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> null);
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> "getCreatureFirstPersonStats".equals(name) ? new HashMap<>() : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> "getLevelData".equals(name) ? kwdFile : null);

        return new CreatureController(us, entityData, creature, null, null, gameTimer, new HashMap<>(), creaturesController,
                positionLookup, null, levelInfo, null, null, null);
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
