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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.ReaperAdvised;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * The mentor's advice that no event brings up, given from each keeper's own
 * tick in DK II (DKII.EXE 0x4B9740). A keeper who is out of the game gets
 * none.
 * <br>
 * Once a level, a keeper with a Horned Reaper whose mana, less what its Imps
 * and traps take a second, is under 25000 is told about it.
 */
public final class MentorHintSystem implements IGameLogicUpdatable {

    /**
     * Not in the level data, DKII.EXE has the number in the check itself
     */
    private static final int REAPER_ADVICE_MANA = 25000;

    private final EntityData entityData;
    private final RoomAttackMessages messages;
    private final List<Keeper> keepers = new ArrayList<>();
    private final List<EntitySet> reapers = new ArrayList<>();

    /**
     * The mentor hint system
     *
     * @param entityData the entity data
     * @param kwdFile the level, for the Horned Reaper
     * @param playerControllers the players, the ones with mana get the advice
     * @param messages for the mentor's messages
     */
    public MentorHintSystem(EntityData entityData, IKwdFile kwdFile, Collection<IPlayerController> playerControllers,
            RoomAttackMessages messages) {
        this.entityData = entityData;
        this.messages = messages;
        for (IPlayerController playerController : playerControllers) {
            if (playerController.getManaControl() != null) {
                keepers.add(playerController.getKeeper());
            }
        }
        for (Creature creature : kwdFile.getCreatureList()) {
            if (creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
                reapers.add(entityData.getEntities(new FieldFilter<>(CreatureComponent.class, "creatureId", creature.getCreatureId()),
                        CreatureComponent.class, Owner.class, Health.class));
            }
        }
    }

    @Override
    public void processTick(float tpf) {
        for (EntitySet reaperSet : reapers) {
            reaperSet.applyChanges();
        }
        for (Keeper keeper : keepers) {
            if (!keeper.isDestroyed()) {
                adviseOnReaper(keeper);
            }
        }
    }

    private void adviseOnReaper(Keeper keeper) {
        if (keeper.getMana() - keeper.getManaUpkeep() >= REAPER_ADVICE_MANA || !hasReaper(keeper.getId())) {
            return;
        }
        EntityId playerEntity = entityData.findEntity(new FieldFilter<>(KeeperComponent.class, "playerId", keeper.getId()), KeeperComponent.class);
        if (playerEntity != null && entityData.getComponent(playerEntity, ReaperAdvised.class) == null) {
            entityData.setComponent(playerEntity, new ReaperAdvised());
            messages.reaperAdvice(keeper.getId());
        }
    }

    private boolean hasReaper(short playerId) {
        for (EntitySet reaperSet : reapers) {
            for (Entity reaper : reaperSet) {
                if (reaper.get(Owner.class).ownerId == playerId) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        for (EntitySet reaperSet : reapers) {
            reaperSet.release();
        }
    }

}
