/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.game.sound;

import com.jme3.asset.*;
import com.jme3.scene.*;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.view.control.CreatureViewControl;
import static org.junit.jupiter.api.Assertions.*;

class FailedAnimationAudioTest {
    @Test void hidingTheCreatureReleasesItsOwnedDigVoiceIncludingAnInheritedCull() throws Exception {
        for (boolean ancestorHidden : new boolean[]{false,true}) {
            List<AudioBackend.Status[]> voices = new ArrayList<>();
            initRecordingBackend(voices);
            var data = new DefaultEntityData(); var id = data.createEntity();
            var assets = new DesktopAssetManager(true) {
                @Override public Spatial loadModel(ModelKey key) { Node wrapper = new Node(); wrapper.attachChild(new Node()); return wrapper; }
            };
            var creature = new Creature();
            creature.getAnimations().put(Creature.AnimationType.DIG,new ArtResource("hidden-audio-dig",ArtResource.ArtResourceType.MESH));
            Node parent = new Node(), root = new Node(); parent.attachChild(root);
            var control = new CreatureViewControl(id,data,creature,Creature.AnimationType.DIG,assets,null); root.addControl(control);
            try {
                var category = loopingCategory();
                var engine = SoundManager.getEngine(); int handle = engine.play(category,1); engine.tick();
                var field = CreatureViewControl.class.getDeclaredField("digSound"); field.setAccessible(true); field.setInt(control,handle);
                assertTrue(engine.isPlaying(handle));
                (ancestorHidden ? parent : root).setCullHint(Spatial.CullHint.Always);
                parent.updateLogicalState(.1f); engine.tick();
                assertFalse(engine.isPlaying(handle),"hidden ancestor="+ancestorHidden);
                assertEquals(SoundEngine.NO_HANDLE,field.getInt(control));
                assertEquals(1,voices.size()); assertEquals(AudioBackend.Status.STOPPED,voices.get(0)[0]);
            } finally { control.cleanup(); data.close(); SoundManager.shutdown(); }
        }
    }
    @Test void failedDeathStopsOwnedResearchAndDigVoicesAndCannotScheduleRetries() throws Exception {
        List<AudioBackend.Status[]> voices = new ArrayList<>();
        initRecordingBackend(voices);
        var data = new DefaultEntityData(); var id = data.createEntity();
        var assets = new DesktopAssetManager(true) {
            @Override public Spatial loadModel(ModelKey key) { Node wrapper = new Node(); wrapper.attachChild(new Node()); return wrapper; }
        };
        var creature = new Creature();
        creature.getAnimations().put(Creature.AnimationType.RESEARCHING,new ArtResource("failed-audio-work",ArtResource.ArtResourceType.MESH));
        Node root = new Node(); var control = new CreatureViewControl(id,data,creature,Creature.AnimationType.RESEARCHING,assets,null); root.addControl(control);
        try {
            var category = loopingCategory();
            var engine = SoundManager.getEngine();
            int research = engine.play(category,1), dig = engine.play(category,1); engine.tick();
            for (String field : new String[]{"researchSound","digSound"}) {
                var target = CreatureViewControl.class.getDeclaredField(field); target.setAccessible(true);
                target.setInt(control,field.equals("researchSound") ? research : dig);
            }
            assertTrue(engine.isPlaying(research)); assertTrue(engine.isPlaying(dig));
            assertFalse(control.setViewState(creature,new CreatureViewState((short)1,0,Creature.AnimationType.DEATH_POSE,1)));
            assertFalse(engine.isPlaying(research)); assertFalse(engine.isPlaying(dig));
            root.updateLogicalState(1); engine.tick();
            assertEquals(2,voices.size()); assertTrue(voices.stream().allMatch(s->s[0] == AudioBackend.Status.STOPPED));
        } finally { control.cleanup(); data.close(); SoundManager.shutdown(); }
    }

    private static SoundCategory loopingCategory() {
        var archive = new SoundArchive("Review",EnumSet.noneOf(toniarts.openkeeper.tools.convert.sound.BankMapFileEntry.BankFlag.class),null,List.of("sound.mp2"));
        var group = new SoundEventGroup(100,100,0,0,0,0,0,0,0,0,100,0,List.of(new SoundSample(1,100,1000,0,archive)),List.of());
        return new SoundCategory("REVIEW",1,Map.of(1,new SoundEvent(1,100,EnumSet.of(SoundEventFlag.REPEATING,SoundEventFlag.LOOP_EACH),List.of(group))));
    }

    private static void initRecordingBackend(List<AudioBackend.Status[]> voices) {
        SoundManager.init(spec -> {
            AudioBackend.Status[] status = {AudioBackend.Status.STOPPED}; voices.add(status);
            return (AudioBackend)Proxy.newProxyInstance(FailedAnimationAudioTest.class.getClassLoader(),new Class<?>[]{AudioBackend.class},(p,m,a)-> {
                switch (m.getName()) {
                    case "getStatus": return status[0];
                    case "play": status[0] = AudioBackend.Status.PLAYING; break;
                    case "stop": status[0] = AudioBackend.Status.STOPPED; break;
                    case "pause": status[0] = AudioBackend.Status.PAUSED; break;
                    case "getDuration": return 1f;
                    case "getPlaybackTime": return 0f;
                }
                return null;
            });
        });
    }
}
