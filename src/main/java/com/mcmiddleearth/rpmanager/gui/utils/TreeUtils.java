/*
 * Copyright (C) 2024 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mcmiddleearth.rpmanager.gui.utils;

import com.mcmiddleearth.rpmanager.gui.components.tree.StaticTreeNode;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class TreeUtils {
    private TreeUtils() {}

    public static TreePath getPathForNode(TreeNode node) {
        if (node.getParent() == null) {
            return new TreePath(node);
        } else {
            return getPathForNode(node.getParent()).pathByAddingChild(node);
        }
    }

    public static StaticTreeNode findNode(JTree tree, Object[] path) {
        StaticTreeNode currentNode = (StaticTreeNode) tree.getModel().getRoot();
        if (currentNode == null || !currentNode.getName().equals(path[0].toString())) {
            return null;
        }
        for (int i = 1; i < path.length; ++i) {
            String currentPathComponent = path[i].toString();
            currentNode = currentNode.getChildren().stream().filter(n -> n.getName().equals(currentPathComponent))
                    .findFirst().orElse(null);
            if (currentNode == null) {
                return null;
            }
        }
        return currentNode;
    }
}
