/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view;

import com.jme3.app.state.AppStateManager;
import com.jme3.asset.*;
import com.jme3.scene.*;
import com.simsilica.es.Entity;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.view.control.CreatureViewControl;
import toniarts.openkeeper.view.effect.*;
import static org.junit.jupiter.api.Assertions.*;

class FailedCreatureDeathTest {
    @Test void unavailableDeathArtStillPresentsExactlyOneDeathAcrossPositionUpdates() throws Exception {
        var assets = new DesktopAssetManager(true) {
            @Override public Spatial loadModel(ModelKey key) { Node wrapper = new Node(); wrapper.attachChild(new Node()); return wrapper; }
        };
        var creature = new Creature();
        creature.getAnimations().put(Creature.AnimationType.IDLE_1,new ArtResource("failed-death-idle",ArtResource.ArtResourceType.MESH));
        var level = (IKwdFile)Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),new Class<?>[]{IKwdFile.class},
                (p,m,a)->m.getName().equals("getCreature") ? creature : null);
        var data = new DefaultEntityData(); var id = data.createEntity();
        data.setComponents(id,new Position(0,0,0,0),new Owner((short)1,(short)1));
        Node owner = new Node(); owner.setCullHint(Spatial.CullHint.Always);
        owner.setUserData("charred",true); // Isolate scheduling from the installed charred texture.
        var control = new CreatureViewControl(id,data,creature,Creature.AnimationType.IDLE_1,assets,null); owner.addControl(control);
        var states = new AppStateManager(null); int[] plays = {0};
        states.attach(new EffectManagerState(level,assets) {
            @Override public VisualEffect load(Node root,com.jme3.math.Vector3f position,int effect,boolean infinite,short player) { plays[0]++; return null; }
        });
        var fog = (toniarts.openkeeper.view.fogofwar.IFogOfWarInformation)Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{toniarts.openkeeper.view.fogofwar.IFogOfWarInformation.class},(p,m,a)->true);
        var view = new PlayerEntityViewState(level,assets,data,(short)1,null,new Node(),null,fog);
        var manager = PlayerEntityViewState.class.getDeclaredField("stateManager"); manager.setAccessible(true); manager.set(view,states);
        var update = PlayerEntityViewState.class.getDeclaredMethod("updateCreatureModelAnimation",Spatial.class,Entity.class); update.setAccessible(true);
        try {
            data.setComponent(id,new CreatureViewState((short)1,1,Creature.AnimationType.DEATH_POSE,1));
            for (int i=0;i<3;i++) {
                data.setComponent(id,new Position(0,i,0,0));
                update.invoke(view,owner,data.getEntity(id,Position.class,CreatureViewState.class));
            }
            assertEquals(1,plays[0]);
            assertEquals(Creature.AnimationType.IDLE_1,control.getCurrentState());
            assertEquals(Creature.AnimationType.DEATH_POSE,control.getRequestedState());
            // A new actual logical death, after returning to life, is presented again.
            data.setComponent(id,new CreatureViewState((short)1,2,Creature.AnimationType.IDLE_1,2));
            update.invoke(view,owner,data.getEntity(id,Position.class,CreatureViewState.class));
            data.setComponent(id,new CreatureViewState((short)1,3,Creature.AnimationType.DEATH_POSE,3));
            update.invoke(view,owner,data.getEntity(id,Position.class,CreatureViewState.class));
            assertEquals(2,plays[0]);
        } finally {
            control.cleanup();
            var clock = PlayerEntityViewState.class.getDeclaredField("presentationClock"); clock.setAccessible(true);
            ((toniarts.openkeeper.view.animation.PresentationClock)clock.get(view)).close();
            data.close();
        }
    }
}
