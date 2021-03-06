// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Geometry;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

/**
 * This allows to select a polygon/multipolgon by an internal point.
 */
public class SelectByInternalPointAction extends JosmAction {

    /**
     * Returns the surrounding polygons/multipolgons
     * ordered by their area size (from small to large)
     * which contain the internal point.
     *
     * @param internalPoint the internal point.
     */
    public static Collection<OsmPrimitive> getSurroundingObjects(EastNorth internalPoint) {
        final Node n = new Node(internalPoint);
        final TreeMap<Double, OsmPrimitive> found = new TreeMap<>();
        for (Way w : getCurrentDataSet().getWays()) {
            if (w.isUsable() && w.isClosed()) {
                if (Geometry.nodeInsidePolygon(n, w.getNodes())) {
                    found.put(Geometry.closedWayArea(w), w);
                }
            }
        }
        for (Relation r : getCurrentDataSet().getRelations()) {
            if (r.isUsable() && r.isMultipolygon()) {
                if (Geometry.isNodeInsideMultiPolygon(n, r, null)) {
                    for (RelationMember m : r.getMembers()) {
                        if (m.isWay() && m.getWay().isClosed()) {
                            found.values().remove(m.getWay());
                        }
                    }
                    // estimate multipolygon size by its bounding box area
                    BBox bBox = r.getBBox();
                    EastNorth en1 = Main.map.mapView.getProjection().latlon2eastNorth(bBox.getTopLeft());
                    EastNorth en2 = Main.map.mapView.getProjection().latlon2eastNorth(bBox.getBottomRight());
                    double s = Math.abs((en1.east() - en2.east()) * (en1.north() - en2.north()));
                    if (s == 0) s = 1e8;
                    found.put(s, r);
                }
            }
        }
        return found.values();
    }


    /**
     * Returns the smallest surrounding polygon/multipolgon which contains the internal point.
     *
     * @param internalPoint the internal point.
     */
    public static OsmPrimitive getSmallestSurroundingObject(EastNorth internalPoint) {
        final Collection<OsmPrimitive> surroundingObjects = getSurroundingObjects(internalPoint);
        return surroundingObjects.isEmpty() ? null : surroundingObjects.iterator().next();
    }

    /**
     * Select a polygon or multipolgon by an internal point.
     *
     * @param internalPoint the internal point.
     * @param doAdd         whether to add selected polygon to the current selection.
     * @param doRemove      whether to remove the selected polygon from the current selection.
     */
    public static void performSelection(EastNorth internalPoint, boolean doAdd, boolean doRemove) {
        final Collection<OsmPrimitive> surroundingObjects = getSurroundingObjects(internalPoint);
        if (surroundingObjects.isEmpty()) {
            return;
        } else if (doRemove) {
            final Collection<OsmPrimitive> newSelection = new ArrayList<>(getCurrentDataSet().getSelected());
            newSelection.removeAll(surroundingObjects);
            getCurrentDataSet().setSelected(newSelection);
        } else if (doAdd) {
            final Collection<OsmPrimitive> newSelection = new ArrayList<>(getCurrentDataSet().getSelected());
            newSelection.add(surroundingObjects.iterator().next());
            getCurrentDataSet().setSelected(newSelection);
        } else {
            getCurrentDataSet().setSelected(surroundingObjects.iterator().next());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException();
    }
}
