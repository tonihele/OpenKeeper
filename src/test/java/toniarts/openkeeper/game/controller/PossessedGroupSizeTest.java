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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The group the PLAYER_CREATURES_GROUPED trigger counts: the creatures
 * following the creature the player possesses.
 */
class PossessedGroupSizeTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final CreaturesController creaturesController = new CreaturesController(emptyLevel(), entityData,
            null, null, null, null, null);

    @Test
    void countsTheFollowersOfThePossessedCreature() {
        EntityId possessed = addCreature(PLAYER_ID, true);
        follow(addCreature(PLAYER_ID, false), possessed);
        follow(addCreature(PLAYER_ID, false), possessed);

        // Followers of a creature nobody possesses and the other keeper's group don't count
        follow(addCreature(PLAYER_ID, false), addCreature(PLAYER_ID, false));
        follow(addCreature(OTHER_PLAYER_ID, false), addCreature(OTHER_PLAYER_ID, true));

        assertEquals(2, creaturesController.getPossessedGroupSize(PLAYER_ID));
    }

    @Test
    void anEmptyGroupIsZero() {
        addCreature(PLAYER_ID, true);

        assertEquals(0, creaturesController.getPossessedGroupSize(PLAYER_ID));
    }

    @Test
    void noPossessionHasNoGroup() {
        follow(addCreature(PLAYER_ID, false), addCreature(PLAYER_ID, false));

        assertEquals(-1, creaturesController.getPossessedGroupSize(PLAYER_ID));
    }

    private EntityId addCreature(short ownerId, boolean possessed) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new Owner(ownerId, ownerId));
        if (possessed) {
            entityData.setComponent(entity, new Possessed(0, 0));
        }
        return entity;
    }

    private void follow(EntityId follower, EntityId leader) {
        entityData.setComponent(follower, new FollowTarget(leader));
    }

    private static IKwdFile emptyLevel() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getThings") ? List.of() : null);
    }
}
