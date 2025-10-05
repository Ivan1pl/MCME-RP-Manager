/*
 * Copyright (C) 2021 MCME
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

package com.mcmiddleearth.rpmanager.utils;

import com.mcmiddleearth.rpmanager.model.*;
import com.mcmiddleearth.rpmanager.model.internal.LayerRelatedFiles;
import com.mcmiddleearth.rpmanager.model.internal.NamespacedPath;
import com.mcmiddleearth.rpmanager.model.internal.RelatedFiles;
import com.mcmiddleearth.rpmanager.model.internal.SelectedFileData;
import com.mcmiddleearth.rpmanager.model.item.CompositeItemsModel;
import com.mcmiddleearth.rpmanager.model.item.ConditionItemsModel;
import com.mcmiddleearth.rpmanager.model.item.ItemsModel;
import com.mcmiddleearth.rpmanager.model.item.ModelItemsModel;
import com.mcmiddleearth.rpmanager.model.item.RangeDispatchItemsModel;
import com.mcmiddleearth.rpmanager.model.item.SelectItemsModel;
import com.mcmiddleearth.rpmanager.model.project.Layer;
import com.mcmiddleearth.rpmanager.model.project.Project;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ResourcePackUtils {
    public static final String DEFAULT_NAMESPACE = "minecraft";
    private static final Function<String, String[]> BLOCK_STATE_DIR_PATH =
            namespace -> new String[] { "assets", namespace, "blockstates" };
    private static final Function<String, String[]> MODEL_DIR_PATH =
            namespace -> new String[] { "assets", namespace, "models" };
    private static final Function<String, String[]> TEXTURES_DIR_PATH =
            namespace -> new String[] { "assets", namespace, "textures" };
    private static final Pattern BLOCK_STATE_NAME_PATTERN =
            Pattern.compile("^(?:(?<namespace>[^:]+):)?(?<path>[^\\[#]+)(?:\\[[^]]+])?$");
    private static final Pattern BLOCK_STATE_NAME_PATTERN2 =
            Pattern.compile("^(?:(?<namespace>[^:]+):)?(?<path>[^#]+)#(?:.*)?$");
    private static final Pattern NAMESPACED_PATH_PATTERN =
            Pattern.compile("^(?:(?<namespace>[^:]+):)?(?<path>.+)");
    private static final Pattern BLOCKSTATE_FILE_PATTERN = Pattern.compile("^assets/[^/]+/blockstates/.*\\.json$");
    private static final Pattern ITEM_FILE_PATTERN = Pattern.compile("^assets/[^/]+/items/.*\\.json$");

    private ResourcePackUtils() {}

    public static void saveFile(Object data, File target) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(target)) {
            fileOutputStream.write(GsonProvider.getGson().toJson(data).getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void compileProject(Project project, File target) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(target))) {}
        try (FileSystem fs = FileSystems.newFileSystem(target.toPath())) {
            for (int i = 1; i < project.getLayers().size(); ++i) {
                com.mcmiddleearth.rpmanager.model.project.Layer layer = project.getLayers().get(i);
                File layerDir = layer.getFile().getParentFile();
                recursiveCopy(layerDir, fs);
            }
        }
    }

    private static void recursiveCopy(File dir, FileSystem fileSystem) throws IOException {
        recursiveCopy(dir, dir.toPath(), fileSystem);
    }

    private static void recursiveCopy(File dir, Path basePath, FileSystem fs) throws IOException {
        for (File f : dir.listFiles()) {
            Path path = basePath.relativize(f.toPath());
            Path target = fs.getPath(path.toString());
            if (f.isDirectory()) {
                if (!".git".equals(f.getName())) { //skip .git directory, TODO: make this configurable
                    if (!Files.exists(target)) {
                        Files.createDirectory(target);
                    }
                    recursiveCopy(f, basePath, fs);
                }
            } else {
                Files.copy(f.toPath(), target, REPLACE_EXISTING);
            }
        }
    }

    public static RelatedFiles getRelatedFiles(BlockState blockState, Project project) throws IOException {
        List<LayerRelatedFiles> relatedModels = getRelatedModels(blockState, project);
        List<LayerRelatedFiles> relatedTextures = getRelatedTextures(relatedModels, project);
        return new RelatedFiles(relatedModels, relatedTextures, Collections.emptyList(), Collections.emptyList());
    }

    public static RelatedFiles getRelatedFiles(Item item, Project project) throws IOException {
        List<LayerRelatedFiles> relatedModels = getRelatedModels(item, project);
        List<LayerRelatedFiles> relatedTextures = getRelatedTextures(relatedModels, project);
        return new RelatedFiles(relatedModels, relatedTextures, Collections.emptyList(), Collections.emptyList());
    }

    public static RelatedFiles getRelatedFiles(BaseModel model, NamespacedPath path, Project project)
            throws IOException {
        List<NamespacedPath> models = model.getParent() == null ?
                Collections.emptyList() :
                Stream.of(model.getParent()).map(ResourcePackUtils::extractPrefix)
                        .map(s -> s.path().contains("/") ?
                                s :
                                new NamespacedPath(s.namespace(),
                                        model instanceof BlockModel ? "block/" + s.path() : "item/" + s.path()))
                        .toList();
        List<LayerRelatedFiles> relatedModels = getModels(models, project);
        List<LayerRelatedFiles> relatedTextures = getRelatedTextures(
                Stream.concat(
                        Stream.of(new LayerRelatedFiles(null,
                                Collections.singletonList(new SelectedFileData(model, "", new Object[0])))),
                        relatedModels.stream())
                        .toList(),
                project);
        List<LayerRelatedFiles> relatedBlockstates = getBlockStatesContainingModel(path, project);
        List<LayerRelatedFiles> relatedItems = getItemsContainingModel(path, project);
        return new RelatedFiles(relatedModels, relatedTextures, relatedBlockstates, relatedItems);
    }

    public static List<LayerRelatedFiles> getMatchingBlockStates(Project project, String blockState)
            throws IOException {
        List<LayerRelatedFiles> result = new LinkedList<>();
        Matcher matcher = BLOCK_STATE_NAME_PATTERN.matcher(blockState);
        Matcher matcher2 = BLOCK_STATE_NAME_PATTERN2.matcher(blockState);
        Matcher matching = null;
        if (matcher.matches()) {
            matching = matcher;
        } else if (matcher2.matches()) {
            matching = matcher2;
        }
        if (matching != null) {
            String namespace = matching.group("namespace");
            String name = matching.group("path");
            if (namespace == null) {
                namespace = DEFAULT_NAMESPACE;
            }
            Object[] path = Stream.concat(
                    Stream.of(BLOCK_STATE_DIR_PATH.apply(namespace)), Stream.of((name + ".json").split("/")))
                    .toArray();
            for (Layer layer : project.getLayers()) {
                if (containsFile(layer, path)) {
                    SelectedFileData selectedFileData = FileLoader.load(layer, Stream.concat(
                            layer.getFile().getName().endsWith(".jar") ? Stream.empty() : Stream.of(layer.getName()),
                            Stream.of(path)).toArray());
                    result.add(new LayerRelatedFiles(layer.getName(), Collections.singletonList(selectedFileData)));
                }
            }
        }
        return result;
    }

    private static List<LayerRelatedFiles> getBlockStatesContainingModel(NamespacedPath model, Project project)
            throws IOException {
        List<LayerRelatedFiles> result = new LinkedList<>();
        for (Layer layer : project.getLayers()) {
            if (!layer.getFile().getName().endsWith(".jar")) {
                result.add(getBlockStatesContainingModel(layer, layer.getFile().getParentFile(), model));
            } else {
                result.add(zipGetBlockStatesContainingModel(layer, layer.getFile(), model));
            }
        }
        return result;
    }

    private static LayerRelatedFiles getBlockStatesContainingModel(Layer layer, File dir, NamespacedPath model)
            throws IOException {
        LayerRelatedFiles layerRelatedFiles = new LayerRelatedFiles(layer.getName(), new LinkedList<>());
        File assetsDir = new File(dir, "assets");
        if (assetsDir.exists()) {
            for (File namespaceDir : assetsDir.listFiles()) {
                if (namespaceDir.isDirectory()) {
                    File blockstatesDir = new File(namespaceDir, "blockstates");
                    if (blockstatesDir.exists() && blockstatesDir.isDirectory()) {
                        for (File blockstateFile : blockstatesDir.listFiles()) {
                            SelectedFileData selectedFileData = FileLoader.load(layer, Stream.concat(
                                    layer.getFile().getName().endsWith(".jar") ? Stream.empty() : Stream.of(layer.getName()),
                                    Stream.of("assets", namespaceDir.getName(), "blockstates",
                                            blockstateFile.getName()))
                                    .toArray());
                            if (selectedFileData != null) {
                                BlockState blockState = (BlockState) selectedFileData.getData();
                                if (containsModel(blockState, model)) {
                                    layerRelatedFiles.getRelatedFiles().add(selectedFileData);
                                }
                            }
                        }
                    }
                }
            }
        }
        return layerRelatedFiles;
    }

    private static LayerRelatedFiles zipGetBlockStatesContainingModel(Layer layer, File zip, NamespacedPath model)
            throws IOException {
        LayerRelatedFiles layerRelatedFiles = new LayerRelatedFiles(layer.getName(), new LinkedList<>());
        try (ZipFile zipFile = new ZipFile(zip)) {
            zipFile.stream().forEach(e -> {
                Matcher m = BLOCKSTATE_FILE_PATTERN.matcher(e.getName());
                if (m.matches()) {
                    try {
                        SelectedFileData selectedFileData = FileLoader.load(
                                layer, Stream.of(e.getName().split("/")).toArray());
                        if (selectedFileData != null) {
                            BlockState blockState = (BlockState) selectedFileData.getData();
                            if (containsModel(blockState, model)) {
                                layerRelatedFiles.getRelatedFiles().add(selectedFileData);
                            }
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }
        return layerRelatedFiles;
    }

    private static List<LayerRelatedFiles> getItemsContainingModel(NamespacedPath model, Project project)
            throws IOException {
        List<LayerRelatedFiles> result = new LinkedList<>();
        for (Layer layer : project.getLayers()) {
            if (!layer.getFile().getName().endsWith(".jar")) {
                result.add(getItemsContainingModel(layer, layer.getFile().getParentFile(), model));
            } else {
                result.add(zipGetItemsContainingModel(layer, layer.getFile(), model));
            }
        }
        return result;
    }

    private static LayerRelatedFiles getItemsContainingModel(Layer layer, File dir, NamespacedPath model)
            throws IOException {
        LayerRelatedFiles layerRelatedFiles = new LayerRelatedFiles(layer.getName(), new LinkedList<>());
        File assetsDir = new File(dir, "assets");
        if (assetsDir.exists()) {
            for (File namespaceDir : assetsDir.listFiles()) {
                if (namespaceDir.isDirectory()) {
                    File itemsDir = new File(namespaceDir, "items");
                    if (itemsDir.exists() && itemsDir.isDirectory()) {
                        for (File blockstateFile : itemsDir.listFiles()) {
                            SelectedFileData selectedFileData = FileLoader.load(layer, Stream.concat(
                                            layer.getFile().getName().endsWith(".jar") ? Stream.empty() : Stream.of(layer.getName()),
                                            Stream.of("assets", namespaceDir.getName(), "items",
                                                    blockstateFile.getName()))
                                    .toArray());
                            if (selectedFileData != null) {
                                Item item = (Item) selectedFileData.getData();
                                if (containsModel(item, model)) {
                                    layerRelatedFiles.getRelatedFiles().add(selectedFileData);
                                }
                            }
                        }
                    }
                }
            }
        }
        return layerRelatedFiles;
    }

    private static LayerRelatedFiles zipGetItemsContainingModel(Layer layer, File zip, NamespacedPath model)
            throws IOException {
        LayerRelatedFiles layerRelatedFiles = new LayerRelatedFiles(layer.getName(), new LinkedList<>());
        try (ZipFile zipFile = new ZipFile(zip)) {
            zipFile.stream().forEach(e -> {
                Matcher m = ITEM_FILE_PATTERN.matcher(e.getName());
                if (m.matches()) {
                    try {
                        SelectedFileData selectedFileData = FileLoader.load(
                                layer, Stream.of(e.getName().split("/")).toArray());
                        if (selectedFileData != null) {
                            Item item = (Item) selectedFileData.getData();
                            if (containsModel(item, model)) {
                                layerRelatedFiles.getRelatedFiles().add(selectedFileData);
                            }
                        }
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
        }
        return layerRelatedFiles;
    }

    private static boolean containsModel(BlockState blockState, NamespacedPath model) {
        if (blockState.getVariants() != null) {
            return blockState.getVariants().values().stream().flatMap(Collection::stream)
                    .anyMatch(m -> model.equals(extractPrefix(m.getModel())));
        } else if (blockState.getMultipart() != null) {
            return blockState.getMultipart().stream().flatMap(c -> c.getApply().stream())
                    .anyMatch(m -> model.equals(extractPrefix(m.getModel())));
        } else {
            return false;
        }
    }

    private static boolean containsModel(Item item, NamespacedPath model) {
        return containsModel(item.getModel(), model);
    }

    private static boolean containsModel(ItemsModel itemsModel, NamespacedPath model) {
        if (itemsModel instanceof CompositeItemsModel compositeItemsModel) {
            return compositeItemsModel.getModels().stream().anyMatch(m -> containsModel(m, model));
        } else if (itemsModel instanceof ConditionItemsModel conditionItemsModel) {
            return containsModel(conditionItemsModel.getOnFalse(), model) ||
                    containsModel(conditionItemsModel.getOnTrue(), model);
        } else if (itemsModel instanceof ModelItemsModel modelItemsModel) {
            return model.equals(extractPrefix(modelItemsModel.getModel()));
        } else if (itemsModel instanceof RangeDispatchItemsModel rangeDispatchItemsModel) {
            return rangeDispatchItemsModel.getEntries().stream().map(RangeDispatchItemsModel.Entry::getModel)
                    .anyMatch(m -> containsModel(m, model)) ||
                    containsModel(rangeDispatchItemsModel.getFallback(), model);
        } else if (itemsModel instanceof SelectItemsModel selectItemsModel) {
            return selectItemsModel.getCases().stream().map(SelectItemsModel.Case::getModel)
                    .anyMatch(m -> containsModel(m, model)) ||
                    containsModel(selectItemsModel.getFallback(), model);
        } else {
            return false;
        }
    }

    private static List<LayerRelatedFiles> getRelatedModels(BlockState blockState, Project project) throws IOException {
        List<NamespacedPath> models = Optional.ofNullable(blockState.getVariants())
                .map(v -> v.values().stream().flatMap(l -> l.stream().map(Model::getModel)))
                .orElseGet(() -> blockState.getMultipart().stream().flatMap(
                        c -> c.getApply().stream().map(Model::getModel)))
                .distinct()
                .map(ResourcePackUtils::extractPrefix)
                .map(s -> s.path().contains("/") ?
                        s : new NamespacedPath(s.namespace(), "block/" + s.path()))
                .toList();
        return getModels(models, project);
    }

    private static List<LayerRelatedFiles> getRelatedModels(Item item, Project project) throws IOException {
        List<NamespacedPath> models = getRelatedModelsStr(item.getModel()).stream()
                .distinct()
                .map(ResourcePackUtils::extractPrefix)
                .map(s -> s.path().contains("/") ?
                        s : new NamespacedPath(s.namespace(), "item/" + s.path()))
                .toList();
        return getModels(models, project);
    }

    private static List<String> getRelatedModelsStr(ItemsModel itemsModel) throws IOException {
        List<String> result = new LinkedList<>();
        if (itemsModel instanceof ModelItemsModel modelItemsModel) {
            if (modelItemsModel.getModel() != null && !modelItemsModel.getModel().isEmpty()) {
                result.add(modelItemsModel.getModel());
            }
        } else if (itemsModel instanceof CompositeItemsModel compositeItemsModel) {
            if (compositeItemsModel.getModels() != null) {
                for (ItemsModel model : compositeItemsModel.getModels()) {
                    result.addAll(getRelatedModelsStr(model));
                }
            }
        } else if (itemsModel instanceof ConditionItemsModel conditionItemsModel) {
            result.addAll(getRelatedModelsStr(conditionItemsModel.getOnTrue()));
            result.addAll(getRelatedModelsStr(conditionItemsModel.getOnFalse()));
        } else if (itemsModel instanceof RangeDispatchItemsModel rangeDispatchItemsModel) {
            if (rangeDispatchItemsModel.getEntries() != null) {
                for (RangeDispatchItemsModel.Entry entry : rangeDispatchItemsModel.getEntries()) {
                    result.addAll(getRelatedModelsStr(entry.getModel()));
                }
            }
            result.addAll(getRelatedModelsStr(rangeDispatchItemsModel.getFallback()));
        } else if (itemsModel instanceof SelectItemsModel selectItemsModel) {
            if (selectItemsModel.getCases() != null) {
                for (SelectItemsModel.Case c : selectItemsModel.getCases()) {
                    result.addAll(getRelatedModelsStr(c.getModel()));
                }
                result.addAll(getRelatedModelsStr(selectItemsModel.getFallback()));
            }
        }
        return result;
    }

    public static List<LayerRelatedFiles> getModels(List<NamespacedPath> models, Project project) throws IOException {
        List<LayerRelatedFiles> result = new LinkedList<>();
        for (com.mcmiddleearth.rpmanager.model.project.Layer layer : project.getLayers()) {
            List<SelectedFileData> layerModels = new LinkedList<>();
            for (NamespacedPath model : models) {
                Object[] path = Stream.concat(
                        Stream.of(MODEL_DIR_PATH.apply(model.namespace())),
                                Stream.of((model.path() + ".json").split("/")))
                        .toArray();
                if (containsFile(layer, path)) {
                    layerModels.add(FileLoader.load(layer, Stream.concat(
                            layer.getFile().getName().endsWith(".jar") ? Stream.empty() : Stream.of(layer.getName()),
                            Stream.of(path)).toArray()));
                }
            }
            if (!layerModels.isEmpty()) {
                result.add(new LayerRelatedFiles(layer.getName(), layerModels));
            }
        }
        return result;
    }

    private static List<LayerRelatedFiles> getRelatedTextures(List<LayerRelatedFiles> models, Project project)
            throws IOException {
        List<NamespacedPath> textures = models.stream()
                .flatMap(files -> files.getRelatedFiles().stream())
                .flatMap(fileData -> Optional.ofNullable(((BaseModel) fileData.getData()).getTextures()).stream()
                        .flatMap(t -> t.values().stream()))
                .map(ResourcePackUtils::extractPrefix)
                .distinct()
                .toList();
        return getTextures(textures, project);
    }

    public static List<LayerRelatedFiles> getTextures(List<NamespacedPath> textures, Project project)
            throws IOException {
        List<LayerRelatedFiles> result = new LinkedList<>();
        for (com.mcmiddleearth.rpmanager.model.project.Layer layer : project.getLayers()) {
            List<SelectedFileData> layerTextures = new LinkedList<>();
            for (NamespacedPath texture : textures) {
                Object[] path = Stream.concat(
                        Stream.of(TEXTURES_DIR_PATH.apply(texture.namespace())),
                                Stream.of((texture.path() + ".png").split("/")))
                        .toArray();
                if (containsFile(layer, path)) {
                    layerTextures.add(FileLoader.load(layer, Stream.concat(
                            layer.getFile().getName().endsWith(".jar") ? Stream.empty() : Stream.of(layer.getName()),
                            Stream.of(path)).toArray()));
                }
            }
            if (!layerTextures.isEmpty()) {
                result.add(new LayerRelatedFiles(layer.getName(), layerTextures));
            }
        }
        return result;
    }

    public static NamespacedPath extractPrefix(String path) {
        Matcher matcher = NAMESPACED_PATH_PATTERN.matcher(path);
        if (matcher.matches()) {
            return new NamespacedPath(
                    Optional.ofNullable(matcher.group("namespace")).orElse(DEFAULT_NAMESPACE),
                    matcher.group("path"));
        }
        return new NamespacedPath(DEFAULT_NAMESPACE, path);
    }

    private static boolean containsFile(com.mcmiddleearth.rpmanager.model.project.Layer layer, Object[] path)
            throws IOException {
        if (path == null || path.length == 0) {
            return false;
        }
        List<String> pathStr = Arrays.stream(path).map(Object::toString)
                .collect(Collectors.toCollection(LinkedList::new));
        if (!layer.getFile().getName().endsWith(".jar")) {
            if (pathStr.isEmpty()) {
                return false;
            }
            return containsFile(layer.getFile().getParentFile(), pathStr);
        } else {
            return zipContainsFile(layer.getFile(), pathStr);
        }
    }

    private static boolean containsFile(File file, List<String> path) {
        for (String part : path) {
            file = new File(file, part);
        }
        return file.exists();
    }

    private static boolean zipContainsFile(File file, List<String> path) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            return zipFile.getEntry(String.join("/", path)) != null;
        }
    }

    private static <T> List<T> reverse(List<T> list) {
        List<T> result = new ArrayList<>(list);
        Collections.reverse(result);
        return result;
    }
}
