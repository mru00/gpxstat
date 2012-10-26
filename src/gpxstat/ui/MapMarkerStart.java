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
public class MapMarkerStart extends AbstractMapMarker {
    final Color color = Color.GREEN;

    public MapMarkerStart(WptType tp) {
        super(tp);
    }

    @Override
    public void setSelected(boolean isSelected) {
        super.setSelected(isSelected);
    }

    public void paint(Graphics g, Point position) {
        int size_h = 5 * (getSelected() ? 3: 1);
        int size = size_h * 2;
        g.setColor(color);
        g.fillOval(position.x - size_h, position.y - size_h, size, size);
        g.setColor(Color.BLACK);
        g.drawOval(position.x - size_h, position.y - size_h, size, size);
    }

}
