import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DjikstraAlgorithm {

	private String finalDstClient;
	
	private ArrayList<Vertex> vertices;
	private ArrayList<Edge> edges;
	private Set<Vertex> visitedVertices;
	private Set<Vertex> unVisitedVertices;
	private HashMap<Vertex,Integer> distances;
	private HashMap<Vertex, Vertex> unsortedPath;
	
	DjikstraAlgorithm(Graph graph){
		this.vertices = graph.getVertices();
		this.edges = graph.getEdges();
		distances = new HashMap<Vertex,Integer>();
		visitedVertices = new HashSet<Vertex>();
		unVisitedVertices = new HashSet<Vertex>();
		unsortedPath = new HashMap<Vertex, Vertex>();
	}
	
	public void runCalculation(Vertex srcRouter, String finalDstClient) {
		this.finalDstClient = finalDstClient;
		unVisitedVertices.add(srcRouter);
		distances.put(srcRouter, 0);
		while(unVisitedVertices.size()!=0)
		{
			Vertex aNode = getCheapest(unVisitedVertices);
			visitedVertices.add(aNode);
			unVisitedVertices.remove(aNode);
			searchCheapestPath(aNode);
		}
	}
	
	public void searchCheapestPath(Vertex aNode) {
		ArrayList<Vertex> neighbourNodes = getNeighbours(aNode);
		for(int i=0; i<neighbourNodes.size(); i++)
		{
			Vertex cmp = neighbourNodes.get(i);
			if(cmp.getName().contains("R"))
			{
				if(getShortestDistance(cmp) > (getShortestDistance(aNode) + getDistance(aNode, cmp)))
				{
					distances.put(cmp,getShortestDistance(aNode) + getDistance(aNode, cmp));
					unVisitedVertices.add(cmp);
					unsortedPath.put(cmp, aNode);
				}
			}
			else if(cmp.getName().equals(finalDstClient))
			{
				distances.put(cmp,getShortestDistance(aNode) + getDistance(aNode, cmp));
				unVisitedVertices.add(cmp);
				unsortedPath.put(cmp, aNode);
			}
		}
	}
	
	public ArrayList<String> generatePath(Vertex destination){
		ArrayList<String> path = new ArrayList<String>();
		if(unsortedPath.get(destination)==null)
		{
			return null;
		}
		Vertex nodeFromBack = destination;
		path.add(nodeFromBack.getName());
		while(unsortedPath.get(nodeFromBack)!=null)
		{
			nodeFromBack = unsortedPath.get(nodeFromBack);
			path.add(nodeFromBack.getName());
		}
		Collections.reverse(path);
		return path;
	}
	
	public int getDistance(Vertex startPoint, Vertex endPoint)
	{
		for(int i=0; i<edges.size(); i++)
		{
			Edge anEdge = edges.get(i);
			if(anEdge.getStartPoint().equals(startPoint) && anEdge.getEndPoint().equals(endPoint))
			{
				return anEdge.getWeight();
			}
		}
		return -1;
	}
	
	public ArrayList<Vertex> getNeighbours(Vertex aNode){
		ArrayList<Vertex> neighbours = new ArrayList<Vertex>();
		for(int i=0; i<edges.size(); i++)
		{
			Edge anEdge = edges.get(i);
			if(anEdge.getStartPoint().equals(aNode) && !isVisited(anEdge.getEndPoint()))
			{
				neighbours.add(anEdge.getEndPoint());
			}
		}
		return neighbours;
	}
	
	public Vertex getCheapest(Set<Vertex> vertices) {
		Vertex cheapest = null;
		for(Vertex cmp : vertices)
		{
			if(cheapest==null)
			{
				cheapest = cmp;
			}
			if(getShortestDistance(cmp)<getShortestDistance(cheapest))
			{
				cheapest = cmp;
			}
		}
		return cheapest;
	}
	
	public int getShortestDistance(Vertex dst) {
		Integer shrtD = distances.get(dst);
		if(shrtD == null)
		{
			return Integer.MAX_VALUE;
		}
		else {
			return distances.get(dst);
		}
	}
	
	public boolean isVisited(Vertex aNode) {
		if(visitedVertices.contains(aNode))
		{
			return true;
		}
		return false;
	}
}
