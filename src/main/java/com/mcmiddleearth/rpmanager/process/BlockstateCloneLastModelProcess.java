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

package com.mcmiddleearth.rpmanager.process;

import com.mcmiddleearth.rpmanager.gui.MainWindow;
import com.mcmiddleearth.rpmanager.gui.modals.BlockstateCloneLastModelSelectNewNamesModal;
import com.mcmiddleearth.rpmanager.model.BlockModel;
import com.mcmiddleearth.rpmanager.model.Model;
import com.mcmiddleearth.rpmanager.model.internal.LayerRelatedFiles;
import com.mcmiddleearth.rpmanager.model.internal.NamespacedPath;
import com.mcmiddleearth.rpmanager.utils.Action;
import com.mcmiddleearth.rpmanager.utils.GsonProvider;
import com.mcmiddleearth.rpmanager.utils.ResourcePackUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockstateCloneLastModelProcess extends Process<BlockstateCloneLastModelProcess.Result> {
    private static final Pattern INCREMENTABLE_NAME_PATTERN = Pattern.compile("^(.*_)(\\d+)$");
    private final List<Model> models;
    private final Model modelToClone;
    private final String layerName;

    public BlockstateCloneLastModelProcess(Consumer<Result> onProcessCompleted,
                                           List<Model> models, Model modelToClone, String layerName) {
        super(onProcessCompleted);
        this.models = models;
        this.modelToClone = modelToClone;
        this.layerName = layerName;
    }

    @Override
    public void start() throws Exception {
        String oldName = modelToClone.getModel();
        Matcher matcher = INCREMENTABLE_NAME_PATTERN.matcher(oldName);
        String newName = oldName;
        if (matcher.matches()) {
            newName = matcher.group(1) + (Integer.parseInt(matcher.group(2)) + 1);
        }
        boolean promptNeeded = oldName.equals(newName);

        NamespacedPath oldNamespacedName = ResourcePackUtils.extractPrefix(oldName);
        BlockModel blockModel = null;
        for (LayerRelatedFiles layerRelatedFiles : ResourcePackUtils.getModels(
                Collections.singletonList(oldNamespacedName), MainWindow.getInstance().getCurrentProject())) {
            if (!layerRelatedFiles.getRelatedFiles().isEmpty()) {
                blockModel = (BlockModel) layerRelatedFiles.getRelatedFiles().get(0).getData();
            }
            if (layerRelatedFiles.getLayerName().equals(layerName)) {
                break;
            }
        }
        if (blockModel == null) {
            complete(null);
            return;
        }
        List<String> textureOldNames = blockModel.getTextures() == null ?
                Collections.emptyList() : blockModel.getTextures().values().stream().distinct().toList();
        List<String> textureNewNames = new ArrayList<>(textureOldNames.size());
        for (String textureOldName : textureOldNames) {
            Matcher textureMatcher = INCREMENTABLE_NAME_PATTERN.matcher(textureOldName);
            String textureNewName = textureOldName;
            if (textureMatcher.matches()) {
                textureNewName = textureMatcher.group(1) + (Integer.parseInt(textureMatcher.group(2)) + 1);
            }
            textureNewNames.add(textureNewName);
            promptNeeded = promptNeeded || textureOldName.equals(textureNewName);
        }

        if (promptNeeded) {
            prompt(blockModel, oldName, newName, textureOldNames, textureNewNames);
        } else {
            proceed(blockModel, newName, textureOldNames, textureNewNames);
        }
    }

    private void prompt(BlockModel blockModel, String oldName, String newName, List<String> textureOldNames,
                        List<String> textureNewNames) {
        new BlockstateCloneLastModelSelectNewNamesModal(
                MainWindow.getInstance(), oldName, newName, textureOldNames, textureNewNames,
                newNames -> {
                    if (newNames == null) {
                        complete(null);
                    } else {
                        try {
                            proceed(blockModel, newNames.getNewName(), textureOldNames,
                                    newNames.getNewTextureNames());
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(MainWindow.getInstance(),
                                    "Unknown error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            complete(null);
                        }
                    }
                });
    }

    private void proceed(BlockModel blockModel, String newName, List<String> oldTextureNames,
                         List<String> newTextureNames) throws IOException {
        Model newModel = cloneModel(newName);
        BlockModel newBlockModel = cloneBlockModel(blockModel, oldTextureNames, newTextureNames);
        File newBlockModelFile = getModelFile(newName);

        Action redoAction = () -> {
            ResourcePackUtils.saveFile(newBlockModel, newBlockModelFile);
            models.add(newModel);
        };
        Action undoAction = () -> models.remove(models.size() - 1);
        if (newBlockModelFile.exists()) {
            byte[] contentToRestore = Files.readAllBytes(newBlockModelFile.toPath());
            undoAction = undoAction.butFirst(
                    () -> Files.write(newBlockModelFile.toPath(), contentToRestore, StandardOpenOption.WRITE));
        } else {
            undoAction = undoAction.butFirst(() -> newBlockModelFile.delete());
        }

        File parent = newBlockModelFile.getParentFile();
        while (!parent.exists()) {
            File file = parent;
            redoAction = redoAction.butFirst(file::mkdir);
            undoAction = undoAction.then(file::delete);
            parent = parent.getParentFile();
        }

        for (int i = 0; i < oldTextureNames.size(); ++i) {
            if (!oldTextureNames.get(i).equals(newTextureNames.get(i))) {
                File newTextureFile = getTextureFile(newTextureNames.get(i));
                if (!newTextureFile.exists()) {
                    BufferedImage oldTexture = null;
                    for (LayerRelatedFiles layerRelatedFiles : ResourcePackUtils.getTextures(
                            Collections.singletonList(ResourcePackUtils.extractPrefix(oldTextureNames.get(0))),
                            MainWindow.getInstance().getCurrentProject())) {
                        if (!layerRelatedFiles.getRelatedFiles().isEmpty()) {
                            oldTexture = (BufferedImage) layerRelatedFiles.getRelatedFiles().get(0).getData();
                        }
                        if (layerRelatedFiles.getLayerName().equals(layerName)) {
                            break;
                        }
                    }
                    if (oldTexture == null) {
                        complete(null);
                        return;
                    }

                    BufferedImage texture = oldTexture;
                    Action textureRedoAction = () -> ImageIO.write(texture, "png", newTextureFile);
                    Action textureUndoAction = () -> newTextureFile.delete();

                    File textureParent = newTextureFile.getParentFile();
                    while (!textureParent.exists()) {
                        File file = textureParent;
                        textureRedoAction = textureRedoAction.butFirst(file::mkdir);
                        textureUndoAction = textureUndoAction.then(file::delete);
                        textureParent = textureParent.getParentFile();
                    }

                    redoAction = redoAction.butFirst(textureRedoAction);
                    undoAction = undoAction.then(textureUndoAction);
                }
            }
        }
        complete(new Result(redoAction, undoAction));
    }

    private Model cloneModel(String newName) {
        Model newModel = new Model();
        newModel.setModel(newName);
        newModel.setUvlock(modelToClone.getUvlock());
        newModel.setWeight(modelToClone.getWeight());
        newModel.setX(modelToClone.getX());
        newModel.setY(modelToClone.getY());
        return newModel;
    }

    private BlockModel cloneBlockModel(BlockModel blockModel, List<String> oldTextureNames,
                                       List<String> newTextureNames) {
        BlockModel newModel = GsonProvider.getGson().fromJson(
                GsonProvider.getGson().toJson(blockModel), BlockModel.class);
        for (int i = 0; i < oldTextureNames.size(); ++i) {
            replaceValue(newModel.getTextures(), oldTextureNames.get(i), newTextureNames.get(i));
        }
        return newModel;
    }

    private void replaceValue(Map<String, String> textures, String oldName, String newName) {
        textures.replaceAll((key, value) -> value.equals(oldName) ? newName : value);
    }

    private File getModelFile(String name) {
        NamespacedPath namespacedPath = ResourcePackUtils.extractPrefix(name);
        File modelsFile = getChildFile(MainWindow.getInstance().getCurrentLayer().getFile().getParentFile(),
                "assets", namespacedPath.namespace(), "models");
        return getChildFile(modelsFile, (namespacedPath.path() + ".json").split("/"));
    }

    private File getTextureFile(String name) {
        NamespacedPath namespacedPath = ResourcePackUtils.extractPrefix(name);
        File texturesFile = getChildFile(MainWindow.getInstance().getCurrentLayer().getFile().getParentFile(),
                "assets", namespacedPath.namespace(), "textures");
        return getChildFile(texturesFile, (namespacedPath.path() + ".png").split("/"));
    }

    private File getChildFile(File file, String... path) {
        for (String string : path) {
            file = new File(file, string);
        }
        return file;
    }

    public static class Result {
        private final Action redoAction;
        private final Action undoAction;

        public Result(Action redoAction, Action undoAction) {
            this.redoAction = redoAction;
            this.undoAction = undoAction;
        }

        public Action getRedoAction() {
            return redoAction;
        }

        public Action getUndoAction() {
            return undoAction;
        }
    }
}
