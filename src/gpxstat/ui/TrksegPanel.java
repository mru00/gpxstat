/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * TrksegPanel.java
 *
 * Created on Jul 4, 2011, 2:38:17 PM
 */
package gpxstat.ui;

import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;
import java.awt.Color;
import java.awt.BasicStroke;
import com.topografix.gpx.x1.x1.TrksegType;
import com.topografix.gpx.x1.x1.WptType;
import java.awt.Component;
import java.text.DateFormat;
import javax.swing.JSeparator;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.tabbedui.VerticalLayout;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import java.util.List;

/**
 *
 * @author mru
 */
public class TrksegPanel extends javax.swing.JPanel {

    private final TrksegType trkseg;
    private final double[] distAcc;
    private final double[] speedVect;
    private final double[] elevationVect;
    private final double[] eleGradientVect;
    private final JFreeChart elevationChart;
    private Marker elevationChartMarker = null;
    //private final int trkLength;
    private final JMapViewer viewer;
    //private final MapMarker[] mapMarker = new MapMarker[100];

    /** Creates new form TrksegPanel
     * @param trkseg
     */
    public TrksegPanel(final TrksegType trkseg) {
        this.trkseg = trkseg;
        int trkLength = trkseg.getTrkptArray().length;

        distAcc = new double[trkLength];
        speedVect = new double[trkLength];
        elevationVect = new double[trkLength];
        eleGradientVect = new double[trkLength];
        viewer = createMapViewer();

        double dist = 0.0;
        double lastEle = 0.0;
        WptType last = null;
        for (int i = 0; i < trkLength; i++) {
            

            final WptType w = trkseg.getTrkptArray(i);
            
            if (w.getEle() == null || w.getLon() == null || w.getLat()== null) {
                distAcc[i] = speedVect[i] = elevationVect[i]=eleGradientVect[i] = Double.NaN;
                continue;
            }
            
            final double ele0 = w.getEle().doubleValue();

            if (last != null) {

                final double ele1 = ele0 - lastEle;
                final double dist2d = getDist(last, w);

                // TODO: find out how NaN in dist2d can occur!

                if (!Double.isNaN(dist2d)) {

                    final double dist3d = Math.sqrt(ele1 * ele1 / 10e6 + dist2d * dist2d);

                    dist += dist3d;

                    final double speed = dist3d / getTimeDiff(last, w);
                    final double eleGradient = ele1 / (dist2d * 10); // 100*([m] - [m]) / [km]*1000

                    eleGradientVect[i] = Double.isInfinite(eleGradient) ? Double.NaN : eleGradient;
                    speedVect[i] = speed;
                }
                else {
                    eleGradientVect[i] = speedVect[i] = Double.NaN;
                }
            }
            else {
                eleGradientVect[i] = speedVect[i] = Double.NaN;
            }

            last = w;


            distAcc[i] = dist;
            elevationVect[i] = ele0;
            lastEle = ele0;
        }

        if (trkLength > 1) {
            speedVect[0] = speedVect[1];
            eleGradientVect[0] = eleGradientVect[1];
        }

        initComponents();


        elevationChart = getElevationOverDistancePlot();

        jPanel1.setLayout(new VerticalLayout());
        jPanel1.add(viewer);
        jPanel1.add(new JSeparator());
        jPanel1.add(new ChartPanel(elevationChart));
        jPanel1.add(new JSeparator());
        jPanel1.add(getSpeedOverDistancePlot());
        jPanel1.add(new JSeparator());
        jPanel1.add(getSpeedOverTimePlot());
        jPanel1.add(new JSeparator());
        jPanel1.add(getDistanceOverTimePlot());
    }

