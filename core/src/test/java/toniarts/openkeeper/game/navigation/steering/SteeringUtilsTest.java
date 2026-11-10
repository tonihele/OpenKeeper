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
package toniarts.openkeeper.game.navigation.steering;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SteeringUtilsTest {

    private static final Vector2 WORKBENCH = new Vector2(5, 5);
    private static final Vector2 NEXT_TO_WORKBENCH = new Vector2(4, 5);
    private static final Vector2 WORK_POSITION = new Vector2(4.5f, 5);

    @Test
    void creatureOnTheWorkbenchStepsStraightToTheSpot() {

        // Dropped on the workbench, through the middle of the next tile and back would go round in circles
        Vector2 position = new Vector2(5.1f, 4.9f);
        List<Vector2> path = new ArrayList<>(List.of(WORKBENCH, NEXT_TO_WORKBENCH));

        assertEquals(List.of(position, WORK_POSITION), SteeringUtils.pathToPosition(path, position, WORK_POSITION));
    }

    @Test
    void creatureFromFurtherAwayKeepsTheTilesInBetween() {
        Vector2 position = new Vector2(2.2f, 5);
        Vector2 between = new Vector2(3, 5);
        List<Vector2> path = new ArrayList<>(List.of(new Vector2(2, 5), between, NEXT_TO_WORKBENCH));

        assertEquals(List.of(position, between, WORK_POSITION), SteeringUtils.pathToPosition(path, position, WORK_POSITION));
    }

    @Test
    void creatureOnTheTargetTileGoesStraightToTheSpot() {
        Vector2 position = new Vector2(3.8f, 5.2f);

        assertEquals(List.of(position, WORK_POSITION), SteeringUtils.pathToPosition(new ArrayList<>(), position, WORK_POSITION));
        assertEquals(List.of(position, WORK_POSITION), SteeringUtils.pathToPosition(new ArrayList<>(List.of(NEXT_TO_WORKBENCH)), position, WORK_POSITION));
    }

}
