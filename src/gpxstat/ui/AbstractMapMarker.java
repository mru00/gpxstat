/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gpxstat.ui;


import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;

import com.topografix.gpx.x1.x1.WptType;


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

    public double getEle() {
        return tp.getEle().doubleValue();
    }
    
    public boolean hasCoordinates() {
        return tp.getLat() != null && tp.getLon() != null && tp.getEle() != null;
    }
    
    @Override
    public String toString() {
        return "MapMarker at " + getLat() + " " + getLon();
    }
}
