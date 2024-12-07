/*
 * Copyright (C) 2022 MCME
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

package com.mcmiddleearth.rpmanager.gui.panes;

import com.mcmiddleearth.rpmanager.gui.MainWindow;
import com.mcmiddleearth.rpmanager.gui.components.FastScrollPane;
import com.mcmiddleearth.rpmanager.gui.components.IconButton;
import com.mcmiddleearth.rpmanager.gui.components.TextInput;
import com.mcmiddleearth.rpmanager.gui.components.VerticalBox;
import com.mcmiddleearth.rpmanager.gui.components.tree.*;
import com.mcmiddleearth.rpmanager.gui.components.tree.actions.*;
import com.mcmiddleearth.rpmanager.gui.constants.Icons;
import com.mcmiddleearth.rpmanager.gui.listeners.LayerTreeSelectionListener;
import com.mcmiddleearth.rpmanager.gui.utils.TreeUtils;
import com.mcmiddleearth.rpmanager.model.project.Layer;
import com.mcmiddleearth.rpmanager.model.project.Project;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class LayerFilesPane extends JPanel {
    private final Layer layer;
    private final JTree tree;
    private boolean eventsEnabled = true;
    private String searchText = "";
    private String filterText = "";

    public LayerFilesPane(Layer layer, Project project) throws IOException, GitAPIException {
        this.layer = layer;

        com.mcmiddleearth.rpmanager.gui.actions.Action nextAction =
                new com.mcmiddleearth.rpmanager.gui.actions.Action("v", Icons.NEXT_ICON, "Next") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        findNext();
                    }
                };
        com.mcmiddleearth.rpmanager.gui.actions.Action previousAction =
                new com.mcmiddleearth.rpmanager.gui.actions.Action("^", Icons.PREVIOUS_ICON, "Previous") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        findPrevious();
                    }
                };
        nextAction.setEnabled(false);
        previousAction.setEnabled(false);

        setLayout(new BorderLayout());
        VerticalBox verticalBox = new VerticalBox();

        JLabel title = new JLabel(layer.getName());
        title.setFont(new Font(title.getFont().getName(), Font.BOLD, title.getFont().getSize()));
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));

        JButton refreshButton = new IconButton(
                new com.mcmiddleearth.rpmanager.gui.actions.Action("Refresh", Icons.REFRESH_ICON, "Refresh files") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        reload();
                    }
                });
        toolbar.add(refreshButton);

        JButton deleteButton = new IconButton(
                new com.mcmiddleearth.rpmanager.gui.actions.Action("-", Icons.DELETE_ICON, "Remove layer") {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        if (JOptionPane.showConfirmDialog(MainWindow.getInstance(),
                                "Removing a layer cannot be undone. Do you want to continue?",
                                "Confirm removing layer",
                                JOptionPane.YES_NO_OPTION) == 0) {
                            project.removeLayer(layer);
                        }
                    }
                });
        if (layer.getFile().getName().endsWith(".jar")) {
            deleteButton.setEnabled(false);
        }
        toolbar.add(deleteButton);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(title, BorderLayout.CENTER);
        titlePanel.add(toolbar, BorderLayout.LINE_END);

        verticalBox.add(titlePanel);
        Box horizontalBox = Box.createHorizontalBox();
        horizontalBox.add(new TextInput("", s -> this.searchText = s, input -> {
            nextAction.setEnabled(!input.getText().isEmpty());
            previousAction.setEnabled(!input.getText().isEmpty());
        }));

        IconButton next = new IconButton(nextAction);
        IconButton previous = new IconButton(previousAction);
        horizontalBox.add(next);
        horizontalBox.add(previous);
        horizontalBox.add(Box.createHorizontalGlue());
        Box filterBox = Box.createHorizontalBox();
        filterBox.add(new TextInput("", s -> this.filterText = s, input -> {}));
        filterBox.add(new JButton(new com.mcmiddleearth.rpmanager.gui.actions.Action("Filter", "Filter files") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((ExpansionStateAwareTreeModel) tree.getModel()).filter(filterText);
            }
        }));
        filterBox.add(Box.createHorizontalGlue());
        verticalBox.add(filterBox);
        verticalBox.add(horizontalBox);
        add(verticalBox, BorderLayout.PAGE_START);
        add(new FastScrollPane(this.tree = createTree(layer.getFile()),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
    }

    private void findNext() {
        find(1);
    }

    private void findPrevious() {
        find(-1);
    }

    private void find(int step) {
        int selectedNode = tree.getMinSelectionRow();
        if (selectedNode < 0) {
            selectedNode = 0;
        }
        for (int i = 1; i < tree.getRowCount(); ++i) {
            int row = (selectedNode + i * step) % tree.getRowCount();
            while (row < 0) {
                row = tree.getRowCount() + row;
            }
            TreePath path = tree.getPathForRow(row);
            StaticTreeNode node = (StaticTreeNode) path.getLastPathComponent();
            if (node.getName().contains(searchText)) {
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                break;
            }
        }
    }

    private static JTree createTree(File file) throws IOException, GitAPIException {
        ExpansionStateAwareTreeModel model = new ExpansionStateAwareTreeModel(createRootNode(file));
        boolean editable = !file.getName().endsWith(".jar");
        JTree tree = new JTree(model);
        tree.setCellRenderer(new StatusTreeCellRenderer());
        model.setTree(tree);
        tree.setComponentPopupMenu(createPopupMenu(tree, editable));
        tree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                MainWindow.getInstance().invalidate();
                MainWindow.getInstance().repaint();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                MainWindow.getInstance().invalidate();
                MainWindow.getInstance().repaint();
            }
        });
        return tree;
    }

    private static StaticTreeNode createRootNode(File file) throws IOException, GitAPIException {
        return file.getName().endsWith(".jar") ?
                JarTreeFactory.createRootNode(file) : ResourcePackTreeFactory.createRootNode(file);
    }

    private static JPopupMenu createPopupMenu(JTree tree, boolean editable) {
        JMenu newMenu = new JMenu("New");
        Action newFileAction = new TreeNewFileAction(tree);
        newFileAction.setEnabled(false);
        Action newDirectoryAction = new TreeNewDirectoryAction(tree);
        newDirectoryAction.setEnabled(false);
        Action copyAction = new TreeCopyAction(tree);
        copyAction.setEnabled(false);
        Action pasteAction = new TreePasteAction(tree);
        pasteAction.setEnabled(false);
        Action deleteAction = new TreeDeleteAction(tree);
        deleteAction.setEnabled(false);
        Action renameAction = new TreeRenameAction(tree);
        renameAction.setEnabled(false);
        Action duplicateAction = new TreeDuplicateAction(tree);
        duplicateAction.setEnabled(false);
        Action replaceInFilesAction = new ReplaceInFilesAction(tree);
        replaceInFilesAction.setEnabled(false);
        Action addToFavoritesAction = new AddToFavoritesAction(tree);
        addToFavoritesAction.setEnabled(false);
        Action removeFromFavoritesAction = new RemoveFromFavoritesAction(tree);
        removeFromFavoritesAction.setEnabled(false);
        Action gitAddAction = new TreeGitAddAction(tree);
        gitAddAction.setEnabled(false);
        Action[] newActions = new Action[] { newFileAction, newDirectoryAction };
        Action[] actions = new Action[]{ copyAction, pasteAction, deleteAction, renameAction, duplicateAction,
                replaceInFilesAction, addToFavoritesAction, removeFromFavoritesAction };
        Action[] gitActions = new Action[] { gitAddAction };

        JPopupMenu menu = new JPopupMenu();
        for (Action action : newActions) {
            newMenu.getActionMap().put(action.getValue(Action.NAME), action);
            newMenu.add(action);
        }
        menu.add(newMenu);
        for (Action action : actions) {
            tree.getActionMap().put(action.getValue(Action.NAME), action);
            if (action.getValue(Action.ACCELERATOR_KEY) != null) {
                tree.getInputMap().put(
                        (KeyStroke) action.getValue(Action.ACCELERATOR_KEY), action.getValue(Action.NAME));
            }
            menu.add(action);
        }
        JMenu gitMenu = new JMenu("Git");
        for (Action action : gitActions) {
            gitMenu.getActionMap().put(action.getValue(Action.NAME), action);
            gitMenu.add(action);
        }
        menu.add(gitMenu);

        tree.addTreeSelectionListener(e -> {
            int selectedFiles = tree.getSelectionCount();
            boolean canAddToGit = false;
            if (selectedFiles > 0) {
                for (TreePath path : tree.getSelectionPaths()) {
                    StaticTreeNode node = (StaticTreeNode) path.getLastPathComponent();
                    if (node.getGit() != null && node.getStatus() != StaticTreeNode.NodeStatus.UNMODIFIED) {
                        canAddToGit = true;
                        break;
                    }
                }
            }
            newFileAction.setEnabled(editable && selectedFiles == 1);
            newDirectoryAction.setEnabled(editable && selectedFiles == 1);
            copyAction.setEnabled(selectedFiles > 0);
            pasteAction.setEnabled(editable && selectedFiles > 0);
            deleteAction.setEnabled(editable && selectedFiles > 0);
            renameAction.setEnabled(editable && selectedFiles > 0);
            duplicateAction.setEnabled(editable && selectedFiles > 0);
            replaceInFilesAction.setEnabled(editable && selectedFiles > 0);
            addToFavoritesAction.setEnabled(selectedFiles > 0);
            removeFromFavoritesAction.setEnabled(selectedFiles > 0);
            gitAddAction.setEnabled(editable && canAddToGit);
        });
        return menu;
    }

    public Layer getLayer() {
        return layer;
    }

    public JTree getTree() {
        return tree;
    }

    public void clearSelection() {
        tree.clearSelection();
    }

    public void addTreeSelectionListener(LayerTreeSelectionListener listener) {
        tree.addTreeSelectionListener(event -> {
            if (eventsEnabled) {
                listener.valueChanged(layer, tree, event);
            }
        });
    }

    public void suppressEvents(Runnable runnable) {
        try {
            this.eventsEnabled = false;
            runnable.run();
        } finally {
            this.eventsEnabled = true;
        }
    }

    public void reload() {
        try {
            StaticTreeNode rootNode = createRootNode(layer.getFile());
            StaticTreeNode currentRoot = (StaticTreeNode) tree.getModel().getRoot();
            rootNode.getChildren().forEach(n -> n.setParent(currentRoot));
            currentRoot.getChildren().clear();
            currentRoot.getChildren().addAll(rootNode.getChildren());
            ((DefaultTreeModel) tree.getModel()).reload();
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException(e);
            //TODO error dialog
        }
    }

    public StaticTreeNode findNode(Object[] path) {
        return TreeUtils.findNode(tree, path);
    }

    public void setSelectedNode(StaticTreeNode node) {
        TreePath treePath = TreeUtils.getPathForNode(node);
        TreePath currentPath = treePath;
        while (currentPath != null) {
            tree.expandPath(currentPath);
            currentPath = currentPath.getParentPath();
        }
        tree.setSelectionPath(treePath);
        tree.scrollPathToVisible(treePath);
    }
}
