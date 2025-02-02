/*
 * Copyright (C) 2023 MCME
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

import javax.swing.*;
import java.io.File;

public class Settings {
    public static File FILE = new File(System.getProperty("user.home"), "mcme-rp-manager-settings.json");

    private String lookAndFeel = UIManager.getLookAndFeel().getName();
    private String imageEditor;
    private int openedFileHistorySize = 20;

    public String getLookAndFeel() {
        return lookAndFeel;
    }

    public void setLookAndFeel(String lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }

    public String getImageEditor() {
        return imageEditor;
    }

    public void setImageEditor(String imageEditor) {
        this.imageEditor = imageEditor;
    }

    public int getOpenedFileHistorySize() {
        return openedFileHistorySize;
    }

    public void setOpenedFileHistorySize(int openedFileHistorySize) {
        this.openedFileHistorySize = openedFileHistorySize;
    }
}
