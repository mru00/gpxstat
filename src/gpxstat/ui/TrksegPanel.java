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

import com.topografix.gpx.x1.x1.TrksegType;
import com.topografix.gpx.x1.x1.WptType;
import java.awt.Component;
import java.text.DateFormat;
import javax.swing.JSeparator;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.tabbedui.VerticalLayout;

/**
 *
 * @author mru
 */
public class TrksegPanel extends javax.swing.JPanel {

    private final TrksegType trkseg;
    private final double[] distAcc;
    private final double[] speedVect;
    private final double[] elevationVect;

    /** Creates new form TrksegPanel
     * @param trkseg
     */
    public TrksegPanel(final TrksegType trkseg) {
        this.trkseg = trkseg;

        distAcc = new double[trkseg.getTrkptArray().length];
        speedVect = new double[trkseg.getTrkptArray().length];
        elevationVect = new double[trkseg.getTrkptArray().length];

        double dist = 0.0;
        WptType last = null;
        for (int i = 0; i < trkseg.getTrkptArray().length; i++) {

            final WptType w = trkseg.getTrkptArray(i);

            if (last != null) {
                final double dist0 = getDist(last, w);
                dist += dist0;
                speedVect[i] = dist0 / getTimeDiff(last, w);
            }

            last = w;
            distAcc[i] = dist;
            elevationVect[i] = w.getEle().doubleValue();
        }

        if (trkseg.getTrkptArray().length > 1) {
            speedVect[0] = speedVect[1];
        }

        initComponents();

        jPanel1.setLayout(new VerticalLayout());
        jPanel1.add(getElevationOverDistancePlot());
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
        if (elevationVect.length < 1) {
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
        if (elevationVect.length < 1) {
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

    private double lastOr(double[] arr, double _then) {
        if (arr.length == 0) {
            return _then;
        }
        return arr[arr.length - 1];
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

    private Component getElevationOverDistancePlot() {

        XYSeries series1 = new XYSeries("Elevation", false);
        XYSeries series2 = new XYSeries("Speed", false);
        for (int i = 0; i < trkseg.getTrkptArray().length; i++) {
            series1.add(distAcc[i], elevationVect[i]);
            series2.add(distAcc[i], speedVect[i]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);
        JFreeChart chart = ChartFactory.createXYLineChart("Elevation over Distance", "Distance [km]", "Elevation [m]", dataset, PlotOrientation.VERTICAL, true, true, true);

        return new ChartPanel(chart);
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
}
