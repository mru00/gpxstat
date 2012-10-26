/*
 * GpxstatView.java
 */
package gpxstat.ui;

import com.topografix.gpx.x1.x1.GpxDocument;
import com.topografix.gpx.x1.x1.GpxType;
import com.topografix.gpx.x1.x1.TrkType;
import com.topografix.gpx.x1.x1.TrksegType;
import com.topografix.gpx.x1.x1.WptType;
import gpxstat.GpxDocumentWrapper;
import gpxstat.GpxstatApp;
import java.awt.Component;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import org.apache.xmlbeans.XmlException;
import java.text.DateFormat;
import javax.swing.tree.DefaultTreeSelectionModel;

/**
 * The application's main frame.
 */
public class GpxstatView extends FrameView {

    private final GpxTreeModel model = new GpxTreeModel();

    public GpxstatView(SingleFrameApplication app) {
        super(app);

        initComponents();
        jTree1.setSelectionPath(jTree1.getPathForRow(1));



// <editor-fold defaultstate="collapsed" desc="some netbeans generated code for the swing framework">
// status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });// </editor-fold>

    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = GpxstatApp.getApplication().getMainFrame();
            aboutBox = new GpxstatAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        GpxstatApp.getApplication().show(aboutBox);
    }

    @Action
    public void openFile() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "GPX Files", "gpx");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this.getComponent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            final File selectedFile = chooser.getSelectedFile();

            try {
                final GpxDocument doc = GpxDocument.Factory.parse(selectedFile);
                model.addDocument(new GpxDocumentWrapper(selectedFile, doc));

            } catch (IOException ex) {
                Logger.getLogger(GpxstatApp.class.getName()).log(Level.SEVERE, null, ex);
            } catch (XmlException ex) {
                Logger.getLogger(GpxstatApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

// <editor-fold defaultstate="collapsed" desc="TreeModel and TreeCellRenderer">
    /**
     * @see GpxstatView.getTreeModel
     * @return a TreeCellRenderer that formats the GPX TreeModel
     */
    private TreeCellRenderer getTreeCellRenderer() {
        return new DefaultTreeCellRenderer() {

            @Override
            public Component getTreeCellRendererComponent(
                    JTree tree, Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                if (value instanceof List) {
                    setText("Open Files");
                } else if (value instanceof GpxDocumentWrapper) {
                    setText(((GpxDocumentWrapper) value).getFileName());
                } else if (value instanceof TrkType) {
                    setText("Track: " + ((TrkType) value).getName());
                } else if (value instanceof TrksegType) {
                    setText("Track Segment");
                } else if (value instanceof WptType) {
                    final DateFormat timeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                    WptType wp = (WptType) value;
                    String time = timeFormat.format(wp.getTime().getTime());
                    setText("Waypoint " + time);
                } else {
                    throw new IllegalArgumentException("unhandled case in getTreeCellRendererComponent");
                }


                return this;
            }
        };
    }

    private TreeSelectionModel getTreeSelectionModel() {
        return new DefaultTreeSelectionModel() {

            @Override
            public void setSelectionPath(TreePath path) {
                // TODO: implement deselection
                // also in tree-value changed

                //boolean preselected = isPathSelected(path);
                super.setSelectionPath(path);
                //if (preselected) clearSelection();
            }
        };
    }

    /**
     * Provides a TreeModel for the loaded GPX files.
     *
     * The tree will contain instances of the JAXB/GPX class instances directly.
     *
     * To format the Tree, @see getTreeCellRenderer is used.
     *
     * @return the TreeModel representing the GPX data.
     */
    private TreeModel getTreeModel() {
        return model;
    }// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Tree Popup Actions">

    private class CloseDocumentAction extends AbstractAction {

        private GpxDocumentWrapper document;

        public CloseDocumentAction(GpxDocumentWrapper document) {
            super("Close");
            this.document = document;
        }

        public void actionPerformed(ActionEvent e) {
            final TreePath parentPath = jTree1.getSelectionPath().getParentPath();
            jTree1.setSelectionPath(parentPath);

            model.closeDocument(document);

        }
    };

    private class SaveDocumentAction extends AbstractAction {

        private GpxDocumentWrapper document;

        public SaveDocumentAction(GpxDocumentWrapper document) {
            super("Save");
            this.document = document;
        }

        public void actionPerformed(ActionEvent e) {
            final TreePath parentPath = jTree1.getSelectionPath().getParentPath();
            jTree1.setSelectionPath(parentPath);
            if (document.file == null) {
                (new SaveDocumentAsAction(document)).actionPerformed(null);
                return;
            }
            try {
                document.doc.save(document.file);
            } catch (IOException ex) {
                Logger.getLogger(GpxstatView.class.getName()).log(Level.SEVERE, null, ex);
            }
            model.fireStructureChanged(parentPath);
        }
    };

    private class SaveDocumentAsAction extends AbstractAction {

        private GpxDocumentWrapper document;

        public SaveDocumentAsAction(GpxDocumentWrapper document) {
            super("Save As");
            this.document = document;
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "GPX Files", "gpx");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(GpxstatView.this.getComponent());
            if (returnVal == JFileChooser.APPROVE_OPTION) {

                document.file = chooser.getSelectedFile();

                // delegate saving to action saveDocument
                (new SaveDocumentAction(document)).actionPerformed(null);
            }
        }
    };

    private class DeleteTrackAction extends AbstractAction {

        private GpxDocumentWrapper document;
        private TrkType trk;

        public DeleteTrackAction(TrkType trk, GpxDocumentWrapper document) {
            super("Delete Track");
            this.trk = trk;
            this.document = document;
        }

        public void actionPerformed(ActionEvent e) {
            GpxType gpx = document.doc.getGpx();

            int trk_id = findTrackId(gpx, trk);
            if (trk_id == -1) {
                return;
            }

            final TreePath parentPath = jTree1.getSelectionPath().getParentPath();
            jTree1.setSelectionPath(parentPath);

            gpx.removeTrk(trk_id);
            model.fireStructureChanged(parentPath);
        }
    };

    private class DeleteTrackSegmentAction extends AbstractAction {

        private TrkType trk;
        private TrksegType trkseg;

        public DeleteTrackSegmentAction(TrkType trk, TrksegType trkseg) {
            super("Delete Track Segment");
            this.trk = trk;
            this.trkseg = trkseg;
        }

        public void actionPerformed(ActionEvent e) {
            int seg_id = findTrackSeqId(trk, trkseg);
            if (seg_id == -1) {
                return;
            }

            final TreePath parentPath = jTree1.getSelectionPath().getParentPath();
            jTree1.setSelectionPath(parentPath);

            trk.removeTrkseg(seg_id);
            model.fireStructureChanged(parentPath);

        }
    };

    private int findTrackId(GpxType gpx, TrkType trk) {
        for (int i = 0; i < gpx.getTrkArray().length; i++) {
            if (gpx.getTrkArray(i) == trk) {
                return i;
            }
        }
        return -1;
    }

    private int findTrackSeqId(TrkType track, TrksegType seg) {
        for (int i = 0; i < track.getTrksegArray().length; i++) {
            if (track.getTrksegArray(i) == seg) {
                return i;
            }
        }
        return -1;
    }

    private int findWaypointId(TrksegType seg, WptType wpt) {
        for (int i = 0; i < seg.getTrkptArray().length; i++) {
            if (seg.getTrkptArray(i) == wpt) {
                return i;
            }
        }
        return -1;
    }

    private class SplitTrackSegmentAction extends AbstractAction {

        private TrkType trk;
        private TrksegType trkseg;
        private WptType wpt;

        public SplitTrackSegmentAction(TrkType trk, TrksegType trkseg, WptType wpt) {
            super("Split Track Segment here");
            this.trk = trk;
            this.trkseg = trkseg;
            this.wpt = wpt;
        }

        public void actionPerformed(ActionEvent e) {

            int seg_id = findTrackSeqId(trk, trkseg);
            if (seg_id == -1) {
                return;
            }

            int wpt_id = findWaypointId(trkseg, wpt);
            if (wpt_id == -1) {
                return;
            }


            // don't split on first position
            if (wpt_id == 0) {
                return;
            }

            final TreePath parentPath = jTree1.getSelectionPath().getParentPath().getParentPath();
            jTree1.setSelectionPath(parentPath);


            TrksegType newseg = trk.insertNewTrkseg(seg_id + 1);


            List<WptType> remainder = new ArrayList<WptType>();
            for (int k = 0; (k + wpt_id) < trkseg.getTrkptArray().length; k++) {
                remainder.add(trkseg.getTrkptArray(k + wpt_id));
            }

            newseg.setTrkptArray(remainder.toArray(new WptType[0]));

            for (; wpt_id < trkseg.getTrkptArray().length;) {
                trkseg.removeTrkpt(wpt_id);
            }

            model.fireStructureChanged(parentPath);
        }
    };

    /**
     * Shows a popup menu for the JTree
     * @param evt
     */
    private void treePopup(MouseEvent evt) {
        final JTree tree = (JTree) evt.getSource();
        final TreePath selPath = tree.getPathForLocation(evt.getX(), evt.getY());

        if (selPath == null) {
            return;
        }
        tree.setSelectionPath(selPath);

        final JPopupMenu popup = new JPopupMenu();

        final TreePath parentPath = selPath.getParentPath();
        final Object component = selPath.getLastPathComponent();
        Object parent = null, grandparent = null;
        if (parentPath != null) {
            parent = parentPath.getLastPathComponent();

            TreePath grandparentPath = parentPath.getParentPath();
            if (grandparentPath != null) {
                grandparent = grandparentPath.getLastPathComponent();
            }
        }

        if (component instanceof List) {
        } else if (component instanceof GpxDocumentWrapper) {
            GpxDocumentWrapper selectedDocument = (GpxDocumentWrapper) component;
            popup.add(new JMenuItem(new CloseDocumentAction(selectedDocument)));
            popup.add(new JMenuItem(new SaveDocumentAction(selectedDocument)));
            popup.add(new JMenuItem(new SaveDocumentAsAction(selectedDocument)));
        } else if (component instanceof TrkType) {

            popup.add(new JMenuItem(new DeleteTrackAction((TrkType) component, (GpxDocumentWrapper) parent)));

        } else if (component instanceof TrksegType) {
            popup.add(new JMenuItem(new DeleteTrackSegmentAction((TrkType) parent, (TrksegType) component)));
        } else if (component instanceof WptType) {
            popup.add(new JMenuItem(new SplitTrackSegmentAction((TrkType) grandparent, (TrksegType) parent, (WptType) component)));
        } else {
            throw new IllegalArgumentException("unmachted case in jTree1ValueChanged");
        }

        if (popup.getComponentCount() > 0) {
            popup.show(tree, evt.getX(), evt.getY());
        }

    }// </editor-fold>

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        jLabel1 = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        toolBar = new javax.swing.JToolBar();
        openToolItem = new javax.swing.JButton();

        mainPanel.setName("mainPanel"); // NOI18N

        jSplitPane1.setName("jSplitPane1"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTree1.setModel(getTreeModel());
        jTree1.setCellRenderer(getTreeCellRenderer());
        jTree1.setDragEnabled(true);
        jTree1.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        jTree1.setMinimumSize(new java.awt.Dimension(200, 0));
        jTree1.setName("jTree1"); // NOI18N
        jTree1.setScrollsOnExpand(true);
        jTree1.setSelectionModel(getTreeSelectionModel());
        jTree1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTree1MousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jTree1MouseReleased(evt);
            }
        });
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree1ValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(jTree1);

        jSplitPane1.setLeftComponent(jScrollPane2);

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(gpxstat.GpxstatApp.class).getContext().getResourceMap(GpxstatView.class);
        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setAlignmentX(0.5F);
        jLabel1.setName("jLabel1"); // NOI18N
        jSplitPane1.setRightComponent(jLabel1);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 712, Short.MAX_VALUE)
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(gpxstat.GpxstatApp.class).getContext().getActionMap(GpxstatView.class, this);
        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 736, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 550, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setName("toolBar"); // NOI18N

        openToolItem.setAction(actionMap.get("openFile")); // NOI18N
        openToolItem.setFocusable(false);
        openToolItem.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        openToolItem.setName("openToolItem"); // NOI18N
        openToolItem.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(openToolItem);

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
        setToolBar(toolBar);
    }// </editor-fold>//GEN-END:initComponents

    private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree1ValueChanged
        Object component = evt.getPath().getLastPathComponent();

        if (component instanceof List) {
            jSplitPane1.setRightComponent(new JLabel("Open Files"));
        } else if (component instanceof GpxDocumentWrapper) {
            jSplitPane1.setRightComponent(new GpxPanel((GpxDocumentWrapper) component));
        } else if (component instanceof TrkType) {
            jSplitPane1.setRightComponent(new TrkPanel((TrkType) component));
        } else if (component instanceof TrksegType) {

            Component rc = jSplitPane1.getRightComponent();
            if (!(rc instanceof TrksegPanel) || ((TrksegPanel) rc).getTrackseg() != (TrksegType) component) {
                jSplitPane1.setRightComponent(new TrksegPanel((TrksegType) component));
            } else {
                ((TrksegPanel)rc).selectWayPoint(null);
            }
        } else if (component instanceof WptType) {
            Component rc = jSplitPane1.getRightComponent();
            Object parent = evt.getPath().getParentPath().getLastPathComponent();

            if (!(rc instanceof TrksegPanel) || ((TrksegPanel) rc).getTrackseg() != (TrksegType) parent) {
                jSplitPane1.setRightComponent(new TrksegPanel((TrksegType) parent));
            }

            if (rc != null && rc instanceof TrksegPanel) {
                ((TrksegPanel) rc).selectWayPoint((WptType) component);
            }

        } else {
            throw new IllegalArgumentException("unmachted case in jTree1ValueChanged");
        }
    }//GEN-LAST:event_jTree1ValueChanged

    private void jTree1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTree1MousePressed
        if (evt.isPopupTrigger()) {
            treePopup(evt);
        }
    }//GEN-LAST:event_jTree1MousePressed

    private void jTree1MouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTree1MouseReleased
        if (evt.isPopupTrigger()) {
            treePopup(evt);
        }
    }//GEN-LAST:event_jTree1MouseReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTree jTree1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton openToolItem;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private JDialog aboutBox;
}
