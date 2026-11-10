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
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.ObjectViewState.GameObjectAnimState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * Keeps the Call to Arms banners up. A keeper has one banner, casting again
 * moves it. The banner rises from the ground and calls the keeper's free
 * creatures to it every second, with its effect every third call. The casting
 * cost pays for the first manaCost / manaDrain seconds, after that the banner
 * drains its mana every second. A moved banner sinks back into the ground,
 * slapping the banner or running out of mana knocks it away.
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
    private final GameEventService gameEventService;
    private final KeeperSpell keeperSpell;
    private final GameObject bannerObject;
    private final int upgradedUpkeepPercentage;
    private final Map<Short, Keeper> keepers;
    private final EntitySet banners;
    private final EntitySet creatures;

    /**
     * Keeps the Call to Arms banners up
     *
     * @param entityData the entity data
     * @param kwdFile the level, for the Call to Arms spell, the banner and the
     * variables
     * @param playerControllers the players, for their mana and upgrades
     * @param creaturesController the creatures that answer the call
     * @param gameTimer the game time
     * @param gameEventService tells the clients when the banner calls
     */
    public CallToArmsSystem(EntityData entityData, IKwdFile kwdFile, Collection<IPlayerController> playerControllers,
            ICreaturesController creaturesController, IGameTimer gameTimer, GameEventService gameEventService) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        this.gameTimer = gameTimer;
        this.gameEventService = gameEventService;
        keeperSpell = kwdFile.getKeeperSpells().stream()
                .filter(spell -> spell.getShotData1() == ObjectsController.OBJECT_CALL_TO_ARMS_ID
                && kwdFile.getShotById(spell.getShotTypeId()) != null
                && kwdFile.getShotById(spell.getShotTypeId()).getProcessType() == Shot.ProcessType.CREATE_OBJECT)
                .findFirst().orElse(null);
        bannerObject = kwdFile.getObject(ObjectsController.OBJECT_CALL_TO_ARMS_ID);
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
            CallToArms callToArms = entityData.getComponent(other.getId(), CallToArms.class);
            if (!other.getId().equals(banner.getId()) && other.get(Owner.class).ownerId == ownerId
                    && callToArms != null && callToArms.isUp()) {
                raised = other.getId();
            }
        }

        // A new banner replaces the old one, but not the one in the keeper's hand
        if (raised != null && entityData.getComponent(raised, InHand.class) != null) {
            entityData.removeEntity(banner.getId());
            return;
        }

        // Moving the banner doesn't buy more time before the upkeep. The old
        // banner sinks and its creatures go to the new one
        double upkeepTime;
        if (raised != null) {
            upkeepTime = entityData.getComponent(raised, CallToArms.class).upkeepTime;
            takeDown(raised, GameObjectAnimState.ADDITIONAL_RESOURCE_2, now);
            for (Entity creature : creatures) {
                FollowTarget followTarget = entityData.getComponent(creature.getId(), FollowTarget.class);
                if (followTarget != null && followTarget.entityId.equals(raised)) {
                    entityData.setComponent(creature.getId(), new FollowTarget(banner.getId()));
                }
            }
        } else {
            upkeepTime = now + (keeperSpell != null && keeperSpell.getManaDrain() > 0
                    ? keeperSpell.getManaCost() / keeperSpell.getManaDrain() : 0);
        }
        double waveTime = now + getDuration(bannerObject != null ? bannerObject.getMeshResource() : null);
        entityData.setComponent(banner.getId(), new CallToArms(waveTime, upkeepTime, now, 0, CallToArms.UP));
    }

    private void update(Entity banner, CallToArms callToArms, double now) {
        EntityId bannerId = banner.getId();
        short ownerId = banner.get(Owner.class).ownerId;
        if (!callToArms.isUp()) {
            if (now >= callToArms.removeTime) {
                entityData.removeEntity(bannerId);
            }
            return;
        }
        if (entityData.getComponent(bannerId, Slapped.class) != null) {
            takeDown(bannerId, GameObjectAnimState.ADDITIONAL_RESOURCE_3, now);
            return;
        }

        // No upkeep and no calling while the keeper carries the banner
        if (entityData.getComponent(bannerId, Position.class) == null) {
            entityData.removeComponent(bannerId, Mana.class);
            return;
        }

        if (now >= callToArms.waveTime) {
            setAnimation(bannerId, GameObjectAnimState.ADDITIONAL_RESOURCE_1);
        }

        if (now >= callToArms.upkeepTime) {
            Keeper keeper = keepers.get(ownerId);
            int upkeep = getUpkeep(keeper);
            if (keeper == null || keeper.getMana() < upkeep) {
                takeDown(bannerId, GameObjectAnimState.ADDITIONAL_RESOURCE_3, now);
                return;
            }
            Mana mana = entityData.getComponent(bannerId, Mana.class);
            if (upkeep > 0 && (mana == null || mana.manaGeneration != -upkeep)) {
                entityData.setComponent(bannerId, new Mana(-upkeep));
            }
        }

        if (now >= callToArms.callTime) {
            int calls = callToArms.calls + 1;
            entityData.setComponent(bannerId, new CallToArms(callToArms.waveTime, callToArms.upkeepTime, now + 1, calls, CallToArms.UP));
            if (calls % 3 == 0) {
                gameEventService.emit(GameEventType.CALL_TO_ARMS, bannerId);
            }
            for (Entity creature : creatures) {
                if (creature.get(Owner.class).ownerId == ownerId && !creature.get(CreatureComponent.class).worker
                        && !BUSY_STATES.contains(creature.get(CreatureAi.class).getCreatureState())) {
                    creaturesController.createController(creature.getId()).answerCallToArms(bannerId);
                }
            }
        }
    }

    /**
     * The banner goes down with the given animation and is gone when it ends
     */
    private void takeDown(EntityId bannerId, GameObjectAnimState animation, double now) {
        CallToArms callToArms = entityData.getComponent(bannerId, CallToArms.class);
        int index = animation.ordinal() - GameObjectAnimState.ADDITIONAL_RESOURCE_1.ordinal();
        double removeTime = now + (bannerObject != null && bannerObject.getAdditionalResources().size() > index
                ? getDuration(bannerObject.getAdditionalResources().get(index)) : 0);
        entityData.setComponent(bannerId, new CallToArms(callToArms.waveTime, callToArms.upkeepTime, callToArms.callTime,
                callToArms.calls, removeTime));
        entityData.removeComponent(bannerId, Mana.class);
        setAnimation(bannerId, animation);
    }

    private void setAnimation(EntityId bannerId, GameObjectAnimState animation) {
        ObjectViewState viewState = entityData.getComponent(bannerId, ObjectViewState.class);
        if (viewState != null && viewState.animState != animation) {
            entityData.setComponent(bannerId, new ObjectViewState(viewState.objectId, viewState.state, animation, viewState.visible));
        }
    }

    private static double getDuration(ArtResource animation) {
        if (animation == null) {
            return 0;
        }
        Number frames = animation.getData(ArtResource.KEY_FRAMES);
        Number fps = animation.getData(ArtResource.KEY_FPS);
        return frames != null && fps != null && fps.intValue() > 0 ? frames.doubleValue() / fps.doubleValue() : 0;
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
