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

package com.mcmiddleearth.rpmanager.utils;

import com.mcmiddleearth.rpmanager.gui.MainWindow;

import javax.swing.*;
import javax.swing.text.BadLocationException;

public class TextAreaUtils {
    public static void scrollTextAreaIgnoreWhitespaces(JTextArea textArea, String part) {
        int index = textArea.getText().replaceAll("\\s+", "").indexOf(part);
        if (index < 0) {
            return;
        }
        String text = textArea.getText();
        int col = 0;
        int row = 0;
        int currIndex = 0;
        int currNonWhitespaceIndex = -1;
        while (currNonWhitespaceIndex < index) {
            if (!Character.isWhitespace(text.codePointAt(currIndex))) {
                currNonWhitespaceIndex++;
            }
            if (text.codePointAt(currIndex) == '\n') {
                row++;
                col = 0;
            } else if (currNonWhitespaceIndex < index) {
                col++;
            }
            currIndex++;
        }
        try {
            scrollTextArea(textArea, col, row);
        } catch (BadLocationException e) {
            JOptionPane.showMessageDialog(MainWindow.getInstance(),
                    "Unknown error trying to scroll preview: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void scrollTextArea(JTextArea textArea, int col, int row) throws BadLocationException {
        textArea.setCaretPosition(textArea.getLineStartOffset(row) + col);
    }
}
