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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author James
 */
public class ScalableAbsoluteLayout implements LayoutManager {
        public static class Point {
            double x, y;
            public Point(double x, double y) {
                this.x = x;
                this.y = y;
            }
        }

        public static class Rect {
            public double x, y, width, height;
            public Rect(double x, double y, double width, double height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }

            public Rect offsetBy(Point p) {
                return new Rect(x + p.x, y + p.y, width, height);
            }
            
            public Rect topFraction(double f) {
                return new Rect(x, y, width, height * f);
            }
            
            public Rect bottomFraction(double f) {
                return new Rect(x, y + height * (1 - f), width, height * f);
            }
            
            public boolean intersects(Rect f) {
                return (x < f.x + f.width) && (x + width > f.x) &&
                    (y < f.y + f.width) && (y + width > f.y);
            }
            
            Rectangle.Double rect() {
                return new Rectangle.Double(x, y, width, height);
            }
        }

    private Map<Component,Rect> bounds = new HashMap<Component, Rect>();

    private double width = 1.0;
    private double height = 1.0;
    private final Rect containerBounds;
    private final Container parent;

    public ScalableAbsoluteLayout(Container parent) {
        this.parent = parent;
        containerBounds = new Rect(0, 0, width, height);
    }

    public ScalableAbsoluteLayout(Container parent, double width, double height) {
        this.parent = parent;
        this.width = width;
        this.height = height;
        containerBounds = new Rect(0, 0, width, height);
    }
    
    public void addComponent(Component c, Rect bounds) {
        if(!containerBounds.intersects(bounds))
            throw new IllegalArgumentException("Supplied constraint is outside container bounds: " + bounds);
        this.bounds.put(c, bounds);
        if(c.getParent() != parent) {
            parent.add(c);
        }
    }
    
    public void addComponent(Component c, double x, double y, double w, double h) {
        addComponent(c, new Rect(x,y,w,h));
    }
    
    public Rect getRect(Component c) {
        return bounds.get(c);
    }
    
    public void updateRect(Component c, Rect r) {
        if(bounds.containsKey(c)) {
            bounds.put(c, r);
        }
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Insets insets = parent.getInsets();
        int prefWidth = parent.getWidth() + insets.left + insets.right;
        int prefHeight = parent.getHeight() + insets.top + insets.bottom;
        return new Dimension(prefWidth, prefHeight);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Insets insets = parent.getInsets();
        return new Dimension(100 + insets.left + insets.right, 100 + insets.top + insets.bottom);
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int parentWidth = parent.getWidth() - insets.left - insets.right;
        int parentHeight = parent.getHeight() - insets.top - insets.bottom;

//        double parentAspect = (double)parentWidth / (double)parentHeight;
//        double aspect = width / height;

        int xOffset = 0;
        int yOffset = 0;
        int usableWidth = parentWidth;
        int usableHeight = parentHeight;

//        if(parentAspect > aspect) { // parent is broader than required
//            usableWidth = (int)(usableHeight * aspect);
//            xOffset = (parentWidth - usableWidth) / 2;                
//        } else {
//            usableHeight = (int)(usableWidth / aspect);
//            yOffset = (parentHeight - usableHeight) / 2;
//        }
        /*
        log.debug("parentWidth " + parentWidth);
        log.debug("parentHeight " + parentHeight);
        log.debug("usableWidth " + usableWidth);
        log.debug("usableHeight " + usableHeight);
        log.debug("xOffset " + xOffset);
        log.debug("yOffset " + yOffset);
        log.debug("insets " + insets.left + ", " + insets.right + ", " + insets.top + ", " + insets.bottom);
        */
        int nComps = parent.getComponentCount();
        for(int i = 0; i < nComps; i++) {
            Component comp = parent.getComponent(i);
            if(!bounds.containsKey(comp))
                throw new RuntimeException("No bounds present for component " + comp);
            Rect b = bounds.get(comp);
            int bx = (int) (xOffset + b.x * usableWidth / width);
            int by = (int) (yOffset + b.y * usableHeight / height);
            int bw = (int) ((b.x + b.width) * usableWidth / width - bx + xOffset);
            int bh = (int) ((b.y + b.height) * usableHeight / height - by + yOffset);
//                log.debug("Bounds for " + comp.getName() + ": " + bx + ", " + by + ", " + bw + ", " + bh);
            comp.setBounds(bx, by, bw, bh);
        }
    }
}
