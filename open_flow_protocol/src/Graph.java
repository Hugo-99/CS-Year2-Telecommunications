import java.util.ArrayList;

public class Graph {

    private ArrayList<Vertex> vertices;
    private ArrayList<Edge> edges;

    Graph (ArrayList<Vertex> vertices, ArrayList<Edge> edges) {
        this.vertices = vertices;
        this.edges = edges;
    }

    public ArrayList<Vertex> getVertices() {
        return vertices;
    }

    public ArrayList<Edge> getEdges() {
        return edges;
    }
}