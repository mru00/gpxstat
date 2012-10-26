/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gpxstat.ui;


import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

import com.topografix.gpx.x1.x1.WptType;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

/**
 *
 * @author mru
 */
public abstract class AbstractMapMarker implements MapMarker {
    private final WptType tp;
    private boolean isSelected = false;

    public AbstractMapMarker(WptType tp) {
        this.tp = tp;
    }

    public WptType getWaypoint() {
        return tp;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public boolean getSelected() {
        return isSelected;
    }
    
    public double getLat() {
        return tp.getLat().doubleValue();
    }

    public double getLon() {
        return tp.getLon().doubleValue();
    }

    @Override
    public String toString() {
        return "MapMarker at " + getLat() + " " + getLon();
    }
}
