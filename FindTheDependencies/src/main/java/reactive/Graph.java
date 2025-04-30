package reactive;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import javax.swing.*;

public class Graph {
    public static void main(String[] args) {
        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();

        graph.getModel().beginUpdate();
        try {
            Object v1 = graph.insertVertex(parent, null, "Nodo A", 20, 20, 80, 30);
            Object v2 = graph.insertVertex(parent, null, "Nodo B", 240, 150, 80, 30);
            graph.insertEdge(parent, null, "Arco", v1, v2);
        } finally {
            graph.getModel().endUpdate();
        }

        JFrame frame = new JFrame("JGraphX");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new mxGraphComponent(graph));
        frame.setSize(400, 320);
        frame.setVisible(true);
    }
}
