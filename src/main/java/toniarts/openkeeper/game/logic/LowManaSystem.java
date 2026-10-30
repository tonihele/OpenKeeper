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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ImpPopCountdown;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.component.LowMana;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Warns a keeper whose mana runs low and, if it keeps running low, pops his
 * imps one by one. Runs once a second like the mana itself.
 */
public final class LowManaSystem implements IGameLogicUpdatable {

    /**
     * The speech of the four low mana warnings
     */
    private static final int[] WARNING_SPEECH_IDS = {447, 1329, 1330, 1331};

    /**
     * "You do not have enough Mana to maintain your Imps"
     */
    private static final int IMP_POP_SPEECH_ID = 1333;

    /**
     * The stage after the fourth warning, the imps are popped
     */
    private static final int POPPING_STAGE = 5;

    /**
     * Seconds from choosing an imp to it popping. DK II has it fixed,
     * IMP_POP_COUNTDOWN_TIME_SECONDS is not used
     */
    private static final int IMP_POP_SECONDS = 5;

    private float tick = 0;
    private final EntityData entityData;
    private final PlayerService playerService;
    private final Map<Short, Keeper> keepers;
    private final EntitySet imps;
    private final EntitySet popCountdowns;
    private final int warningThreshold;
    private final int evaluationPeriod;
    private final int minimumImps;
    private final int createImpManaCost;

    /**
     * The warnings and the imp choice are timed by the level's
     * INSUFFICIENT_MANA_IMP_EVALUATION_PERIOD_SECONDS
     *
     * @param entityData the entity data
     * @param kwdFile the level, for the imp and the Create Imp spell
     * @param playerControllers the players, the ones with mana are warned
     * @param gameSettings the level variables
     * @param playerService for the mentor's messages
     */
    public LowManaSystem(EntityData entityData, IKwdFile kwdFile, Collection<IPlayerController> playerControllers,
            Map<MiscType, Variable.MiscVariable> gameSettings, PlayerService playerService) {
        this.entityData = entityData;
        this.playerService = playerService;
        keepers = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController playerController : playerControllers) {
            if (playerController.getManaControl() != null) {
                keepers.put(playerController.getKeeper().getId(), playerController.getKeeper());
            }
        }
        warningThreshold = (int) gameSettings.get(MiscType.LOW_MANA_WARNING_THRESHOLD).getValue();
        evaluationPeriod = (int) gameSettings.get(MiscType.INSUFFICIENT_MANA_IMP_EVALUATION_PERIOD_SECONDS).getValue();
        minimumImps = (int) gameSettings.get(MiscType.MINIMUM_IMP_THRESHOLD).getValue();
        int manaCost = 0;
        for (KeeperSpell keeperSpell : kwdFile.getKeeperSpells()) {
            if (keeperSpell.getFlags().contains(KeeperSpell.KeeperSpellFlag.CREATE_WORKER)) {
                manaCost = keeperSpell.getManaCost();
                break;
            }
        }
        createImpManaCost = manaCost;

        imps = entityData.getEntities(new FieldFilter(CreatureComponent.class, "creatureId", kwdFile.getImp().getId()),
                CreatureComponent.class, CreatureExperience.class, Owner.class);
        popCountdowns = entityData.getEntities(ImpPopCountdown.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        tick += tpf;
        if (tick >= 1) {
            imps.applyChanges();
            popCountdowns.applyChanges();
            updatePopCountdowns();
            for (Keeper keeper : keepers.values()) {
                updateKeeper(keeper);
            }
            tick -= 1;
        }
    }

    /**
     * An imp pops when his countdown runs out and his keeper still can't
     * afford a new imp. If the keeper can, the countdown is called off
     */
    private void updatePopCountdowns() {
        for (Entity entity : popCountdowns) {
            Keeper keeper = keepers.get(entity.get(Owner.class).ownerId);
            if (keeper == null || canCreateImp(keeper)) {
                entityData.removeComponent(entity.getId(), ImpPopCountdown.class);
                continue;
            }

            int secondsLeft = entity.get(ImpPopCountdown.class).secondsLeft - 1;
            Health health = entityData.getComponent(entity.getId(), Health.class);
            if (secondsLeft <= 0 && health != null && entityData.getComponent(entity.getId(), Position.class) != null) {
                EntityController.setDamage(entityData, entity.getId(), health.health);
                entityData.removeComponent(entity.getId(), ImpPopCountdown.class);
            } else {
                entityData.setComponent(entity.getId(), new ImpPopCountdown(secondsLeft));
            }
        }
    }

    private boolean canCreateImp(Keeper keeper) {
        return createImpManaCost <= keeper.getMana();
    }

