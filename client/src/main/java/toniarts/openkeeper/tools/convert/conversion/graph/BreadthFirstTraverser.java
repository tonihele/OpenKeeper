/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.tools.convert.conversion.graph;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Breadth-first traversing algorithm, traverses only "down" (outgoing nodes)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> The type of node
 */
public class BreadthFirstTraverser<T extends Node> implements ITraverser<T> {

    @Override
    public void traverse(T startNode, ITraverserAction<T> action) {
        Queue<T> queue = new ArrayDeque<>();
        Set<T> visitedNodes = new HashSet<>();
        queue.add(startNode);
        visitedNodes.add(startNode);
        while (!queue.isEmpty()) {
            T node = queue.poll();
            if (action.onVisitNode(node)) {
                for (Object childNodeObj : node.getOutgoingNodes()) {
                    T childNode = (T) childNodeObj;
                    if (!visitedNodes.contains(childNode) && isVisitChildValid(childNode)) {
                        queue.add(childNode);
                        visitedNodes.add(childNode);
                    }
                }
            }
        }
    }

    protected boolean isVisitChildValid(T childNode) {
        return true;
    }

}
