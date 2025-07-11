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

package com.mcmiddleearth.rpmanager.model;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.mcmiddleearth.rpmanager.json.adapters.ItemsModelJsonAdapter;
import com.mcmiddleearth.rpmanager.model.item.ItemsModel;

public class Item implements JsonRoot {
    @SerializedName("hand_animation_on_swap")
    private Boolean handAnimationOnSwap;
    @JsonAdapter(ItemsModelJsonAdapter.Factory.class)
    private ItemsModel model;

    public Boolean getHandAnimationOnSwap() {
        return handAnimationOnSwap;
    }

    public void setHandAnimationOnSwap(Boolean handAnimationOnSwap) {
        this.handAnimationOnSwap = handAnimationOnSwap;
    }

    public ItemsModel getModel() {
        return model;
    }

    public void setModel(ItemsModel model) {
        this.model = model;
    }
}