    private void updateKeeper(Keeper keeper) {
        EntityId playerEntity = entityData.findEntity(new FieldFilter(KeeperComponent.class, "playerId", keeper.getId()), KeeperComponent.class);
        if (playerEntity == null) {
            return;
        }
        if (keeper.getMana() >= warningThreshold) {
            entityData.removeComponent(playerEntity, LowMana.class);
            return;
        }

        LowMana lowMana = entityData.getComponent(playerEntity, LowMana.class);
        int stage = lowMana != null ? lowMana.stage : 0;
        int secondsToWarning = lowMana != null ? lowMana.secondsToWarning - 1 : 0;
        int secondsToImpChoice = lowMana != null ? lowMana.secondsToImpChoice - 1 : -1;

        // Warn while the mana isn't growing, the first warning comes one period after the mana went low
        if (stage < POPPING_STAGE && keeper.getManaGain() - keeper.getManaLoose() <= 0) {
            if (stage == 0) {
                stage = 1;
                secondsToWarning = evaluationPeriod;
            } else if (secondsToWarning <= 0) {

                // Without spare imps the third warning is repeated, the fourth one starts the popping
                if (stage == POPPING_STAGE - 1 && getImps(keeper.getId()).size() <= minimumImps) {
                    stage--;
                }
                playerService.showMentorMessage(WARNING_SPEECH_IDS[stage - 1], null, keeper.getId());
                stage++;
                secondsToWarning += evaluationPeriod;
            }
        }

        // Pop the imps, even if the mana is growing again
        if (stage == POPPING_STAGE && secondsToImpChoice < 0) {
            chooseImpToPop(keeper);
            secondsToImpChoice = evaluationPeriod;
        }

        entityData.setComponent(playerEntity, new LowMana(stage, secondsToWarning, secondsToImpChoice));
    }

    /**
     * Chooses the imp to pop, if the keeper has more imps than the minimum.
     * It is the imp with the lowest level. If several share it, it is an idle
     * imp, or failing that the imp farthest from the Dungeon Heart. Imps in
     * hand are not chosen.
     *
     * @param keeper the keeper
     */
    private void chooseImpToPop(Keeper keeper) {
        List<Entity> ownImps = getImps(keeper.getId());
        if (ownImps.size() <= minimumImps) {
            return;
        }
        ownImps.removeIf(imp -> entityData.getComponent(imp.getId(), Position.class) == null);

        Entity chosen = null;
        boolean shared = false;
        for (Entity imp : ownImps) {
            int level = imp.get(CreatureExperience.class).level;
            if (chosen == null || level < chosen.get(CreatureExperience.class).level) {
                chosen = imp;
                shared = false;
            } else if (level == chosen.get(CreatureExperience.class).level) {
                shared = true;
            }
        }
        if (shared) {
            chosen = getIdleImp(ownImps);
            if (chosen == null) {
                chosen = getImpFarthestFrom(ownImps, keeper.getDungeonHeartLocation());
            }
        }
        if (chosen == null) {
            return;
        }

        entityData.setComponent(chosen.getId(), new ImpPopCountdown(IMP_POP_SECONDS));
        playerService.showMentorMessage(IMP_POP_SPEECH_ID, null, keeper.getId());
    }

    private Entity getIdleImp(List<Entity> imps) {
        for (Entity imp : imps) {
            CreatureAi creatureAi = entityData.getComponent(imp.getId(), CreatureAi.class);
            if (creatureAi != null && creatureAi.getCreatureState() == CreatureState.IDLE) {
                return imp;
            }
        }
        return null;
    }

    private Entity getImpFarthestFrom(List<Entity> imps, Point tile) {
        if (tile == null) {
            return null;
        }
        Vector3f heart = WorldUtils.pointToVector3f(tile);
        Entity farthest = null;
        float farthestDistance = -1;
        for (Entity imp : imps) {
            Vector3f position = entityData.getComponent(imp.getId(), Position.class).position;
            float dx = position.x - heart.x;
            float dz = position.z - heart.z;
            float distance = dx * dx + dz * dz;
            if (distance > farthestDistance) {
                farthest = imp;
                farthestDistance = distance;
            }
        }
        return farthest;
    }

    /**
     * The keeper's imps, oldest first
     */
    private List<Entity> getImps(short playerId) {
        List<Entity> ownImps = new ArrayList<>();
        for (Entity imp : imps) {
            if (imp.get(Owner.class).ownerId == playerId) {
                ownImps.add(imp);
            }
        }
        ownImps.sort(Comparator.comparing(Entity::getId));
        return ownImps;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        imps.release();
        popCountdowns.release();
    }

}
