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

import com.mcmiddleearth.rpmanager.events.ChangeEvent;
import com.mcmiddleearth.rpmanager.gui.MainWindow;
import com.mcmiddleearth.rpmanager.gui.components.FastScrollPane;
import com.mcmiddleearth.rpmanager.gui.components.tree.StaticTreeNode;
import com.mcmiddleearth.rpmanager.gui.modals.FileEditModal;
import com.mcmiddleearth.rpmanager.model.BlockModel;
import com.mcmiddleearth.rpmanager.model.BlockState;
import com.mcmiddleearth.rpmanager.model.Item;
import com.mcmiddleearth.rpmanager.model.ItemModel;
import com.mcmiddleearth.rpmanager.model.internal.SelectedFileData;
import com.mcmiddleearth.rpmanager.utils.Action;
import com.mcmiddleearth.rpmanager.utils.ActionManager;
import com.mcmiddleearth.rpmanager.utils.GsonProvider;
import com.mcmiddleearth.rpmanager.utils.TextAreaUtils;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class FileEditPane extends JPanel {
    private final JPanel editPane;
    private final JTextArea previewArea;
    private final JButton previewEditButton;
    private Class<?> previewType = null;
    private StaticTreeNode currentNode = null;
    private JTree currentTree = null;
    private final ActionManager actionManager;

    public FileEditPane(ActionManager actionManager) {
        this.actionManager = actionManager;
        setLayout(new BorderLayout());

        this.editPane = new JPanel();
        this.editPane.setLayout(new BorderLayout());

        this.previewArea = new JTextArea(0, 0);
        this.previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        this.previewArea.setEditable(false);
        ((DefaultCaret) this.previewArea.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JPanel previewPane = new JPanel();
        previewPane.setLayout(new BorderLayout());
        previewEditButton = new JButton(new com.mcmiddleearth.rpmanager.gui.actions.Action(
                "Edit", "Manually edit this file") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (!previewArea.getText().isEmpty()) {
                    new FileEditModal(MainWindow.getInstance(), previewArea.getText(), previewType, s -> {
                        updatePreview(s);
                        updateFile(s);
                        actionManager.refresh();
                    });
                }
            }
        });
        previewPane.add(previewArea, BorderLayout.CENTER);

        JPanel previewOuterPane = new JPanel();
        previewOuterPane.setLayout(new BorderLayout());
        previewOuterPane.add(new FastScrollPane(previewPane), BorderLayout.CENTER);
        previewOuterPane.add(previewEditButton, BorderLayout.PAGE_START);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
                editPane,
                previewOuterPane);
        splitPane.setDividerSize(10);
        splitPane.setOneTouchExpandable(false);
        splitPane.setResizeWeight(0.5);

        add(splitPane, BorderLayout.CENTER);
    }

    public void setSelectedFile(SelectedFileData fileData, StaticTreeNode node) {
        this.currentNode = null;
        setData(fileData, node == null ? null : node.getFile());
        this.currentNode = node;
    }

    public void setCurrentTree(JTree currentTree) {
        this.currentTree = currentTree;
    }

    public void scrollToMatchingNodeAndExpand(String searchString) {
        if (editPane.getComponentCount() > 0 && editPane.getComponent(0) instanceof JScrollPane scrollPane &&
                scrollPane.getViewport() != null &&
                scrollPane.getViewport().getView() instanceof BlockstateFileEditPane blockstateFileEditPane) {
            String previewPart = blockstateFileEditPane.scrollToMatchingNodeAndExpand(searchString);
            if (previewPart != null) {
                SwingUtilities.invokeLater(
                        () -> TextAreaUtils.scrollTextAreaIgnoreWhitespaces(previewArea, previewPart));
            }
        }
    }

    private void setData(SelectedFileData data, File file) {
        editPane.removeAll();
        if (data == null) {
            updatePreview("", String.class, false);
            JLabel label = new JLabel("No file selected, or no editor available for selected file.");
            editPane.add(label, BorderLayout.CENTER);
        } else if (data.getData() instanceof BlockState blockState) {
            updatePreview(GsonProvider.getGson().toJson(data.getData()), BlockState.class);
            BlockstateFileEditPane blockstateFileEditPane = new BlockstateFileEditPane(data.getName(), blockState);
            blockstateFileEditPane.addChangeListener(this::onChange);
            JScrollPane scrollPane = new FastScrollPane(blockstateFileEditPane);
            editPane.add(scrollPane, BorderLayout.CENTER);
        } else if (data.getData() instanceof Item item) {
            updatePreview(GsonProvider.getGson().toJson(data.getData()), Item.class);
            ItemFileEditPane itemFileEditPane = new ItemFileEditPane(item);
            itemFileEditPane.addChangeListener(this::onChange);
            JScrollPane scrollPane = new FastScrollPane(itemFileEditPane);
            editPane.add(scrollPane, BorderLayout.CENTER);
        } else if (data.getData() instanceof BlockModel blockModel) {
            updatePreview(GsonProvider.getGson().toJson(data.getData()), BlockModel.class);
            BlockModelFileEditPane blockModelFileEditPane = new BlockModelFileEditPane(data.getName(), blockModel);
            blockModelFileEditPane.addChangeListener(this::onChange);
            JScrollPane scrollPane = new FastScrollPane(blockModelFileEditPane);
            editPane.add(scrollPane, BorderLayout.CENTER);
        } else if (data.getData() instanceof ItemModel itemModel) {
            updatePreview(GsonProvider.getGson().toJson(data.getData()), ItemModel.class);
            ItemModelFileEditPane itemModelFileEditPane = new ItemModelFileEditPane(data.getName(), itemModel);
            itemModelFileEditPane.addChangeListener(this::onChange);
            JScrollPane scrollPane = new FastScrollPane(itemModelFileEditPane);
            editPane.add(scrollPane, BorderLayout.CENTER);
        } else if (data.getData() instanceof BufferedImage bufferedImage) {
            updatePreview("", BufferedImage.class);
            TextureFileEditPane textureFileEditPane = new TextureFileEditPane(file, bufferedImage);
            JScrollPane scrollPane = new FastScrollPane(textureFileEditPane);
            editPane.add(scrollPane, BorderLayout.CENTER);
        } else if (data.getData() instanceof String string) {
            updatePreview(string, String.class);
            JLabel label = new JLabel("No editor available for selected file.");
            editPane.add(label, BorderLayout.CENTER);
        } else {
            updatePreview("", String.class, false);
            JLabel label = new JLabel("No file selected, or no editor available for selected file.");
            editPane.add(label, BorderLayout.CENTER);
        }
        editPane.revalidate();
        editPane.repaint();
    }

    private void onChange(ChangeEvent changeEvent) {
        String newContent = GsonProvider.getGson().toJson(changeEvent.getObject());
        updatePreview(newContent);
        updateFile(newContent);
    }

    private void updatePreview(String text) {
        updatePreview(text, previewType);
    }

    private void updatePreview(String text, Class<?> type) {
        updatePreview(text, type, true);
    }

    private void updatePreview(String text, Class<?> type, boolean editable) {
        previewArea.setText(text);
        previewType = type;
        previewEditButton.setEnabled(editable);
    }

    private void updateFile(String text) {
        if (currentNode != null) {
            File file = this.currentNode.getFile();
            Action undoAction = null;
            Action redoAction = null;
            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] content = inputStream.readAllBytes();
                undoAction = () -> {
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        outputStream.write(content);
                    }
                };
                redoAction = () -> {
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
                    }
                };
            } catch (IOException e) {
                //TODO display error dialog
            }
            if (undoAction != null && redoAction != null) {
                actionManager.submit(undoAction, redoAction);
            }
            StaticTreeNode nodeToRefresh = currentNode;
            if (!nodeToRefresh.isDirectory()) {
                nodeToRefresh = (StaticTreeNode) nodeToRefresh.getParent();
            }
            try {
                nodeToRefresh.refreshGitStatus();
            } catch (GitAPIException e) {
                //TODO display error dialog?
            }
            currentTree.invalidate();
            currentTree.repaint();
        }
    }
}
