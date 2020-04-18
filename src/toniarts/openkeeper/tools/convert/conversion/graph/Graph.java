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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Serves as a kinda root for the graph and offers methods for construction of a
 * graph
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> The node type
 */
public class Graph<T extends Node> {

    private final Map<T, T> nodes = new HashMap<>();
    private final Set<T> rootNodes = new HashSet<>();

    /**
     * Adds the node to the graph or gets the node if it already exists
     *
     * @param node the node
     * @return the node
     */
    public T add(T node) {
        if (nodes.containsKey(node)) {
            return nodes.get(node);
        } else {
            nodes.put(node, node);

            // Maintain the root nodes
            rootNodes.add(node);

            return node;
        }
    }

    /**
     * Adds dependency between two nodes
     *
     * @param dependency the parent node
     * @param dependant the child node
     */
    public void addDependency(T dependency, T dependant) {
        T dependencyNode = add(dependency);
        T dependantNode = add(dependant);

        // Add the connection
        addEdges(dependencyNode, dependantNode);
    }

    private void addEdges(T dependencyNode, T dependantNode) {
        dependencyNode.addOutgoingNode(dependantNode);
        dependantNode.addIncomingNode(dependencyNode);

        // Maintain the root nodes
        rootNodes.remove(dependantNode);
    }

    /**
     * Get root nodes
     *
     * @return all the root nodes
     */
    public List<T> getRootNodes() {
        return new ArrayList<>(rootNodes);
    }

}
