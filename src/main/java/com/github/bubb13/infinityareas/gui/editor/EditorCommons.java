
package com.github.bubb13.infinityareas.gui.editor;

import com.github.bubb13.infinityareas.gui.dialog.WarningAlertTwoOptions;
import com.github.bubb13.infinityareas.misc.InstanceHashMap;

import java.awt.Toolkit;
import java.util.ArrayList;

public final class EditorCommons
{
    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static void onBisectLine(final Editor editor)
    {
        if (editor.selectedCount() != 2)
        {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        final RenderableVertex[] selectedVertices = new RenderableVertex[2];
        int i = 0;

        for (final Renderable renderable : editor.selectedObjects())
        {
            if (!(renderable instanceof RenderableVertex renderableVertex))
            {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            selectedVertices[i++] = renderableVertex;
        }

        final RenderableVertex renderableVertex1;
        final RenderableVertex renderableVertex2;

        if (selectedVertices[0].next() == selectedVertices[1])
        {
            renderableVertex1 = selectedVertices[0];
            renderableVertex2 = selectedVertices[1];
        }
        else if (selectedVertices[1].next() == selectedVertices[0])
        {
            renderableVertex1 = selectedVertices[1];
            renderableVertex2 = selectedVertices[0];
        }
        else
        {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        final GenericVertex vertex1 = renderableVertex1.getVertex();
        final GenericVertex vertex2 = renderableVertex2.getVertex();

        final int finalX = vertex2.x() - (vertex2.x() - vertex1.x()) / 2;
        final int finalY = vertex2.y() - (vertex2.y() - vertex1.y()) / 2;

        renderableVertex1.addNewVertexAfter(finalX, finalY);
        editor.unselectAll();
        editor.requestDraw();
    }

    public static void deleteSelected(final Editor editor)
    {
        final InstanceHashMap<RenderablePolygon, VerticesDeleteInfo> verticesDeleteInfoMap = new InstanceHashMap<>();
        final ArrayList<Renderable> otherSelectedObjects = new ArrayList<>();

        for (final Renderable renderable : editor.selectedObjects())
        {
            if (renderable instanceof RenderableVertex vertex)
            {
                final VerticesDeleteInfo deleteInfo = verticesDeleteInfoMap.computeIfAbsent(
                    vertex.getRenderablePolygon(), (ignored) -> new VerticesDeleteInfo());

                deleteInfo.toDelete.add(vertex);
            }
            else
            {
                otherSelectedObjects.add(renderable);
            }
        }

        final boolean[] noContinue = new boolean[] { false };

        for (final var entry : verticesDeleteInfoMap.entries())
        {
            final RenderablePolygon polygon = entry.getKey();
            final VerticesDeleteInfo deleteInfo = entry.getValue();
            final int numVertices = polygon.getRenderablePolygonVertices().size();
            final int numVerticesToDelete = deleteInfo.toDelete.size();
            final int remainingVertices = numVertices - numVerticesToDelete;

            if (remainingVertices > 0 && remainingVertices < 3)
            {
                noContinue[0] = true;

                WarningAlertTwoOptions.openAndWait(
                    "The delete operation will result in polygon(s) with less than 3 vertices. These " +
                        "polygons will be deleted.\n\n" +
                        "Do you still wish to perform the delete operation?",
                    "Yes", () -> noContinue[0] = false,
                    "Cancel", null);

                if (noContinue[0])
                {
                    return;
                }
                break;
            }
        }

        for (final var entry : verticesDeleteInfoMap.entries())
        {
            final RenderablePolygon polygon = entry.getKey();
            final VerticesDeleteInfo deleteInfo = entry.getValue();
            final int numVertices = polygon.getRenderablePolygonVertices().size();
            final int numVerticesToDelete = deleteInfo.toDelete.size();
            final int remainingVertices = numVertices - numVerticesToDelete;

            if (remainingVertices < 3)
            {
                polygon.delete();
            }
            else
            {
                for (final RenderableVertex vertex : deleteInfo.toDelete)
                {
                    vertex.delete();
                }
            }
        }

        for (final Renderable renderable : otherSelectedObjects)
        {
            renderable.delete();
        }
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private EditorCommons() {}

    /////////////////////
    // Private Classes //
    /////////////////////

    private static class VerticesDeleteInfo
    {
        public final ArrayList<RenderableVertex> toDelete = new ArrayList<>();
    }
}
