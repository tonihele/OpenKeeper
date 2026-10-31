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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Turncoat;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.SpellTargetFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Turncoat keeper spell: an enemy creature fights for the caster for the
 * spell's duration
 */
class TurncoatSpellTest {

    private static final short TURNCOAT_SHOT_ID = 13;
    private static final short CREATURE_ID = 5;
    private static final short IMMUNE_CREATURE_ID = 6;
    private static final short CASTER = 3;
    private static final short ENEMY = 4;
    private static final double NOW = 100;

    private EntityData entityData;
    private IKwdFile kwdFile;
    private ShotsController shotsController;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        Shot shot = SpellTargetFixtures.shot(TURNCOAT_SHOT_ID, Shot.ProcessType.TURNCOAT);
        Creature creature = SpellTargetFixtures.creature(CREATURE_ID);
        Creature immuneCreature = SpellTargetFixtures.creature(IMMUNE_CREATURE_ID, Creature.CreatureFlag.IS_IMMUNE_TO_TURNCOAT);
        kwdFile = proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getShotById" -> shot;
            case "getCreature" -> (short) args[0] == CREATURE_ID ? creature : immuneCreature;
            default -> null;
        });
        IGameTimer gameTimer = () -> NOW;
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> null);
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "isValidEntity" -> entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null;
            case "createController" -> {
                EntityId entity = (EntityId) args[0];
                Creature targetCreature = kwdFile.getCreature(entityData.getComponent(entity, CreatureComponent.class).creatureId);
                yield new CreatureController(entity, entityData, targetCreature, null, null, gameTimer, null, null, null, null, levelInfo, null, null, null);
            }
            default -> null;
        });
        shotsController = new ShotsController(kwdFile, entityData, null, gameTimer, null, null, null, null, creaturesController, null);
    }

    @Test
    void enemyCreatureFightsForTheCasterForTheSpellDuration() {
        EntityId target = creature(CREATURE_ID, ENEMY);

        shotsController.createShot(TURNCOAT_SHOT_ID, 25, 0, CASTER, new Vector3f(), target, null);

        Turncoat turncoat = entityData.getComponent(target, Turncoat.class);
        assertNotNull(turncoat);
        assertEquals(CASTER, turncoat.playerId);
        assertEquals(NOW + 25, turncoat.endTime, 0.0001);

        // The real owner doesn't change
        assertEquals(ENEMY, entityData.getComponent(target, Owner.class).ownerId);
    }

    @Test
    void immuneCreatureIsNotTurned() {
        EntityId target = creature(IMMUNE_CREATURE_ID, ENEMY);

        shotsController.createShot(TURNCOAT_SHOT_ID, 25, 0, CASTER, new Vector3f(), target, null);

        assertNull(entityData.getComponent(target, Turncoat.class));
    }

    @Test
    void onlyCreaturesCanBeTurned() {
        EntityId door = entityData.createEntity();
        entityData.setComponent(door, new Owner(ENEMY, ENEMY));

        shotsController.createShot(TURNCOAT_SHOT_ID, 25, 0, CASTER, new Vector3f(), door, null);
        shotsController.createShot(TURNCOAT_SHOT_ID, 25, 0, CASTER, new Vector3f(), null, null);

        assertNull(entityData.getComponent(door, Turncoat.class));
    }

    @Test
    void spellNeedsACreatureToTarget() {
        KeeperSpell turncoat = SpellTargetFixtures.keeperSpell(TURNCOAT_SHOT_ID, KeeperSpell.TargetRule.ENEMY_CREATURES, KeeperSpell.CastRule.OWN_LAND);
        EntityId enemyDoor = entityData.createEntity();
        entityData.setComponent(enemyDoor, new Owner(ENEMY, ENEMY));
        entityData.setComponent(enemyDoor, new DoorComponent());

        assertTrue(KeeperSpellCastValidator.checkTurncoatRule(turncoat, entityData, creature(CREATURE_ID, ENEMY)));
        assertFalse(KeeperSpellCastValidator.checkTurncoatRule(turncoat, entityData, enemyDoor));
        assertFalse(KeeperSpellCastValidator.checkTurncoatRule(turncoat, entityData, null));
    }

    @Test
    void otherSpellsDontNeedACreature() {
        KeeperSpell landSpell = SpellTargetFixtures.keeperSpell((short) 1, KeeperSpell.TargetRule.LAND, KeeperSpell.CastRule.OWN_LAND);

        assertTrue(KeeperSpellCastValidator.checkTurncoatRule(landSpell, entityData, null));
    }

    private EntityId creature(short creatureId, short ownerId) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(entity, creatureComponent);
        entityData.setComponent(entity, new Owner(ownerId, ownerId));
        return entity;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method.getName(), args));
    }

    @FunctionalInterface
    private interface Handler {

        Object invoke(String method, Object[] args);
    }

}
