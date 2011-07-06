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
import javax.swing.event.TreeModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.apache.xmlbeans.XmlException;

/**
 * The application's main frame.
 */
public class GpxstatView extends FrameView {

    private final List documents = new ArrayList();

    public GpxstatView(SingleFrameApplication app) {
        super(app);

        initComponents();

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
        });
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
                documents.add(new GpxDocumentWrapper(selectedFile, doc));
                jTree1.updateUI();
            } catch (IOException ex) {
                Logger.getLogger(GpxstatApp.class.getName()).log(Level.SEVERE, null, ex);
            } catch (XmlException ex) {
                Logger.getLogger(GpxstatApp.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Action
    public void newFile() {

        final GpxDocument doc = GpxDocument.Factory.newInstance();
        final GpxType gpx = GpxType.Factory.newInstance();
        gpx.setCreator("gpxstat");
        doc.setGpx(gpx);
        documents.add(new GpxDocumentWrapper(null, doc));
        jTree1.updateUI();
    }

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
                    setText("Waypoint");
                } else {
                    throw new IllegalArgumentException("unhandled case in getTreeCellRendererComponent");
                }


                return this;
            }
        };
    }

    /**
     *
     * @return
     */
    private TreeModel getTreeModel() {
        return (new TreeModel() {

            public Object getRoot() {
                return documents;
            }

            public Object getChild(Object parent, int index) {
                if (parent instanceof List) {
                    return ((List) parent).get(index);
                } else if (parent instanceof GpxDocumentWrapper) {
                    return ((GpxDocumentWrapper) parent).doc.getGpx().getTrkArray(index);
                } else if (parent instanceof TrkType) {
                    return ((TrkType) parent).getTrksegArray(index);
                } else if (parent instanceof TrksegType) {
                    return ((TrksegType) parent).getTrkptArray(index);
                } else {
                    throw new IllegalArgumentException("unhandled case in getChild");
                }

            }

            public int getChildCount(Object parent) {
                if (parent instanceof List) {
                    return ((List) parent).size();
                } else if (parent instanceof GpxDocumentWrapper) {
                    return ((GpxDocumentWrapper) parent).doc.getGpx().sizeOfTrkArray();
                } else if (parent instanceof TrkType) {
                    return ((TrkType) parent).sizeOfTrksegArray();
                } else if (parent instanceof TrksegType) {
                    return ((TrksegType) parent).sizeOfTrkptArray();
                } else {
                    throw new IllegalArgumentException("unhandled case in getChildCount");
                }
            }

            public boolean isLeaf(Object node) {
                return (node instanceof WptType);
            }

            public void valueForPathChanged(TreePath path, Object newValue) {
                //throw new UnsupportedOperationException("Not supported yet.");
            }

            public int getIndexOfChild(Object parent, Object child) {
                int i = 0;
                if (parent instanceof List) {
                    return ((List) parent).indexOf(child);
                } else if (parent instanceof GpxDocumentWrapper) {

                    for (TrkType t : ((GpxDocumentWrapper) parent).doc.getGpx().getTrkArray()) {
                        if (t == child) {
                            return i;
                        }
                        i++;
                    }
                    return 0;
                } else if (parent instanceof TrkType) {
                    for (TrksegType t : ((TrkType) parent).getTrksegArray()) {
                        if (t == child) {
                            return i;
                        }
                        i++;
                    }
                    return 0;
                } else if (parent instanceof TrksegType) {
                    for (WptType t : ((TrksegType) parent).getTrkptArray()) {
                        if (t == child) {
                            return i;
                        }
                        i++;
                    }
                    return 0;
                } else {
                    throw new IllegalArgumentException("unhandled case in getIndexOfChild");
                }
            }

            public void addTreeModelListener(TreeModelListener l) {
                //throw new UnsupportedOperationException("Not supported yet.");
            }

            public void removeTreeModelListener(TreeModelListener l) {
                //throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }
    GpxDocumentWrapper selectedDocument;
    TrkType selectedTrack;
    TrksegType selectedTrackSegment;
    private final AbstractAction closeDocument = new AbstractAction("Close") {

        public void actionPerformed(ActionEvent e) {
            documents.remove(selectedDocument);
            jTree1.updateUI();
        }
    };
    private final AbstractAction saveDocument = new AbstractAction("Save") {

        public void actionPerformed(ActionEvent e) {
            if (selectedDocument.file == null) {
                saveDocumentAs.actionPerformed(null);
                return;
            }
            try {
                selectedDocument.doc.save(selectedDocument.file);
            } catch (IOException ex) {
                Logger.getLogger(GpxstatView.class.getName()).log(Level.SEVERE, null, ex);
            }
            jTree1.updateUI();
        }
    };
    private final AbstractAction saveDocumentAs = new AbstractAction("Save As") {

        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "GPX Files", "gpx");
            chooser.setFileFilter(filter);
            int returnVal = chooser.showSaveDialog(GpxstatView.this.getComponent());
            if (returnVal == JFileChooser.APPROVE_OPTION) {

                selectedDocument.file = chooser.getSelectedFile();

                // delegate saving to action saveDocument
                saveDocument.actionPerformed(null);
            }
        }
    };
    private final AbstractAction addNewTrack = new AbstractAction("Add new Track") {

        public void actionPerformed(ActionEvent e) {
            GpxType gpx = selectedDocument.doc.getGpx();
            TrkType trk = gpx.addNewTrk();
            trk.setName("Unnamed");
            jTree1.updateUI();
        }
    };
    private final AbstractAction deleteTrack = new AbstractAction("Delete Track") {

        public void actionPerformed(ActionEvent e) {
            GpxType gpx = selectedDocument.doc.getGpx();
            for (int i = 0; i < gpx.getTrkArray().length; i++) {
                if (gpx.getTrkArray(i) == selectedTrack) {
                    gpx.removeTrk(i);
                    break;
                }
            }
            jTree1.updateUI();
        }
    };
    private final AbstractAction addTrackSegment = new AbstractAction("Add new Track Segment") {

        public void actionPerformed(ActionEvent e) {
            selectedTrack.addNewTrkseg();
            jTree1.updateUI();
        }
    };
    private final AbstractAction deleteTrackSegment = new AbstractAction("Delete Track Segment") {

        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < selectedTrack.getTrksegArray().length; i++) {
                if (selectedTrack.getTrksegArray(i) == selectedTrackSegment) {
                    selectedTrack.removeTrkseg(i);
                    break;
                }
            }
            jTree1.updateUI();

        }
    };

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

        final Object parent = parentPath != null ? parentPath.getLastPathComponent() : null;

        if (component instanceof List) {
        } else if (component instanceof GpxDocumentWrapper) {
            selectedDocument = (GpxDocumentWrapper) component;
            popup.add(new JMenuItem(closeDocument));
            popup.add(new JMenuItem(saveDocument));
            popup.add(new JMenuItem(saveDocumentAs));
            popup.add(new JMenuItem(addNewTrack));
        } else if (component instanceof TrkType) {
            selectedDocument = (GpxDocumentWrapper) parent;
            selectedTrack = (TrkType) component;

            popup.add(new JMenuItem(deleteTrack));
            popup.add(new JMenuItem(addTrackSegment));

        } else if (component instanceof TrksegType) {
            selectedTrackSegment = (TrksegType) component;
            selectedTrack = (TrkType) parent;
            popup.add(new JMenuItem(deleteTrackSegment));
        } else if (component instanceof WptType) {
        } else {
            throw new IllegalArgumentException("unmachted case in jTree1ValueChanged");
        }

        if (popup.getComponentCount() > 0) {
            popup.show(tree, evt.getX(), evt.getY());
        }

    }

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
        newToolItem = new javax.swing.JButton();
        openToolItem = new javax.swing.JButton();

        mainPanel.setName("mainPanel"); // NOI18N

        jSplitPane1.setName("jSplitPane1"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        jTree1.setModel(getTreeModel());
        jTree1.setCellRenderer(getTreeCellRenderer());
        jTree1.setDragEnabled(true);
        jTree1.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        jTree1.setEditable(true);
        jTree1.setName("jTree1"); // NOI18N
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
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE)
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(gpxstat.GpxstatApp.class).getContext().getResourceMap(GpxstatView.class);
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

        newToolItem.setAction(actionMap.get("newFile")); // NOI18N
        newToolItem.setFocusable(false);
        newToolItem.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newToolItem.setName("newToolItem"); // NOI18N
        newToolItem.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar.add(newToolItem);

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
            jSplitPane1.setRightComponent(new TrksegPanel((TrksegType) component));
        } else if (component instanceof WptType) {
            jSplitPane1.setRightComponent(new JLabel("Waypoint"));
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
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTree jTree1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton newToolItem;
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
