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
import com.mcmiddleearth.rpmanager.model.SmlScopes;
import com.mcmiddleearth.rpmanager.utils.ResourcePackUtils;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class GenerateSmlScopesAction extends Action {
    private final JTree tree;

    public GenerateSmlScopesAction(JTree tree) {
        super("Generate sml_load_scopes", null, "Generate sml_load_scopes", KeyEvent.VK_L, KeyEvent.VK_N);
        this.tree = tree;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            StaticTreeNode node = (StaticTreeNode) tree.getLastSelectedPathComponent();
            File namespaceDir = node.getFile();
            File smlLoadScopesDir = new File(namespaceDir, "sml_load_scopes");
            File testScopesFile = new File(smlLoadScopesDir, "test_scopes.json");
            File blocksDir = new File(new File(namespaceDir, "models"), "block");
            List<String> entries = new LinkedList<>();
            if (blocksDir.exists()) {
                traverse(blocksDir, entries, namespaceDir.getName() + ":block/");
            }
            Collections.sort(entries);
            SmlScopes smlScopes = new SmlScopes();
            smlScopes.setVersion(1);
            smlScopes.setEntries(entries);
            com.mcmiddleearth.rpmanager.utils.Action redoAction =
                    () -> ResourcePackUtils.saveFile(smlScopes, testScopesFile);
            com.mcmiddleearth.rpmanager.utils.Action undoAction;
            if (testScopesFile.exists()) {
                byte[] content = Files.readAllBytes(testScopesFile.toPath());
                undoAction = () -> Files.write(testScopesFile.toPath(), content, StandardOpenOption.WRITE);
            } else {
                undoAction = () -> testScopesFile.delete();
            }
            if (!smlLoadScopesDir.exists()) {
                redoAction = redoAction.butFirst(() -> smlLoadScopesDir.mkdir());
                undoAction = undoAction.then(() -> smlLoadScopesDir.delete());
            }
            MainWindow.getInstance().getActionManager().submit(undoAction, redoAction);
            node.refreshGitStatus();
            ((DefaultTreeModel) tree.getModel()).reload(node.getParent());
            tree.revalidate();
            tree.repaint();
            SwingUtilities.invokeLater(() -> MainWindow.getInstance().getActionManager().refresh());
        } catch (GitAPIException | IOException e) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(), "Unknown error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void traverse(File dir, List<String> entries, String prefix) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                traverse(file, entries, prefix + file.getName() + "/");
            } else if (file.getName().endsWith(".json") || file.getName().endsWith(".JSON")) {
                entries.add(prefix + file.getName().substring(0, file.getName().length() - 5));
            }
        }
    }
}
