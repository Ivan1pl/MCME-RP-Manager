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

import com.mcmiddleearth.rpmanager.events.ListDoubleClickEvent;
import com.mcmiddleearth.rpmanager.events.ListSelectionChangeEvent;
import com.mcmiddleearth.rpmanager.gui.MainWindow;
import com.mcmiddleearth.rpmanager.gui.components.FastScrollPane;
import com.mcmiddleearth.rpmanager.gui.components.tree.StaticTreeNode;
import com.mcmiddleearth.rpmanager.model.BaseModel;
import com.mcmiddleearth.rpmanager.model.BlockState;
import com.mcmiddleearth.rpmanager.model.Item;
import com.mcmiddleearth.rpmanager.model.internal.NamespacedPath;
import com.mcmiddleearth.rpmanager.model.internal.RelatedFiles;
import com.mcmiddleearth.rpmanager.model.internal.SelectedFileData;
import com.mcmiddleearth.rpmanager.model.project.Layer;
import com.mcmiddleearth.rpmanager.model.project.Project;
import com.mcmiddleearth.rpmanager.utils.ActionManager;
import com.mcmiddleearth.rpmanager.utils.FileLoader;
import com.mcmiddleearth.rpmanager.utils.ResourcePackUtils;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectPane extends JPanel {
    private final Project project;
    private final ProjectFilesPane treesPane;
    private final FileEditPane fileEditPane;
    private final RelatedFilesPane relatedFilesPane;
    private final ActionManager actionManager;

    public ProjectPane(Project project) throws IOException, GitAPIException {
        this.project = project;
        this.actionManager = new ActionManager(this::reload);

        setLayout(new BorderLayout());

        this.treesPane = new ProjectFilesPane(project);
        this.fileEditPane = new FileEditPane(actionManager);
        this.relatedFilesPane = new RelatedFilesPane();
        this.relatedFilesPane.addListDoubleClickEventListener(this::onListDoubleClick);
        treesPane.addTreeSelectionListener(this::onTreeSelectionChanged);

        JSplitPane innerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                new FastScrollPane(treesPane,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                this.fileEditPane);
        innerSplitPane.setDividerSize(10);
        innerSplitPane.setOneTouchExpandable(false);
        innerSplitPane.setResizeWeight(0.5);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                innerSplitPane,
                relatedFilesPane);
        splitPane.setDividerSize(10);
        splitPane.setOneTouchExpandable(false);
        splitPane.setResizeWeight(0.66);

        FileShortcutsPane fileShortcutsPane = new FileShortcutsPane(project);
        fileShortcutsPane.addListSelectionChangeListener(this::onShortcutListSelectionChange);

        add(fileShortcutsPane, BorderLayout.LINE_START);
        add(splitPane, BorderLayout.CENTER);
    }

    public void onTreeSelectionChanged(Layer layer, JTree tree, TreeSelectionEvent event) {
        TreePath newPath = event.getNewLeadSelectionPath();
        try {
            SelectedFileData fileData = newPath == null ? null : FileLoader.load(layer, newPath.getPath());
            fileEditPane.setSelectedFile(fileData,
                    newPath == null ? null : (StaticTreeNode) newPath.getLastPathComponent());
            fileEditPane.setCurrentTree(tree);
            updateRelatedFiles(fileData);
            updateRecentFiles(fileData);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(),
                    "Unknown error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void selectFoundBlockState(String searchString, Object[] path) {
        treesPane.setSelectedNode(path);
        SwingUtilities.invokeLater(() -> {
            fileEditPane.scrollToMatchingNodeAndExpand(searchString);
        });
    }

    private void updateRelatedFiles(SelectedFileData fileData) {
        try {
            if (fileData != null && fileData.getData() != null) {
                if (fileData.getData() instanceof BlockState blockState) {
                    RelatedFiles relatedFiles = ResourcePackUtils.getRelatedFiles(blockState, project);
                    relatedFilesPane.setRelatedFiles(relatedFiles);
                } else if (fileData.getData() instanceof Item item) {
                    RelatedFiles relatedFiles = ResourcePackUtils.getRelatedFiles(item, project);
                    relatedFilesPane.setRelatedFiles(relatedFiles);
                } else if (fileData.getData() instanceof BaseModel model) {
                    RelatedFiles relatedFiles = ResourcePackUtils.getRelatedFiles(
                            model, extractPath(fileData.getPath()), project);
                    relatedFilesPane.setRelatedFiles(relatedFiles);
                } else {
                    relatedFilesPane.setRelatedFiles(null);
                }
            } else {
                relatedFilesPane.setRelatedFiles(null);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(),
                    "Unknown error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            relatedFilesPane.setRelatedFiles(null);
        }
    }

    private static NamespacedPath extractPath(Object[] path) {
        int current = 0;
        while (!"assets".equals(((StaticTreeNode) path[current]).getName())) {
            current++;
        }
        String namespace = ((StaticTreeNode) path[++current]).getName();
        String file = Stream.of(path).skip(current + 2).map(StaticTreeNode.class::cast).map(StaticTreeNode::getName)
                .collect(Collectors.joining("/")).replaceAll("\\.json$", "");
        return new NamespacedPath(namespace, file);
    }

    private void updateRecentFiles(SelectedFileData data) {
        if (data == null) {
            return;
        }
        File recentFilesFile = project.getRecentFilesFile();
        List<String> recentFiles = null;
        if (recentFilesFile.exists()) {
            try {
                recentFiles = new ArrayList<>(Files.readAllLines(recentFilesFile.toPath()));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(MainWindow.getInstance(),
                        "Unknown error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        if (recentFiles == null) {
            recentFiles = new ArrayList<>();
        }
        String path = Stream.of(data.getPath()).map(Object::toString).collect(Collectors.joining("/"));
        recentFiles.remove(path);
        recentFiles.add(0, path);
        while (recentFiles.size() > MainWindow.getInstance().getSettings().getOpenedFileHistorySize()) {
            recentFiles.remove(recentFiles.size() - 1);
        }
        try {
            Files.write(recentFilesFile.toPath(), recentFiles);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(),
                    "Unknown error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onListDoubleClick(ListDoubleClickEvent event) {
        SelectedFileData selectedFileData = (SelectedFileData) event.getObject();
        treesPane.setSelectedNode(selectedFileData.getPath());
    }

    private void onShortcutListSelectionChange(ListSelectionChangeEvent event) {
        treesPane.setSelectedNode(Stream.of(event.getObject().toString().split("/")).toArray());
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    public void reload() {
        treesPane.reload();
    }
}
