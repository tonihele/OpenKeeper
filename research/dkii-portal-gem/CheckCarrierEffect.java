import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.scene.Node;
import com.simsilica.es.base.DefaultEntityData;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.view.control.CreatureViewControl;
import toniarts.openkeeper.view.effect.*;

/** Headless integration check using original Level 1 definitions and converted models. */
class CheckCarrierEffect {
    public static void main(String[] args) {
        var level = KwdFile.load("/home/wietse/Downloads/Data/Bullfrog/Dungeon Keeper 2/", "Level1");
        var assets = new DesktopAssetManager(true);
        assets.registerLocator("assets/Converted", FileLocator.class);
        var data = new DefaultEntityData();
        var id = data.createEntity();
        data.setComponents(id, new Owner((short) 3, (short) 3), new PortalGem());
        var model = new Node("Lord");
        var root = new Node("World");
        root.attachChild(model);
        var control = new CreatureViewControl(id, data, level.getCreature((short) 29),
                Creature.AnimationType.STAND_STILL, assets, null);
        control.setEffectManager(new EffectManagerState(level, assets));
        model.addControl(control);
        int visibleFrames = 0;
        int maxCount = 0;
        for (int frame = 0; frame < 360; frame++) {
            if (frame == 120) control.setTargetState(Creature.AnimationType.WALK);
            if (frame == 240) control.setTargetState(Creature.AnimationType.MELEE_ATTACK);
            model.setLocalTranslation(frame / 120f, 0, 0);
            root.updateLogicalState(1f / 60);
            root.updateGeometricState();
            control.render(null, null);
            var emitter = (PortalGemEffect) ((Node) model.getChild("Carried portal gem")).getChild(0);
            if (emitter.getNumVisibleParticles() > 0) visibleFrames++;
            maxCount = Math.max(maxCount, emitter.getNumVisibleParticles());
            if (emitter.getMaterial().getTextureParam("Texture") == null) throw new AssertionError("Missing original texture");
        }
        if (visibleFrames < 350 || maxCount < 10) throw new AssertionError("No sustained marker emission: " + visibleFrames + "/" + maxCount);
        data.setComponent(id, new Death(0));
        root.updateLogicalState(0.05f);
        if (model.getChild("Carried portal gem") != null) throw new AssertionError("Death did not clean up particles");
        control.cleanup();
        data.close();
        System.out.println("PASS: real static/walk/combat model markers, original texture, sustained world-space sparks, death cleanup; frames=" + visibleFrames + " peak=" + maxCount);
    }
}
