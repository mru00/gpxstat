/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gpxstat.ui;

import com.topografix.gpx.x1.x1.TrkType;
import com.topografix.gpx.x1.x1.TrksegType;
import com.topografix.gpx.x1.x1.WptType;
import gpxstat.GpxDocumentWrapper;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author mru
 */
public class GpxTreeModel implements TreeModel {

    private final List<GpxDocumentWrapper> documents = new ArrayList<GpxDocumentWrapper>();
    private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();

    public GpxTreeModel() {
    }

    public void addDocument(GpxDocumentWrapper document) {
        documents.add(document);
        fireStructureChanged(new TreePath( new Object[]{documents}));
    }

    public void closeDocument(GpxDocumentWrapper document) {
        documents.remove(document);
        fireStructureChanged(new TreePath(documents));
    }



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
        listeners.add(l);
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    void fireStructureChanged(TreePath pathForRow) {
        TreeModelEvent ev = new TreeModelEvent(this, pathForRow);

        for (TreeModelListener l : listeners) {
            l.treeStructureChanged(ev);
        }
    }

    void fireNodesInserted(TreePath path) {
        TreeModelEvent ev = new TreeModelEvent(this, path);

        for (TreeModelListener l : listeners) {
            l.treeNodesInserted(ev);
        }
    }

    void fireNodesRemoved(TreePath path) {
        TreeModelEvent ev = new TreeModelEvent(this, path);

        for (TreeModelListener l : listeners) {
            l.treeNodesRemoved(ev);
        }
    }

}
