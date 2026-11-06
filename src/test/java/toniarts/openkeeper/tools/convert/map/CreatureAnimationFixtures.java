package toniarts.openkeeper.tools.convert.map;

/** Parser-owned animation setters used by renderer tests. */
public final class CreatureAnimationFixtures {
    private CreatureAnimationFixtures() {
    }

    public static Creature creature(ArtResource walk, ArtResource run) {
        Creature creature = new Creature();
        creature.setAnimation(Creature.AnimationType.WALK, walk);
        if (run != null) {
            creature.setAnimation(Creature.AnimationType.RUN, run);
        }
        return creature;
    }
}
