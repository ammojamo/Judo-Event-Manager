/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
    private Map<Component,Rectangle.Double> bounds = new HashMap<Component, Rectangle.Double>();

    private double width = 1.0;
    private double height = 1.0;
    private Rectangle.Double containerBounds;
    private Container parent;

    public ScalableAbsoluteLayout(Container parent) {
        this.parent = parent;
        containerBounds = new Rectangle.Double(0, 0, width, height);
    }

    public ScalableAbsoluteLayout(Container parent, double width, double height) {
        this.parent = parent;
        this.width = width;
        this.height = height;
        containerBounds = new Rectangle.Double(0, 0, width, height);
    }
    
    public void addComponent(Component c, Rectangle.Double bounds) {
        if(!containerBounds.intersects(bounds))
            throw new IllegalArgumentException("Supplied constraint is outside container bounds: " + bounds);
        this.bounds.put(c, bounds);
        parent.add(c);
    }
    
    public void addComponent(Component c, double x, double y, double w, double h) {
        addComponent(c, new Rectangle.Double(x,y,w,h));
    }


    @Override
    public void addLayoutComponent(String name, Component comp) {
        return;
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        return;
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

        double parentAspect = (double)parentWidth / (double)parentHeight;
        double aspect = width / height;

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
            Rectangle.Double b = bounds.get(comp);
            int bx = (int) (xOffset + b.getX() * usableWidth / width);
            int by = (int) (yOffset + b.getY() * usableHeight / height);
            int bw = (int) ((b.getX() + b.getWidth()) * usableWidth / width - bx + xOffset);
            int bh = (int) ((b.getY() + b.getHeight()) * usableHeight / height - by + yOffset);
//                log.debug("Bounds for " + comp.getName() + ": " + bx + ", " + by + ", " + bw + ", " + bh);
            comp.setBounds(bx, by, bw, bh);
        }
    }
}
