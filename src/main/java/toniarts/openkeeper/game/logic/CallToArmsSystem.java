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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CallToArms;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Keeps the Call to Arms banners up. A keeper has one banner, casting again
 * moves it. Every second the banner calls the keeper's free creatures to it.
 * The casting cost pays for the first manaCost / manaDrain seconds, after that
 * the banner drains its mana every second. Slapping the banner or running out
 * of mana takes it down.
 */
public final class CallToArmsSystem implements IGameLogicUpdatable {

    /**
     * Creatures in these states don't answer the call
     */
    private static final Set<CreatureState> BUSY_STATES = EnumSet.of(CreatureState.CALL_TO_ARMS,
            CreatureState.ENTERING_DUNGEON, CreatureState.FIGHT, CreatureState.MELEE_ATTACK,
            CreatureState.CAST_SPELL, CreatureState.FLEE, CreatureState.IMPRISONED, CreatureState.TORTURED,
            CreatureState.UNCONSCIOUS, CreatureState.STUNNED, CreatureState.DRAGGED, CreatureState.FALLEN,
            CreatureState.GETTING_UP, CreatureState.PICKED_UP, CreatureState.DEAD);

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final IGameTimer gameTimer;
    private final KeeperSpell keeperSpell;
    private final int upgradedUpkeepPercentage;
    private final Map<Short, Keeper> keepers;
    private final EntitySet banners;
    private final EntitySet creatures;

    /**
     * Keeps the Call to Arms banners up
     *
     * @param entityData the entity data
     * @param kwdFile the level, for the Call to Arms spell and its variables
     * @param playerControllers the players, for their mana and upgrades
     * @param creaturesController the creatures that answer the call
     * @param gameTimer the game time
     */
    public CallToArmsSystem(EntityData entityData, IKwdFile kwdFile, Collection<IPlayerController> playerControllers,
            ICreaturesController creaturesController, IGameTimer gameTimer) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        this.gameTimer = gameTimer;
        keeperSpell = kwdFile.getKeeperSpells().stream()
                .filter(spell -> spell.getShotData1() == ObjectsController.OBJECT_CALL_TO_ARMS_ID
                && kwdFile.getShotById(spell.getShotTypeId()) != null
                && kwdFile.getShotById(spell.getShotTypeId()).getProcessType() == Shot.ProcessType.CREATE_OBJECT)
                .findFirst().orElse(null);
        upgradedUpkeepPercentage = (int) kwdFile.getVariables().get(Variable.MiscVariable.MiscType.UPGRADED_CALL_TO_ARMS_COST_PERCENTAGE).getValue();
        keepers = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController playerController : playerControllers) {
            keepers.put(playerController.getKeeper().getId(), playerController.getKeeper());
        }

        banners = entityData.getEntities(new FieldFilter<>(ObjectComponent.class, "objectId", ObjectsController.OBJECT_CALL_TO_ARMS_ID),
                ObjectComponent.class, Owner.class);
        creatures = entityData.getEntities(CreatureComponent.class, CreatureAi.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        double now = gameTimer.getGameTime();
        creatures.applyChanges();
        if (banners.applyChanges()) {
            for (Entity banner : banners.getAddedEntities()) {
                raise(banner, now);
            }
        }

        for (Entity banner : banners) {
            CallToArms callToArms = entityData.getComponent(banner.getId(), CallToArms.class);
            if (callToArms != null) {
                update(banner, callToArms, now);
            }
        }
    }

    private void raise(Entity banner, double now) {
        short ownerId = banner.get(Owner.class).ownerId;
        EntityId raised = null;
        for (Entity other : banners) {
            if (!other.getId().equals(banner.getId()) && other.get(Owner.class).ownerId == ownerId
                    && entityData.getComponent(other.getId(), CallToArms.class) != null) {
                raised = other.getId();
            }
        }

        // A new banner replaces the old one, but not the one in the keeper's hand
        if (raised != null && entityData.getComponent(raised, InHand.class) != null) {
            entityData.removeEntity(banner.getId());
            return;
        }

        // Moving the banner doesn't buy more time before the upkeep
        double upkeepTime;
        if (raised != null) {
            upkeepTime = entityData.getComponent(raised, CallToArms.class).upkeepTime;
            entityData.removeEntity(raised);
        } else {
            upkeepTime = now + (keeperSpell != null && keeperSpell.getManaDrain() > 0
                    ? keeperSpell.getManaCost() / keeperSpell.getManaDrain() : 0);
        }
        entityData.setComponent(banner.getId(), new CallToArms(upkeepTime, now));
    }

    private void update(Entity banner, CallToArms callToArms, double now) {
        EntityId bannerId = banner.getId();
        short ownerId = banner.get(Owner.class).ownerId;
        if (entityData.getComponent(bannerId, Slapped.class) != null) {
            entityData.removeEntity(bannerId);
            return;
        }

        // No upkeep and no calling while the keeper carries the banner
        if (entityData.getComponent(bannerId, Position.class) == null) {
            entityData.removeComponent(bannerId, Mana.class);
            return;
        }

        if (now >= callToArms.upkeepTime) {
            Keeper keeper = keepers.get(ownerId);
            int upkeep = getUpkeep(keeper);
            if (keeper == null || keeper.getMana() < upkeep) {
                entityData.removeEntity(bannerId);
                return;
            }
            Mana mana = entityData.getComponent(bannerId, Mana.class);
            if (upkeep > 0 && (mana == null || mana.manaGeneration != -upkeep)) {
                entityData.setComponent(bannerId, new Mana(-upkeep));
            }
        }

        if (now >= callToArms.callTime) {
            entityData.setComponent(bannerId, new CallToArms(callToArms.upkeepTime, now + 1));
            for (Entity creature : creatures) {
                if (creature.get(Owner.class).ownerId == ownerId && !creature.get(CreatureComponent.class).worker
                        && !BUSY_STATES.contains(creature.get(CreatureAi.class).getCreatureState())) {
                    creaturesController.createController(creature.getId()).answerCallToArms(bannerId);
                }
            }
        }
    }

    private int getUpkeep(Keeper keeper) {
        if (keeperSpell == null) {
            return 0;
        }
        boolean upgraded = keeper != null && keeper.getAvailableSpells().stream()
                .anyMatch(spell -> spell.getId() == keeperSpell.getId() && spell.isUpgraded());
        return upgraded ? keeperSpell.getManaDrain() * upgradedUpkeepPercentage / 100 : keeperSpell.getManaDrain();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        banners.release();
        creatures.release();
    }

}
