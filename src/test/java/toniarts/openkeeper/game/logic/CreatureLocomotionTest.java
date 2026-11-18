/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatureLocomotionTest {
    @Test void possessionUsesActualMovementAndAvailableGaitWithWalkFallback() {
        for (byte mode : new byte[]{PossessedMovement.SPEED_WALK,PossessedMovement.SPEED_RUN,PossessedMovement.SPEED_CREEP}) {
            for(boolean available : new boolean[]{false,true}) {
                var desired = mode == PossessedMovement.SPEED_RUN ? Creature.AnimationType.RUN
                        : mode == PossessedMovement.SPEED_CREEP ? Creature.AnimationType.STEALTH_WALK : Creature.AnimationType.WALK;
                Creature creature = CreatureFixtures.creatureWithAnimation(desired,available
                        ? ArtResource.ArtResourceType.ANIMATING_MESH : ArtResource.ArtResourceType.NONE);
                IKwdFile catalog = (IKwdFile)Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),new Class<?>[]{IKwdFile.class},
                        (proxy,method,args)->method.getName().equals("getCreature") ? creature : null);
                var data = new DefaultEntityData(); var id = data.createEntity();
                data.setComponents(id,new Position(0,new Vector3f()),new Possessed(),
                        new PossessedMovement(new Vector2f(1,0),0,1,mode),
                        new CreatureViewState((short)1,0,Creature.AnimationType.STAND_STILL));
                var system = new CreatureViewSystem(data,catalog);
                system.processTick(.25f);
                assertEquals(Creature.AnimationType.STAND_STILL,data.getComponent(id,CreatureViewState.class).state);
                data.setComponent(id,new Position(0,.1f,0,0)); system.processTick(.25f);
                assertEquals(available ? desired : Creature.AnimationType.WALK,data.getComponent(id,CreatureViewState.class).state);
                system.processTick(.25f);
                assertEquals(Creature.AnimationType.STAND_STILL,data.getComponent(id,CreatureViewState.class).state);
                data.setComponent(id,new Death()); system.processTick(.25f);
                assertEquals(Creature.AnimationType.DEATH_POSE,data.getComponent(id,CreatureViewState.class).state);
                system.stop();
            }
        }
    }
    @Test void stationaryNavigationAndStaleNavigationOnDeathCannotForceWalking() {
        var data = new DefaultEntityData(); var id = data.createEntity();
        data.setComponents(id,new Position(0,new Vector3f()),new Navigation(),new CreatureViewState((short)1,0,Creature.AnimationType.WALK));
        var system = new CreatureViewSystem(data,null); system.processTick(.25f);
        assertEquals(Creature.AnimationType.STAND_STILL,data.getComponent(id,CreatureViewState.class).state);
        data.setComponent(id,new Death()); system.processTick(.25f);
        assertEquals(Creature.AnimationType.DEATH_POSE,data.getComponent(id,CreatureViewState.class).state);
        system.stop();
    }
}
