package toniarts.openkeeper.view.effect;

import com.jme3.effect.Particle;
import com.jme3.math.FastMath;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;
import toniarts.openkeeper.utils.Color;
import static org.junit.jupiter.api.Assertions.*;

class ChosenCreatureEmitterTest {
    static void set(Object o, String field, Object value) throws Exception {
        var f = o.getClass().getDeclaredField(field); f.setAccessible(true); f.set(o, value);
    }
    static ChosenCreatureEmitter emitter() throws Exception {
        Effect effect = new Effect();
        set(effect,"minHp",4);set(effect,"maxHp",4);set(effect,"elementsPerTurn",(short)2);
        EffectElement element = new EffectElement();
        set(element,"name","Synthetic rising sparks");set(element,"minHp",7);set(element,"maxHp",7);
        set(element,"minSpeedXy",1f);set(element,"maxSpeedXy",1f);set(element,"mass",-.125f);
        set(element,"airFriction",.0125f);set(element,"minScale",.125f);
        set(element,"flags",EnumSet.of(EffectElement.EffectElementFlag.FADE));
        set(element,"color",new Color(90,60,10));
        return new ChosenCreatureEmitter(effect,element);
    }
    @Test void startsAtTheOriginThenRisesAndDarkens() throws Exception {
        var emitter=emitter();emitter.emitAllParticles();
        Particle p=emitter.getParticles()[0];
        assertEquals(0,p.position.lengthSquared());assertEquals(0,p.velocity.y);
        float horizontal=p.velocity.x*p.velocity.x+p.velocity.z*p.velocity.z;
        float red=p.color.r;
        emitter.updateFromControl(.05f);
        assertTrue(p.position.y>0);assertTrue(p.velocity.y>0);
        assertTrue(p.velocity.x*p.velocity.x+p.velocity.z*p.velocity.z<horizontal);
        assertTrue(p.color.r<red);assertEquals(1,p.color.a);
    }
    @Test void emitsFiniteOverlappingPacketsAndCleansUp() throws Exception {
        var emitter=emitter();emitter.emitAllParticles();
        assertEquals(2,emitter.getNumVisibleParticles());
        emitter.updateFromControl(.15f);assertEquals(8,emitter.getNumVisibleParticles());
        emitter.updateFromControl(.05f);assertEquals(8,emitter.getNumVisibleParticles());
        emitter.updateFromControl(1);assertEquals(0,emitter.getNumVisibleParticles());
    }
    @Test void frameRateDoesNotChangeTheTrajectoryOrPacketCount() throws Exception {
        FastMath.rand.setSeed(901);var a=emitter();a.emitAllParticles();
        for(int i=0;i<12;i++)a.updateFromControl(.01f);
        FastMath.rand.setSeed(901);var b=emitter();b.emitAllParticles();b.updateFromControl(.12f);
        assertEquals(a.getNumVisibleParticles(),b.getNumVisibleParticles());
        for(int i=0;i<a.getParticles().length;i++) {
            assertEquals(a.getParticles()[i].position,b.getParticles()[i].position);
            assertEquals(a.getParticles()[i].velocity,b.getParticles()[i].velocity);
        }
    }
}
