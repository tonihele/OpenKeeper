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

import com.jme3.math.Vector2f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.lang.System.Logger;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Shot;

/**
 * Summons the Horned Reaper, what the Summon Reaper shot does. The Reaper
 * appears where the spell was cast. The shot data tells how long he stays: 0
 * means he stays as long as the keeper pays for him, as with the Summon Reaper
 * spell. Otherwise he stays for that many seconds for free, as with the Reaper
 * Talisman.
 */
public final class ReaperSummoner {

    /**
     * The Reaper of the Summon Reaper spell is always this strong
     */
    public static final int SPELL_REAPER_LEVEL = 10;

    private static final Logger logger = System.getLogger(ReaperSummoner.class.getName());

    private final IKwdFile kwdFile;
    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final ICreaturesController creaturesController;

    public ReaperSummoner(IKwdFile kwdFile, EntityData entityData, IGameTimer gameTimer, ICreaturesController creaturesController) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.creaturesController = creaturesController;
    }

    /**
     * Summons the Horned Reaper
     *
     * @param shotTypeId the Summon Reaper shot
     * @param shotData1 how many seconds the Reaper stays, 0 if he stays for as
     * long as the keeper pays for him
     * @param playerId the summoning keeper
     * @param position where to summon the Reaper
     * @return the Reaper, {@code null} if the level has no Reaper
     */
    public EntityId summon(short shotTypeId, int shotData1, short playerId, Vector2f position) {
        Creature reaper = getReaper(kwdFile);
        if (reaper == null) {
            logger.log(Logger.Level.WARNING, "No Horned Reaper to summon");
            return null;
        }

        // The Talisman Reaper has the level of the map and no upkeep
        boolean timed = shotData1 > 0;
        int level = timed ? Math.max(1, kwdFile.getGameLevel().getOneShotHornyLev()) : SPELL_REAPER_LEVEL;
        int manaDrain = timed ? 0 : getManaDrain(shotTypeId);

        EntityId entityId = creaturesController.spawnCreature(reaper.getId(), playerId, level, position, ICreaturesController.SpawnType.CONJURE);
        entityData.setComponent(entityId, new SummonedReaper(gameTimer.getGameTime(), timed ? shotData1 : 0, manaDrain));

        // Once on its feet the Reaper stands and roars before it hunts (DK II sets state 0xEB, 0x4AFAF0)
        entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), CreatureState.FALLEN, reaper.getCreatureId(),
                CreatureState.REAPER_ARRIVE));

        return entityId;
    }

    /**
     * The Summon Reaper spells, also the Talisman's one shot Reaper, can't be
     * cast while the keeper has a Horned Reaper. Checked on the server only,
     * the shared {@link KeeperSpellCastValidator} must not read the entity data
     *
     * @param keeperSpell the spell
     * @param kwdFile the level
     * @param player the casting player
     * @param entityData the entities
     * @return true if the rule allows the cast
     */
    static boolean checkReaperRule(KeeperSpell keeperSpell, IKwdFile kwdFile, Keeper player, EntityData entityData) {
        Shot shot = kwdFile.getShotById(keeperSpell.getShotTypeId());
        if (shot == null || shot.getProcessType() != Shot.ProcessType.SUMMON_REAPER) {
            return true;
        }

        EntitySet creatures = entityData.getEntities(new FieldFilter<>(Owner.class, "ownerId", player.getId()), Owner.class, CreatureComponent.class);
        try {
            for (Entity entity : creatures) {
                Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
                if (creature != null && creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
                    return false;
                }
            }
        } finally {
            creatures.release();
        }

        return true;
    }

    private static Creature getReaper(IKwdFile kwdFile) {
        for (Creature creature : kwdFile.getCreatureList()) {
            if (creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
                return creature;
            }
        }
        return null;
    }

    /**
     * The upkeep is the mana drain of the spell that summons the Reaper for
     * good
     */
    private int getManaDrain(short shotTypeId) {
        for (KeeperSpell keeperSpell : kwdFile.getKeeperSpells()) {
            if (keeperSpell.getShotTypeId() == shotTypeId && keeperSpell.getShotData1() == 0) {
                return keeperSpell.getManaDrain();
            }
        }
        return 0;
    }

}
