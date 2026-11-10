/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.modelviewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Effect;
import toniarts.openkeeper.tools.convert.map.EffectElement;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;

/** Read-only inspection of the definitions used by the current preview. */
public final class EffectInspector {
    /** Distinguishes linked effect definitions from particle definitions. */
    public enum Kind { EFFECT, PARTICLE }
    /** A navigable definition reference with the role shown in the inspector. */
    public record Link(Kind kind, int id, String label) { }
    /** Immutable inspector text and links for one selected definition. */
    public record Details(String title, List<String> fields, List<Link> links) {
        /** Copies collections so a rendered inspection cannot change underneath the UI. */
        public Details {
            fields = List.copyOf(fields);
            links = List.copyOf(links);
        }
    }

    private final IKwdFile data;

    /**
     * Creates an inspector over the currently loaded level definitions.
     *
     * @param data source of effect definitions and their creature, room and object references
     */
    public EffectInspector(IKwdFile data) {
        this.data = data;
    }

    /**
     * Describes a selected definition without changing preview playback.
     *
     * @param selection creature, room, object, effect, particle, or an unsupported selection
     * @return inspector text and links, with a selection prompt for unsupported values
     */
    public Details describe(Object selection) {
        List<String> fields = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        String title;
        if (selection instanceof Effect e) {
            title = describeEffect(e, fields, links);
        } else if (selection instanceof EffectElement e) {
            title = describeParticle(e, fields, links);
        } else if (selection instanceof Creature c) {
            title = "Creature: " + c.getName();
            fields.add("Entrance effect ID: " + c.getEntranceEffectId());
            effect(links, c.getEntranceEffectId(), "Entrance effect");
        } else if (selection instanceof Room r) {
            title = "Room: " + r.getName();
            fields.add("Room effect slots: " + r.getEffects());
            for (int id : r.getEffects()) effect(links, id, "Room effect");
            effect(links, r.getFightEffectId(), "Fight effect");
        } else if (selection instanceof GameObject o) {
            title = "Object: " + o.getName();
            effect(links, o.getMiscEffectId(), "Misc effect");
            effect(links, o.getSlapEffectId(), "Slap effect");
            effect(links, o.getDeathEffectId(), "Death effect");
        } else {
            title = "Effect details";
            fields.add("Select a creature, room, object or effect to inspect its effect definitions.");
        }
        if (links.isEmpty()) fields.add("No linked effects or particles.");
        return new Details(title, fields, links);
    }

    private String describeEffect(Effect e, List<String> fields, List<Link> links) {
        fields.add("Used by: " + references(data, e.getEffectId()));
        fields.add("Generator: " + e.getGenerationType());
        fields.add("Count per update: " + e.getElementsPerTurn());
        fields.add("Generator lifetime: " + life(e.getMinHp(), e.getMaxHp()));
        fields.add("Flags: " + e.getFlags());
        fields.add("Generated slots: " + e.getGenerateIds());
        fields.add("Circular path: " + e.getCircularPathRate() + "; whirlpool: " + e.getWhirlpoolRate());
        fields.add("Origin: " + e.getInnerOriginRange() + " to " + e.getOuterOriginRange()
                + "; height: " + e.getLowerHeightLimit() + " to " + e.getUpperHeightLimit());
        fields.add("Light fade duration field: " + e.getFadeDuration());
        for (int id : e.getGenerateIds()) {
            if (e.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECT_ELEMENTS)) particle(links, id, "Generated particle");
            else if (e.getFlags().contains(Effect.EffectFlag.GENERATE_EFFECTS)) effect(links, id, "Generated effect");
        }
        effect(links, e.getNextEffectId(), "Next effect");
        effect(links, e.getDeathEffectId(), "Death effect");
        effect(links, e.getHitSolidEffectId(), "Solid hit");
        effect(links, e.getHitWaterEffectId(), "Water hit");
        effect(links, e.getHitLavaEffectId(), "Lava hit");
        return "Effect " + e.getEffectId() + ": " + e.getName();
    }

    private String describeParticle(EffectElement e, List<String> fields, List<Link> links) {
        fields.add("Particle lifetime: " + life(e.getMinHp(), e.getMaxHp()));
        fields.add("Flags: " + e.getFlags());
        fields.add("Particle fade percentage field: " + e.getFadePercentage());
        fields.add("Horizontal speed: " + range(e.getMinSpeedXy(), e.getMaxSpeedXy()));
        fields.add("Vertical speed: " + range(e.getMinSpeedYz(), e.getMaxSpeedYz()));
        fields.add("Scale: " + range(e.getMinScale(), e.getMaxScale()));
        fields.add("RGB: " + e.getColor().getRed() + ", " + e.getColor().getGreen() + ", " + e.getColor().getBlue());
        ArtResource art = e.getArtResource();
        if (art != null) {
            fields.add("Texture / art: " + art.getName() + " (" + art.getType() + ")");
            if (art.getDataKeys().contains(ArtResource.KEY_FRAMES)) {
                fields.add("Frames: " + art.getData(ArtResource.KEY_FRAMES));
            }
            fields.add("Texture flags: " + art.getFlags());
            if (art.getFlags().contains(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE)) {
                fields.add("Texture animation: " + (art.getFlags().contains(ArtResource.ArtResourceFlag.DOESNT_LOOP)
                        ? "does not loop" : "loops"));
            }
        }
        effect(links, e.getNextEffectId(), "Next effect");
        particle(links, e.getDeathElementId(), "Death particle");
        particle(links, e.getHitSolidElementId(), "Solid hit");
        particle(links, e.getHitWaterElementId(), "Water hit");
        particle(links, e.getHitLavaElementId(), "Lava hit");
        return "Particle " + e.getEffectElementId() + ": " + e.getName();
    }

    private void effect(List<Link> links, int id, String role) {
        if (id == 0) return;
        Effect effect = data.getEffect(id);
        links.add(new Link(Kind.EFFECT, id, role + ": " + id + (effect == null ? " (missing)" : " - " + effect.getName())));
    }

    private void particle(List<Link> links, int id, String role) {
        if (id == 0) return;
        EffectElement element = data.getEffectElement(id);
        links.add(new Link(Kind.PARTICLE, id, role + ": " + id + (element == null ? " (missing)" : " - " + element.getName())));
    }

    /**
     * Finds authored creature, room and object references to an effect.
     *
     * @param data currently loaded definitions
     * @param id effect ID to find
     * @return comma-separated reference labels, or {@code None} when unused
     */
    public static String references(IKwdFile data, int id) {
        List<String> references = new ArrayList<>();
        for (Creature c : data.getCreatureList()) if (c.getEntranceEffectId() == id) references.add(c.getName() + " entrance");
        for (Room r : data.getRooms()) {
            if (r.getEffects().contains(id)) references.add(r.getName() + " room");
            if (r.getFightEffectId() == id) references.add(r.getName() + " fight");
        }
        for (GameObject o : data.getObjectList()) {
            if (o.getMiscEffectId() == id) references.add(o.getName() + " misc");
            if (o.getSlapEffectId() == id) references.add(o.getName() + " slap");
            if (o.getDeathEffectId() == id) references.add(o.getName() + " death");
        }
        return references.isEmpty() ? "None" : String.join(", ", references);
    }

    private static String life(int min, int max) {
        return range(min, max) + " steps (~" + range(min / 20f, max / 20f) + " s at 20 Hz)";
    }

    private static String range(float min, float max) {
        return min == max ? number(min) : number(min) + " to " + number(max);
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("\\.?0+$", "");
    }
}
