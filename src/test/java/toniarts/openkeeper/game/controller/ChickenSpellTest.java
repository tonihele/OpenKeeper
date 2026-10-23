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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Morphed;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.SpellTargetFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Chicken keeper spell: a creature turns into a chicken for the spell's
 * duration
 */
class ChickenSpellTest {

    private static final short CHICKEN_SHOT_ID = 15;
    private static final short CREATURE_ID = 5;
    private static final short CASTER = 3;
    private static final short ENEMY = 4;

    private EntityData entityData;
    private ShotsController shotsController;
    private List<String> chickenCalls;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        chickenCalls = new ArrayList<>();
        Shot shot = SpellTargetFixtures.shot(CHICKEN_SHOT_ID, Shot.ProcessType.CHICKEN);
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getShotById" -> shot;
            default -> null;
        });
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "isValidEntity" -> entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null;
            case "createController" -> {
                EntityId entity = (EntityId) args[0];
                yield proxy(ICreatureController.class, (creatureMethod, creatureArgs) -> {
                    if (creatureMethod.equals("turnIntoChicken")) {
                        chickenCalls.add(entity.getId() + ":" + creatureArgs[0]);
                    }
                    return null;
                });
            }
            default -> null;
        });
        shotsController = new ShotsController(kwdFile, entityData, null, () -> 100, null, null, null, null, creaturesController);
    }

    @Test
    void creatureTurnsIntoChickenForTheSpellDuration() {
        EntityId target = creature(CREATURE_ID, ENEMY);

        shotsController.createShot(CHICKEN_SHOT_ID, 60, 0, CASTER, new Vector3f(), target, null);

        assertEquals(List.of(target.getId() + ":60.0"), chickenCalls);
    }

    @Test
    void onlyCreaturesTurnIntoChickens() {
        EntityId door = entityData.createEntity();
        entityData.setComponent(door, new Owner(ENEMY, ENEMY));

        shotsController.createShot(CHICKEN_SHOT_ID, 60, 0, CASTER, new Vector3f(), door, null);
        shotsController.createShot(CHICKEN_SHOT_ID, 60, 0, CASTER, new Vector3f(), null, null);

        assertTrue(chickenCalls.isEmpty());
    }

    @Test
    void spellTargetsAnyCreatureButNeutralOnes() {
        KeeperSpell chicken = SpellTargetFixtures.keeperSpell(CHICKEN_SHOT_ID, KeeperSpell.TargetRule.ALL_CREATURES, KeeperSpell.CastRule.OWN_LAND);

        assertTrue(KeeperSpellCastValidator.checkChickenRule(creature(CREATURE_ID, ENEMY), entityData, chicken));
        assertTrue(KeeperSpellCastValidator.checkChickenRule(creature(CREATURE_ID, CASTER), entityData, chicken));
        assertFalse(KeeperSpellCastValidator.checkChickenRule(creature(CREATURE_ID, Player.NEUTRAL_PLAYER_ID), entityData, chicken));
    }

    @Test
    void chickenCantBePossessed() {
        KeeperSpell possession = SpellTargetFixtures.keeperSpell((short) 26, KeeperSpell.TargetRule.POSESSION, KeeperSpell.CastRule.OWN_LAND);
        EntityId ownCreature = creature(CREATURE_ID, CASTER);
        assertTrue(KeeperSpellCastValidator.checkChickenRule(ownCreature, entityData, possession));

        entityData.setComponent(ownCreature, new Morphed(160));

        assertFalse(KeeperSpellCastValidator.checkChickenRule(ownCreature, entityData, possession));
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
