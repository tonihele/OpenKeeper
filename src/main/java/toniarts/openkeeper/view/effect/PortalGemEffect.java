package toniarts.openkeeper.view.effect;

import com.jme3.effect.Particle;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.influencers.DefaultParticleInfluencer;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;
import toniarts.openkeeper.utils.AssetUtils;

/** Native marker-driven effect 257; particles outlive and trail the carrier. */
public final class PortalGemEffect extends EffectParticleEmitter {
    public static final int EFFECT_ID = 257;
    private static final float STEPS_PER_SECOND = 20;
    private final Effect effect;
    private final EffectElement element;
    private final Map<Particle, Appearance> appearances = new IdentityHashMap<>();
    private final List<Generator> generators = new ArrayList<>();
    private final Vector3f origin = new Vector3f();
    private float stepRemainder;

    private static final class Generator {
        final Vector3f position;
        int life;
        Generator(Vector3f position, int life) {
            this.position = position.clone();
            this.life = life;
        }
    }

    private static final class Appearance {
        int red, green, blue, step;
        final int frame;
        final float size;
        Appearance(EffectElement element, int frame, float size) {
            red = element.getColor().getRed();
            green = element.getColor().getGreen();
            blue = element.getColor().getBlue();
            this.frame = frame;
            this.size = size;
        }
    }

    public PortalGemEffect(EffectManagerState manager) {
        this(manager, manager.getKwdFile().getEffect(EFFECT_ID));
    }

    private PortalGemEffect(EffectManagerState manager, Effect effect) {
        this(manager, effect, manager.getKwdFile().getEffectElement(effect.getGenerateIds().getFirst()));
    }

    private PortalGemEffect(EffectManagerState manager, Effect effect, EffectElement element) {
        // A particle pool bounds allocation; it does not determine emission density.
        super("Carried portal gem", ParticleMesh.Type.Triangle, 2048,
                element.getAirFriction(), element.getElasticity(), false, effect.getWhirlpoolRate());
        this.effect = effect;
        this.element = element;
        var material = AssetUtils.createParticleMaterial(element.getArtResource(), manager.getAssetManger());
        material.getAdditionalRenderState().setBlendMode(BlendMode.Additive);
        material.getAdditionalRenderState().setDepthTest(true);
        material.getAdditionalRenderState().setDepthWrite(false);
        setMaterial(material);
        setImagesX(Math.max(1, element.getArtResource().getData(ArtResource.KEY_FRAMES)));
        setImagesY(1);
        setSelectRandomImage(true);
        setInWorldSpace(true);
        setParticlesPerSec(0);
        setLowLife(element.getMinHp() / STEPS_PER_SECOND);
        setHighLife(element.getMaxHp() / STEPS_PER_SECOND);
        setStartColor(new ColorRGBA(element.getColor().getRed() / 255f,
                element.getColor().getGreen() / 255f, element.getColor().getBlue() / 255f, 1));
        setEndColor(getStartColor());
        setStartSize(element.getMinScale());
        setEndSize(element.getMinScale());
        setRandomAngle(true);
        setRotateSpeed(EffectControl.randomSpinRate(effect.getSpriteSpinRateRange()));
        setGravity(0, element.getMass() * 25, 0);
        setParticleInfluencer(new DefaultParticleInfluencer() {
            @Override
            public void influenceParticle(Particle particle, EmitterShape shape) {
                particle.position.set(EffectControl.randomOriginOffset(effect)).addLocal(origin);
                float angle = FastMath.nextRandomFloat() * FastMath.TWO_PI;
                float horizontal = random(element.getMinSpeedXy(), element.getMaxSpeedXy()) * 1.25f;
                particle.velocity.set(-FastMath.sin(angle) * horizontal,
                        random(element.getMinSpeedYz(), element.getMaxSpeedYz()) * 1.25f,
                        FastMath.cos(angle) * horizontal);
                particle.life = particle.startlife = FastMath.nextRandomInt(element.getMinHp(), element.getMaxHp()) / STEPS_PER_SECOND;
                var appearance = new Appearance(element, FastMath.nextRandomInt(0, getImagesX() - 1),
                        random(element.getMinScale(), element.getMaxScale()));
                appearances.put(particle, appearance);
                particle.size = appearance.size;
                particle.imageIndex = appearance.frame;
            }
        });
    }

    private static float random(float min, float max) {
        return min + FastMath.nextRandomFloat() * (max - min);
    }

    /** Called once for each visible marker during model submission, like DKII. */
    public void request(Vector3f worldPosition) {
        generators.add(new Generator(worldPosition, FastMath.nextRandomInt(effect.getMinHp(), effect.getMaxHp())));
    }

    /** Generation occurs before the parent effect's lifetime is decremented. */
    public void advance(float tpf) {
        stepRemainder += tpf * STEPS_PER_SECOND;
        int steps = (int) stepRemainder;
        stepRemainder -= steps;
        for (var iterator = generators.iterator(); iterator.hasNext();) {
            Generator generator = iterator.next();
            if (generator.life > 0) {
                getWorldTransform().transformInverseVector(generator.position, origin);
                emitParticles(scaledCount(effect.getElementsPerTurn(), generationScale(tpf)));
            }
            generator.life -= steps;
            if (generator.life <= 0) {
                iterator.remove();
            }
        }
    }

    /** Native 0x43B28B: generation quality rises with the measured frame rate. */
    static int generationScale(float tpf) {
        // 0x57573B measures frame duration in integer milliseconds.
        int millis = (int) (tpf * 1000);
        int fps = 1000 / Math.max(1, millis);
        return Math.clamp(50 + fps * 205 / 14, 50, 255);
    }

    static int scaledCount(int count, int scale) {
        return count == 0 ? 0 : Math.max(1, count * scale / 255);
    }

    @Override
    protected void updateParticle(Particle particle, float tpf, Vector3f min, Vector3f max) {
        super.updateParticle(particle, tpf, min, max);
        Appearance appearance = appearances.get(particle);
        int step = (int) ((particle.startlife - particle.life) * STEPS_PER_SECOND + 0.0001f);
        if (step > appearance.step) {
            // Native 0x4364DD: one integer recurrence per update, even when
            // multiple effect steps were skipped. Fade affects RGB, not alpha.
            int life = Math.max(1, Math.round(particle.life * STEPS_PER_SECOND));
            if (element.getFlags().contains(EffectElement.EffectElementFlag.FADE)
                    && life <= Math.max(appearance.red, Math.max(appearance.green, appearance.blue))) {
                appearance.red = appearance.red * (life - 1) / life;
                appearance.green = appearance.green * (life - 1) / life;
                appearance.blue = appearance.blue * (life - 1) / life;
            }
            appearance.step = step;
        }
        particle.color.set(appearance.red / 255f, appearance.green / 255f, appearance.blue / 255f, 1);
        particle.size = appearance.size;
        particle.imageIndex = (appearance.frame + step) % getImagesX();
    }

    public void clear() {
        generators.clear();
        killAllParticles();
        appearances.clear();
    }
}
