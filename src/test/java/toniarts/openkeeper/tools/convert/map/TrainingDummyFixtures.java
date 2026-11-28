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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;
import java.util.List;

/**
 * Builds the training room's data for the training dummy tests. Lives in the
 * data classes' own package because their setters are {@code protected}
 * (parser-only).
 */
public final class TrainingDummyFixtures {

    private TrainingDummyFixtures() {
    }

    /**
     * The Training Dummy as DK II has it: a still mesh, and its recoil as the
     * first additional resource
     *
     * @param objectId the object id
     * @param recoilFrames the frames of the recoil animation
     * @param recoilFps the speed of the recoil animation
     * @return the dummy
     */
    public static GameObject trainingDummy(short objectId, int recoilFrames, int recoilFps) {
        ArtResource mesh = new ArtResource();
        mesh.setName("Training Dummy");
        mesh.setType(ArtResource.ArtResourceType.MESH);
        mesh.setFlags(EnumSet.noneOf(ArtResource.ArtResourceFlag.class));

        ArtResource recoil = new ArtResource();
        recoil.setName("Recoil_back");
        recoil.setType(ArtResource.ArtResourceType.ANIMATING_MESH);
        recoil.setFlags(EnumSet.noneOf(ArtResource.ArtResourceFlag.class));
        recoil.setData(ArtResource.KEY_FRAMES, recoilFrames);
        recoil.setData(ArtResource.KEY_FPS, recoilFps);

        GameObject dummy = new GameObject();
        dummy.setName("Training Dummy");
        dummy.setObjectId(objectId);
        dummy.setMeshResource(mesh);
        dummy.setAdditionalResources(List.of(recoil));
        return dummy;
    }

    /**
     * A level variable
     *
     * @param type the variable
     * @param value its value
     * @return the variable
     */
    public static Variable.MiscVariable trainingVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

}
