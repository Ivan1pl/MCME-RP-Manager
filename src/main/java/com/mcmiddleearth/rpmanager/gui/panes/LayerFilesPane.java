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

import com.mcmiddleearth.rpmanager.events.ComponentSizeUpdatedEvent;
import com.mcmiddleearth.rpmanager.events.EventDispatcher;
import com.mcmiddleearth.rpmanager.events.EventListener;
import com.mcmiddleearth.rpmanager.gui.MainWindow;
import com.mcmiddleearth.rpmanager.gui.components.*;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class LayerFilesPane extends JPanel {
    private final EventDispatcher eventDispatcher = new EventDispatcher();
    private final Layer layer;
    private final JTree tree;
    private final JLabel title;
    private final VerticalLabel verticalTitle;
    private final VerticalBox verticalBox;
    private final FastScrollPane fastScrollPane;
    private boolean expanded = true;
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
        verticalBox = new VerticalBox();

        title = new JLabel(layer.getName());
        title.setFont(new Font(title.getFont().getName(), Font.BOLD, title.getFont().getSize()));
        title.addMouseListener(new MouseClickAdapter());
        verticalTitle = new VerticalLabel(layer.getName(), false);
        verticalTitle.setFont(new Font(
                verticalTitle.getFont().getName(), Font.BOLD, verticalTitle.getFont().getSize()));
        verticalTitle.addMouseListener(new MouseClickAdapter());
        JPanel toolbar = createToolbar(layer, project);

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
        add(fastScrollPane = new FastScrollPane(this.tree = createTree(layer.getFile()),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
    }

    private JPanel createToolbar(Layer layer, Project project) {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 5));

        JButton changeLocationButton = new IconButton(
                new com.mcmiddleearth.rpmanager.gui.actions.Action(
                        "Change location", Icons.FOLDER_ICON, "Change location") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (layer.getFile().getName().endsWith(".jar")) {
                            changeMinecraftVersion();
                        } else {
                            changeResourcePackLocation();
                        }
                    }
                });
        toolbar.add(changeLocationButton);

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
        return toolbar;
    }

    private void changeMinecraftVersion() {
        JFileChooser fileChooser = new JFileChooser(layer.getFile());
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("JAR file", "jar"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileHidingEnabled(false);
        fileChooser.setDialogTitle("Choose Minecraft JAR location");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            layer.setFile(fileChooser.getSelectedFile());
            reload();
        }
    }

    private void changeResourcePackLocation() {
        JFileChooser fileChooser = new JFileChooser(layer.getFile());
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Resource pack metadata file", "mcmeta"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileHidingEnabled(false);
        fileChooser.setDialogTitle("Choose resource pack location");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            layer.setFile(fileChooser.getSelectedFile());
            layer.setName(layer.getFile().getParentFile().getName());
            reload(true);
        }
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
        Action openFileLocationAction = new TreeOpenFileLocationAction(tree);
        openFileLocationAction.setEnabled(false);
        Action generateSmlScopesAction = new GenerateSmlScopesAction(tree);
        generateSmlScopesAction.setEnabled(false);
        Action gitAddAction = new TreeGitAddAction(tree);
        gitAddAction.setEnabled(false);
        Action[] newActions = new Action[] { newFileAction, newDirectoryAction };
        Action[] actions = new Action[]{ copyAction, pasteAction, deleteAction, renameAction, duplicateAction,
                replaceInFilesAction, addToFavoritesAction, removeFromFavoritesAction, openFileLocationAction,
                generateSmlScopesAction };
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
            boolean canGenerateScopes = false;
            if (selectedFiles == 1 && editable) {
                StaticTreeNode node = (StaticTreeNode) tree.getSelectionPaths()[0].getLastPathComponent();
                canGenerateScopes = !"minecraft".equals(node.getName()) && isNamespaceDir(node);
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
            openFileLocationAction.setEnabled(editable && selectedFiles == 1);
            generateSmlScopesAction.setEnabled(editable && canGenerateScopes);
            gitAddAction.setEnabled(editable && canAddToGit);
        });
        return menu;
    }

    private static boolean isNamespaceDir(StaticTreeNode node) {
        return node.getParent() != null && "assets".equals(((StaticTreeNode) node.getParent()).getName()) &&
                (node.getParent().getParent() == null || node.getParent().getParent().getParent() == null);
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
        reload(false);
    }

    private void reload(boolean changeTitle) {
        try {
            StaticTreeNode rootNode = createRootNode(layer.getFile());
            StaticTreeNode currentRoot = (StaticTreeNode) tree.getModel().getRoot();
            rootNode.getChildren().forEach(n -> n.setParent(currentRoot));
            currentRoot.getChildren().clear();
            currentRoot.getChildren().addAll(rootNode.getChildren());
            currentRoot.setName(rootNode.getName());
            currentRoot.setGit(rootNode.getGit());
            ((DefaultTreeModel) tree.getModel()).reload();
            if (changeTitle) {
                title.setText(layer.getName());
            }
            invalidate();
            repaint();
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

    private class MouseClickAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if ((e.getSource() == title || e.getSource() == verticalTitle) && e.getButton() == MouseEvent.BUTTON1) {
                expanded = !expanded;
                updateState();
            }
        }
    }

    private void updateState() {
        removeAll();
        if (expanded) {
            add(verticalBox, BorderLayout.PAGE_START);
            add(fastScrollPane, BorderLayout.CENTER);
        } else {
            add(verticalTitle, BorderLayout.PAGE_START);
        }
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> eventDispatcher.dispatchEvent(
                new ComponentSizeUpdatedEvent(this, getMinimumSize())));
    }

    public void addComponentSizeUpdatedListener(EventListener<ComponentSizeUpdatedEvent> listener) {
        eventDispatcher.addEventListener(listener, ComponentSizeUpdatedEvent.class);
    }

    public boolean isExpanded() {
        return expanded;
    }
}
