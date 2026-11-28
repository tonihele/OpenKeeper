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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.SpellTargetFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Possession keeper spell can't take Jack, nor a second creature while the
 * keeper already possesses one (DKII.EXE 0x4AEEA0)
 */
class PossessionSpellTest {

    private static final short POSSESS_SHOT_ID = 32;
    private static final short CREATURE_ID = 5;
    private static final short JACK_ID = 46;
    private static final short CASTER = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> possessed = new ArrayList<>();
    private EntityId alreadyPossessed;
    private final ShotsController shotsController;

    PossessionSpellTest() {
        Shot shot = SpellTargetFixtures.shot(POSSESS_SHOT_ID, Shot.ProcessType.POSSESS_CREATURE);
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> "getShotById".equals(method) ? shot : null);
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "getPossessedCreature" -> alreadyPossessed;
            case "createController" -> proxy(ICreatureController.class, (creatureMethod, creatureArgs) -> {
                if ("setPossession".equals(creatureMethod)) {
                    possessed.add((EntityId) args[0]);
                }
                return null;
            });
            default -> null;
        });
        shotsController = new ShotsController(kwdFile, entityData, null, () -> 0, null, null, null, null, creaturesController, null);
    }

    @Test
    void possessesTheTarget() {
        EntityId target = creature(CREATURE_ID);

        shotsController.createShot(POSSESS_SHOT_ID, 0, 0, CASTER, new Vector3f(), target, null);

        assertEquals(List.of(target), possessed);
    }

    @Test
    void jackCannotBePossessed() {
        shotsController.createShot(POSSESS_SHOT_ID, 0, 0, CASTER, new Vector3f(), creature(JACK_ID), null);

        assertEquals(List.of(), possessed);
    }

    @Test
    void nothingIsPossessedWhileTheKeeperPossessesACreature() {
        alreadyPossessed = creature(CREATURE_ID);

        shotsController.createShot(POSSESS_SHOT_ID, 0, 0, CASTER, new Vector3f(), creature(CREATURE_ID), null);

        assertEquals(List.of(), possessed);
    }

    private EntityId creature(short creatureId) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(entity, creatureComponent);
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
