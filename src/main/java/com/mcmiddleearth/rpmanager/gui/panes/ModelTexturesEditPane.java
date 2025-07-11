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
import com.mcmiddleearth.rpmanager.events.EventDispatcher;
import com.mcmiddleearth.rpmanager.events.EventListener;
import com.mcmiddleearth.rpmanager.gui.actions.Action;
import com.mcmiddleearth.rpmanager.gui.components.CollapsibleSection;
import com.mcmiddleearth.rpmanager.gui.components.IconButton;
import com.mcmiddleearth.rpmanager.gui.components.VerticalBox;
import com.mcmiddleearth.rpmanager.gui.constants.Icons;
import com.mcmiddleearth.rpmanager.model.BaseModel;
import com.mcmiddleearth.rpmanager.utils.Pair;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModelTexturesEditPane extends VerticalBox {
    private final BaseModel baseModel;
    private final List<Pair<String, String>> textures;
    private final EventDispatcher eventDispatcher = new EventDispatcher();

    public ModelTexturesEditPane(BaseModel baseModel) {
        this.baseModel = baseModel;
        this.textures = baseModel.getTextures() == null ?
                new LinkedList<>() :
                new LinkedList<>(baseModel.getTextures().entrySet().stream().map(Pair::new).toList());

        if (!textures.isEmpty()) {
            for (Pair<String, String> entry : textures) {
                this.add(createTextureEntry(entry));
            }
        }
    }

    public void addTextureEntry() {
        Pair<String, String> entry = new Pair<>(null, null);
        textures.add(entry);
        this.add(createTextureEntry(entry));
        revalidate();
        repaint();
        onTexturesChange();
    }

    private CollapsibleSection createTextureEntry(Pair<String, String> entry) {
        TextureEntryEditPane textureEntryEditPane = new TextureEntryEditPane(entry);
        textureEntryEditPane.addChangeListener(e -> onTexturesChange());
        return new CollapsibleSection("Texture", textureEntryEditPane, false, removeTextureButton(entry));
    }

    private JButton removeTextureButton(Pair<String, String> entry) {
        return new IconButton(new Action("-", Icons.DELETE_ICON, "Remove texture") {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int index = textures.indexOf(entry);
                textures.remove(index);
                remove(index);
                revalidate();
                repaint();
                onTexturesChange();
            }
        });
    }

    private void onTexturesChange() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Pair<String, String> entry : textures) {
            if (entry.getLeft() != null) {
                map.put(entry.getLeft(), entry.getRight());
            }
        }
        if (map.isEmpty()) {
            map = null;
        }
        baseModel.setTextures(map);
        onChange();
    }

    private void onChange() {
        eventDispatcher.dispatchEvent(new ChangeEvent(this, null));
    }

    public void addChangeListener(EventListener<ChangeEvent> listener) {
        eventDispatcher.addEventListener(listener, ChangeEvent.class);
    }
}
