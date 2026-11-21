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
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Warns a keeper whose mana runs low and, if it keeps running low, pops his
 * imps one by one. A keeper with no mana at all also hears the no mana alert.
 * Runs once a second like the mana itself.
 */
public final class LowManaSystem implements IGameLogicUpdatable {

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
    private final RoomAttackMessages messages;
    private final GameEventService gameEventService;
    private final short heartId;
    private final Map<Short, Keeper> keepers;
    private final short impId;
    private final EntitySet creatures;
    private final EntitySet popCountdowns;
    private final int warningThreshold;
    private final int evaluationPeriod;
    private final int minimumImps;
    private final int createImpManaCost;

    /**
     * The warnings, the no mana alert and the imp choice are timed by the
     * level's INSUFFICIENT_MANA_IMP_EVALUATION_PERIOD_SECONDS
     *
     * @param entityData the entity data
     * @param kwdFile the level, for the imp, the Create Imp spell and the
     * Dungeon Heart
     * @param playerControllers the players, the ones with mana are warned
     * @param gameSettings the level variables
     * @param messages for the mentor's messages
     * @param gameEventService for the no mana alert
     */
    public LowManaSystem(EntityData entityData, IKwdFile kwdFile, Collection<IPlayerController> playerControllers,
            Map<MiscType, Variable.MiscVariable> gameSettings, RoomAttackMessages messages,
            GameEventService gameEventService) {
        this.entityData = entityData;
        this.messages = messages;
        this.gameEventService = gameEventService;
        heartId = kwdFile.getDungeonHeart().getId();
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

        impId = kwdFile.getImp().getId();
        creatures = entityData.getEntities(CreatureComponent.class, CreatureExperience.class, Owner.class);
        popCountdowns = entityData.getEntities(ImpPopCountdown.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        tick += tpf;
        if (tick >= 1) {
            creatures.applyChanges();
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
        int secondsToNoManaAlert = lowMana != null ? lowMana.secondsToNoManaAlert - 1 : 0;

        // With no mana the heart sounds its alert, again every period while the mana stays at 0 (0x4C10E0)
        if (keeper.getMana() == 0 && secondsToNoManaAlert <= 0) {
            gameEventService.emitToPlayer(GameEventType.NO_MANA, ThingFlag.ROOM, heartId, keeper.getId());
            secondsToNoManaAlert = evaluationPeriod;
        }

        // Warn while the mana isn't growing, the first warning comes one period after the mana went low
        if (stage < POPPING_STAGE && keeper.getManaGain() - keeper.getManaLoose() <= 0) {
            if (stage == 0) {
                stage = 1;
                secondsToWarning = evaluationPeriod;
            } else if (secondsToWarning <= 0) {

                // Without spare imps the third warning is repeated, the fourth one starts the popping
                if (stage == POPPING_STAGE - 1 && getCreatures(keeper.getId(), true).size() <= minimumImps) {
                    stage--;
                }
                messages.lowManaWarning(keeper.getId(), stage);
                stage++;
                secondsToWarning += evaluationPeriod;
            }
        }

        // Pop the imps, even if the mana is growing again
        if (stage == POPPING_STAGE && secondsToImpChoice < 0) {
            chooseImpToPop(keeper);
            secondsToImpChoice = evaluationPeriod;
        }

        entityData.setComponent(playerEntity, new LowMana(stage, secondsToWarning, secondsToImpChoice, secondsToNoManaAlert));
    }

    /**
     * Chooses the imp to pop, if the keeper has more imps than the minimum.
     * It is the keeper's creature with the lowest level, of any kind, and
     * only an imp pops, so a lower level creature that isn't an imp saves the
     * imps for this period (0x4C1BC0, 0x488930). If several share the lowest
     * level, it is an idle imp, or failing that the imp farthest from the
     * Dungeon Heart. Creatures in hand are not chosen.
     *
     * @param keeper the keeper
     */
    private void chooseImpToPop(Keeper keeper) {
        List<Entity> ownImps = getCreatures(keeper.getId(), true);
        if (ownImps.size() <= minimumImps) {
            return;
        }
        ownImps.removeIf(this::isInHand);

        Entity chosen = null;
        boolean shared = false;
        for (Entity creature : getCreatures(keeper.getId(), false)) {
            if (isInHand(creature)) {
                continue;
            }
            int level = creature.get(CreatureExperience.class).level;
            if (chosen == null || level < chosen.get(CreatureExperience.class).level) {
                chosen = creature;
                shared = false;
            } else if (level == chosen.get(CreatureExperience.class).level) {
                shared = true;
            }
        }
        if (shared || chosen == null) {
            chosen = getIdleImp(ownImps);
            if (chosen == null) {
                chosen = getImpFarthestFrom(ownImps, keeper.getDungeonHeartLocation());
            }
        }
        if (chosen == null) {
            return;
        }

        if (chosen.get(CreatureComponent.class).creatureId == impId) {
            entityData.setComponent(chosen.getId(), new ImpPopCountdown(IMP_POP_SECONDS));
        }
        messages.notEnoughManaForImps(keeper.getId());
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

    private boolean isInHand(Entity creature) {
        return entityData.getComponent(creature.getId(), Position.class) == null;
    }

    /**
     * The keeper's creatures, oldest first
     *
     * @param playerId the keeper
     * @param impsOnly only his imps
     */
    private List<Entity> getCreatures(short playerId, boolean impsOnly) {
        List<Entity> own = new ArrayList<>();
        for (Entity creature : creatures) {
            if (creature.get(Owner.class).ownerId == playerId
                    && (!impsOnly || creature.get(CreatureComponent.class).creatureId == impId)) {
                own.add(creature);
            }
        }
        own.sort(Comparator.comparing(Entity::getId));
        return own;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        creatures.release();
        popCountdowns.release();
    }

}
