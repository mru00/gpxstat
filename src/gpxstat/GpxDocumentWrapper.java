/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gpxstat;

import com.topografix.gpx.x1.x1.GpxDocument;
import java.io.File;

/**
 *
 * @author mru
 */
public class GpxDocumentWrapper {

    public final File file;
    public final GpxDocument doc;

    public GpxDocumentWrapper(File file, GpxDocument doc) {
        this.file = file;
        this.doc = doc;
    }

    public String getFileName() {
        if (file == null) {
            return "unsaved document";
        }
        return file.getName();
    }
}
