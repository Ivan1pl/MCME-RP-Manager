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

package com.mcmiddleearth.rpmanager.model.internal;

import java.util.List;

public class RelatedFiles {
    private final List<LayerRelatedFiles> relatedModels;
    private final List<LayerRelatedFiles> relatedTextures;
    private final List<LayerRelatedFiles> relatedBlockstates;
    private final List<LayerRelatedFiles> relatedItems;

    public RelatedFiles(List<LayerRelatedFiles> relatedModels, List<LayerRelatedFiles> relatedTextures,
                        List<LayerRelatedFiles> relatedBlockstates, List<LayerRelatedFiles> relatedItems) {
        this.relatedModels = relatedModels;
        this.relatedTextures = relatedTextures;
        this.relatedBlockstates = relatedBlockstates;
        this.relatedItems = relatedItems;
    }

    public List<LayerRelatedFiles> getRelatedModels() {
        return relatedModels;
    }

    public List<LayerRelatedFiles> getRelatedTextures() {
        return relatedTextures;
    }

    public List<LayerRelatedFiles> getRelatedBlockstates() {
        return relatedBlockstates;
    }

    public List<LayerRelatedFiles> getRelatedItems() {
        return relatedItems;
    }
}