    private double getDist(WptType first, WptType second) {
        //http://www.movable-type.co.uk/scripts/latlong.html

        //http://www.csgnetwork.com/gpsdistcalc.html

        final double lat1 = Math.PI * first.getLat().doubleValue() / 180.0;
        final double lon1 = Math.PI * first.getLon().doubleValue() / 180.0;
        final double lat2 = Math.PI * second.getLat().doubleValue() / 180.0;
        final double lon2 = Math.PI * second.getLon().doubleValue() / 180.0;


        double R = 6371; // km
        return Math.acos(Math.sin(lat1) * Math.sin(lat2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.cos(lon2 - lon1)) * R;
    }

    private double getTimeDiff(WptType first, WptType second) {
        final double millis = second.getTime().getTimeInMillis() - first.getTime().getTimeInMillis();
        return millis / (60 * 60 * 1000);
    }

    private double arrMax(double[] arr) {
        double maxVal = Double.MIN_VALUE;
        for (double d : arr) {
            if (d > maxVal) {
                maxVal = d;
            }
        }
        return maxVal;
    }

    private double arrMin(double[] arr) {
        double maxVal = Double.MAX_VALUE;
        for (double d : arr) {
            if (d < maxVal) {
                maxVal = d;
            }
        }
        return maxVal;
    }

    private double arrAvg(double[] arr) {
        double sum = 0.0;
        for (double d : arr) {
            sum += d;
        }
        return sum / arr.length;
    }

    private double ascent() {
        double sum = 0.0;
        if (elevationVect.length == 0) {
            return 0.0;
        }
        double last = elevationVect[0];
        for (int i = 1; i < elevationVect.length; i++) {
            final double diff = elevationVect[i] - last;
            if (diff > 0) {
                sum += diff;
            }
            last = elevationVect[i];
        }
        return sum;
    }

    private double descent() {
        double sum = 0.0;
        if (elevationVect.length == 0) {
            return 0.0;
        }
        double last = elevationVect[0];
        for (int i = 1; i < elevationVect.length; i++) {
            final double diff = elevationVect[i] - last;
            if (diff < 0) {
                sum += diff;
            }
            last = elevationVect[i];
        }
        return -sum;
    }

    private TableModel getStatsTableModel() {

        final DateFormat timeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        final String speedFormat = "%1.1f";

        String startTime = "", endTime = "", distance = "0";

        if (trkseg.getTrkptArray().length > 0) {
            startTime = timeFormat.format(trkseg.getTrkptArray(0).getTime().getTime());
            endTime = timeFormat.format(trkseg.getTrkptArray(trkseg.getTrkptArray().length - 1).getTime().getTime());
            distance = "" + String.format(speedFormat, distAcc[distAcc.length - 1]);
        }

        final String[][] entries = {
            {"Total Distance [km]", distance},
            {"Start Time", startTime},
            {"End Time", endTime},
            {"Ascent [m]", "" + (int) ascent()},
            {"Descent [m]", "" + (int) descent()},
            {"Speed Max [km/h]", String.format(speedFormat, arrMax(speedVect))},
            {"Speed Min [km/h]", String.format(speedFormat, arrMin(speedVect))},
            {"Speed Avg [km/h]", String.format(speedFormat, arrAvg(speedVect))},
            {"Elevation Max [m]", "" + (int) arrMax(elevationVect)},
            {"Elevation Min [m]", "" + (int) arrMin(elevationVect)},
            {"Elevation Avg [m]", "" + (int) arrAvg(elevationVect)}
        };

        return new AbstractTableModel() {

            public int getRowCount() {
                return entries.length;
            }

            public int getColumnCount() {
                return 2;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                return entries[rowIndex][columnIndex];
            }
        };

    }

    private JFreeChart getElevationOverDistancePlot() {

        XYSeries series1 = new XYSeries("Elevation", false);
        XYSeries series2 = new XYSeries("Speed", false);
        XYSeries series3 = new XYSeries("Elevation Gradient", false);
        for (int i = 0; i < trkseg.getTrkptArray().length; i++) {
            series1.add(distAcc[i], elevationVect[i]);
            series2.add(distAcc[i], speedVect[i]);
            series3.add(distAcc[i], eleGradientVect[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);
        dataset.addSeries(series3);
        JFreeChart chart = ChartFactory.createXYLineChart("Elevation over Distance", "Distance [km]", "Elevation [m]", dataset, PlotOrientation.VERTICAL, true, true, true);

        return chart;
    }

    private Component getSpeedOverDistancePlot() {

        XYSeries series = new XYSeries(0, false);
        for (int i = 0; i < trkseg.getTrkptArray().length; i++) {
            series.add(distAcc[i], speedVect[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Speed over Distance", "Distance [km]", "Speed [km/h]", dataset, PlotOrientation.VERTICAL, false, true, true);

        return new ChartPanel(chart);
    }

    private Component getSpeedOverTimePlot() {

        XYSeries series = new XYSeries(0);
        for (int i = 0; i < trkseg.getTrkptArray().length; i++) {
            series.add(trkseg.getTrkptArray(i).getTime().getTimeInMillis(), speedVect[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Speed over Time", "Time", "Speed [km/h]", dataset, PlotOrientation.VERTICAL, false, true, true);

        return new ChartPanel(chart);
    }

    private Component getDistanceOverTimePlot() {

        XYSeries series = new XYSeries(0);
        for (int i = 0; i < trkseg.getTrkptArray().length; i++) {
            series.add(trkseg.getTrkptArray(i).getTime().getTimeInMillis(), distAcc[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Distance over Time", "Time", "Distance [km]", dataset, PlotOrientation.VERTICAL, false, true, true);

        return new ChartPanel(chart);
    }

    private JMapViewer createMapViewer() {

        JMapViewer newViewer = new JMapViewer();
        final int trkLength = trkseg.getTrkptArray().length;

        for (int i = 0; i < 1 && i < trkLength; i++) {
            final AbstractMapMarker mark = new MapMarkerStart(trkseg.getTrkptArray(i));
            if (mark.hasCoordinates())
                newViewer.addMapMarker(mark);
        }

        for (int i = 1; i < trkLength - 1; i++) {
            final AbstractMapMarker mark = new MapMarkerBetween(trkseg.getTrkptArray(i), trkseg.getTrkptArray(i));
            if (mark.hasCoordinates())
            newViewer.addMapMarker(mark);
        }

        for (int i = trkLength - 1; i > 0 && i < trkLength; i++) {
            final AbstractMapMarker mark = new MapMarkerEnd(trkseg.getTrkptArray(i), trkseg.getTrkptArray(i));
            if (mark.hasCoordinates())
            newViewer.addMapMarker(mark);
        }


        newViewer.setZoom(10);
        newViewer.setDisplayToFitMapMarkers();
        return newViewer;
    }

    public void selectWayPoint(WptType wp) {
        // use 'wp = null' to clear selection!

        WptType selected = null;
        for (int i = 0; i < trkseg.getTrkptArray().length; i++) {
            final WptType w = trkseg.getTrkptArray(i);
            if (w == wp) {
                selected = w;
                break;
            }
        }

        removeChartMarker();

        List<AbstractMapMarker> mapMarkers = (List<AbstractMapMarker>) (List<?>) viewer.getMapMarkerList();
        int i = 0;
        for (AbstractMapMarker mapMarker : mapMarkers) {

            boolean isSelected = mapMarker.getWaypoint() == selected;

            mapMarker.setSelected(isSelected);
            if (isSelected) {
                addChartMarker(i);
            }
            i++;
        }


        viewer.repaint();
    }

    private void removeChartMarker() {
        if (elevationChartMarker != null) {
            elevationChart.getXYPlot().removeDomainMarker(elevationChartMarker);
            elevationChartMarker = null;
        }
    }

    private void addChartMarker(int index) {
        elevationChartMarker = new ValueMarker(distAcc[index]);
        elevationChartMarker.setPaint(Color.BLUE);
        elevationChartMarker.setStroke(new BasicStroke(3));
        elevationChartMarker.setLabel("Selected Waypoint");
        elevationChartMarker.setLabelAnchor(RectangleAnchor.BOTTOM_RIGHT);
        elevationChartMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);

        elevationChart.getXYPlot().addDomainMarker(elevationChartMarker);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        setName("Form"); // NOI18N

        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(gpxstat.GpxstatApp.class).getContext().getResourceMap(TrksegPanel.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jSeparator1.setName("jSeparator1"); // NOI18N

        jPanel1.setBorder(new javax.swing.border.MatteBorder(null));
        jPanel1.setName("jPanel1"); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 698, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 287, Short.MAX_VALUE)
        );

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTable1.setModel(getStatsTableModel());
        jTable1.setName("jTable1"); // NOI18N
        jScrollPane2.setViewportView(jTable1);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jScrollPane1.setViewportView(jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 726, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables

    TrksegType getTrackseg() {
        return trkseg;
    }
}
