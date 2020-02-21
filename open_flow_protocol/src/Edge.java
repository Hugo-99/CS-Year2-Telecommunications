public class Edge {

    private String id;
    private Vertex sourceVertex;
    private Vertex destVertex;
    private int weight;

    Edge(String id, Vertex sourceVertex, Vertex destVertex, int weight) {
        this.id = id;
        this.sourceVertex = sourceVertex;
        this.destVertex = destVertex;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public Vertex getStartPoint() {
        return sourceVertex;
    }

    public Vertex getEndPoint() {
        return destVertex;
    }

    public int getWeight() {
        return weight;
    }

    public String edgeToString() {
        return sourceVertex + " " + destVertex;
    }
}