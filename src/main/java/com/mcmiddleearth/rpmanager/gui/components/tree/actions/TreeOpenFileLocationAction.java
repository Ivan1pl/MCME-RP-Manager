/*
 * Copyright (C) 2025 MCME
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

package com.mcmiddleearth.rpmanager.gui.components.tree.actions;

import com.mcmiddleearth.rpmanager.gui.MainWindow;
import com.mcmiddleearth.rpmanager.gui.actions.Action;
import com.mcmiddleearth.rpmanager.gui.components.tree.StaticTreeNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class TreeOpenFileLocationAction extends Action {
    private final JTree tree;

    public TreeOpenFileLocationAction(JTree tree) {
        super("Open file location", null, "Open file location in system file browser",
                KeyEvent.VK_O, null);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            File file = ((StaticTreeNode) path.getLastPathComponent()).getFile();
            try {
                Desktop.getDesktop().browse(file.getParentFile().toURI());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(MainWindow.getInstance(), "Unknown error", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
