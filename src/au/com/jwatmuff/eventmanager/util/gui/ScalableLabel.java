/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.com.jwatmuff.eventmanager.util.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class ScalableLabel extends JLabel {
    private static final Logger log = Logger.getLogger(ScalableLabel.class);

    private static final float BASE_FONT_SIZE = 32.0f;
    private boolean ignoreWidth = false;
    private boolean constructed = false;

    private Font baseFont;


    public ScalableLabel(String label, boolean ignoreWidth) {
        this(label);
        this.ignoreWidth = ignoreWidth;
    }

    public ScalableLabel(String label) {
        this(label, null);
    }

    public ScalableLabel(String label, Font f) {
        super(label);
        setOpaque(true);
        setHorizontalAlignment(ScalableLabel.CENTER);
        //this.setBorder(new EmptyBorder(0,0,0,0));
        setBorder(new LineBorder(Color.GRAY, 1));

        if(f != null)
            baseFont = f.deriveFont(BASE_FONT_SIZE);
        else
            baseFont = super.getFont().deriveFont(BASE_FONT_SIZE);

        baseFont = baseFont.deriveFont(Font.BOLD);
        constructed = true;
        setText(label);
    }

    @Override
    public void setFont(Font font) {
        if(!constructed) {
            super.setFont(font);
        }
        else {
            baseFont = font.deriveFont(BASE_FONT_SIZE);
            updateFont(getText());
        }
    }

    @Override
    public void setText(String text) {
        if(text == null) text = "";
        if(!constructed) {
            super.setText(text);
        }
        else {
            //super.setText("");
            updateFont(text);
            super.setText(text);
        }
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x, y, w, h);
        updateFont(getText());
    }

    private void updateFont(String text) {
        FontMetrics fm = this.getFontMetrics(baseFont);
        Rectangle2D bounds = fm.getStringBounds(text, getGraphics());

        Insets is = this.getInsets();
        int vspace = is.bottom + is.top + 2;
        int hspace = is.left + is.right + 2;
        
        float scale = (float)(getHeight()-vspace) / (float)(bounds.getHeight()+vspace);

        if(!ignoreWidth) {
            scale = Math.min(scale, (float)(getWidth()-hspace) / (float)(bounds.getWidth()+hspace));
        }

        float size = (float) Math.floor(scale * BASE_FONT_SIZE);
        super.setFont(baseFont.deriveFont(size));
    }
}
