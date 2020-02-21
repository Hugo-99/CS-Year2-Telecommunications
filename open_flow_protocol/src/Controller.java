import tcdIO.Terminal;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Controller extends Node implements GlobalConstants{


    private String name;
    private String CNTAddress;
    private int CNTSrcPort;
    
    private Terminal terminal;
    private Listener listener;
    private DatagramPacket packet;
    private DatagramPacket temp;
    private DatagramSocket socket;
    private InetSocketAddress dstAddress;
    
    private boolean connectionMapAvailable = false;
    private RConnections[][] connectionMap = new RConnections[GlobalVariables.height][GlobalVariables.width];
    private String[][] RConnectionTable;

    private Hashtable<String, ArrayList<String>> sortedPaths = new Hashtable<String, ArrayList<String>>();;
	private ArrayList<Vertex> vertices;
	private ArrayList<Edge> edges;


    Controller(String name, String CNTAddress, int CNTSrcPort){
        //Assign Variables
        this.vertices = new ArrayList<Vertex>();
        this.edges = new ArrayList<Edge>();
        this.name = name;
        this.CNTAddress = CNTAddress;
        this.CNTSrcPort = CNTSrcPort;
        this.RConnectionTable = new String[(GlobalVariables.height * GlobalVariables.width) * 5][3];

        
        terminal = new Terminal(name);
        terminal.println(name);
        terminal.println("Source Port: " + CNTSrcPort);
        terminal.println("\nController " + name + " is now active");
        terminal.println("________________________________________\n");

        try {
            this.socket = new DatagramSocket(CNTSrcPort);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        listener = new Listener(name);
        listener.startListener();

    }

    protected synchronized void toSend() {
    }

    protected synchronized void toReceive() {
        packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        try {
            socket.receive(packet);
            terminal.println("Received packet from: " + packet.getSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (new String(packet.getData()).contains(RESQUEST_FOR_NEW_PATH)) 
        {
            if (!connectionMapAvailable) 
            {
                temp = packet;
                terminal.println("Controller is now working hard on generating a MAP...");
                generateconnectionMap();
                packet = temp;
                terminal.println("Map Generated.\n");
                connectionMapAvailable = true;
            }
            if (!pathAvailable()) 
            {
                terminal.println("Controller is now working hard on generating a PATH...");
                ArrayList<String> newPath = generatePath(RConnectionTable, getPacketSrc(), getPacketDestination());                
                sortedPaths.put(getPacketDestination(), newPath);
                terminal.println("Path Generated.\n");
            }
            if (pathAvailable()) 
            {
                terminal.println("Controller is now sending out the next destination of the path");
                sendResponse(packet.getPort());
            }
        }
    }

    private void sendResponse(int routerPort) {
        byte[] header = (name + "#" + getPacketDestination() + "#-").getBytes();
        byte[] payload = ("NEXTDST~" + getNextStationFromList(getPacketSrc()) + "}").getBytes();
        byte[] buffer  = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        
        dstAddress = new InetSocketAddress("localhost", routerPort);
        packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        
        try {
            socket.send(packet);
            terminal.println(name + ": Sent Out Next Destination To " + packet.getSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateconnectionMap() {
        int accum = 4;
        int r = 0;
        for (int i = 0;i < GlobalVariables.height; i++) {
            for (int j = 0; j < GlobalVariables.width; j++)
            {
                sendResquestToRouter(STARTING_ROUTERS_PORT + accum, r); //Plus four to hit routers gateway port
                String data = receiveRouterData();
                RConnections entry = parseRouterData(data, getPacketSrc());
                if (j > 0) {
                    entry.setLeftLatency(connectionMap[i][j - 1].getRightLatency());
                }
                if (i > 0) {
                    entry.setTopLatency(connectionMap[i - 1][j].getBottomLatency());
                }
                connectionMap[i][j] = entry;

                Vertex location = new Vertex(entry.getRouterName(), entry.getRouterName());
                if (!isVertextExisted(entry.getRouterName())) {
                    vertices.add(location); //Maybe have this in ifs?
                }
                if (entry.getLeftBond().contains("R") || entry.getLeftBond().contains("c")) {
                    if (!isVertextExisted(entry.getLeftBond())) {
                        vertices.add(new Vertex(entry.getLeftBond(), entry.getLeftBond()));
                    }
                }
                if (entry.getTopBond().contains("R") || entry.getTopBond().contains("c")) {
                    if (!isVertextExisted(entry.getTopBond())) {
                        vertices.add(new Vertex(entry.getTopBond(), entry.getTopBond()));
                    }

                }
                if (entry.getRightBond().contains("R") || entry.getRightBond().contains("c")) {
                    if (!isVertextExisted(entry.getRightBond())) {
                        vertices.add(new Vertex(entry.getRightBond(), entry.getRightBond()));
                    }

                }
                if (entry.getBottomBond().contains("R") || entry.getBottomBond().contains("c")) {
                    if (!isVertextExisted(entry.getBottomBond())) {
                        vertices.add(new Vertex(entry.getBottomBond(), entry.getBottomBond()));
                    }
                }
                accum += 5;
                r++;
            }
        }

        generateEdges();
    }

    public void generateEdges() {
		Vertex aNode;
		String[] bonds;
		int[] latencies;
		int edgeCount = 0;
		for(int i=0; i<GlobalVariables.height; i++)
		{
			for(int j=0; j<GlobalVariables.width; j++)
			{
				RConnections aRouter = connectionMap[i][j];
				aNode = new Vertex(aRouter.getRouterName(),aRouter.getRouterName());
				bonds = aRouter.getBonds();

				latencies = aRouter.getLatencies();
				for(int k=0; k<bonds.length; k++)
				{	
					if(bonds[k]==null)
					{
						
					}
					else if(bonds[k].contains("R") || bonds[k].contains("c"))
					{
						if(!isEdgeExisted(aNode, vertices.get(getVerticesIndex(bonds[k]))))
						{
							edges.add(new Edge("E"+edgeCount, vertices.get(getVerticesIndex(bonds[k])), aNode, latencies[k]));
							edges.add(new Edge("E"+edgeCount, aNode, vertices.get(getVerticesIndex(bonds[k])), latencies[k]));
						}
					}
				}
				edgeCount++;
			}
		}
    }

    private boolean isVertextExisted(String name) {
		for(int i=0; i<vertices.size(); i++)
		{
			if(vertices.get(i).getName().equals(name))
			{
				return true;
			}
		}
		return false;
    }

    private boolean isEdgeExisted(Vertex num1, Vertex num2) {
		for(int i=0; i<edges.size(); i++)
		{
			//consider both directions
			if(edges.get(i).getStartPoint() == num1 && edges.get(i).getEndPoint() == num2)
			{
				return true;
			}
			if(edges.get(i).getStartPoint() == num2 && edges.get(i).getEndPoint() == num1)
			{
				return true;
			}
		}
		return false;
    }

    private int getVerticesIndex(String name) {
		int index = -1;
		if(vertices.size()!=0)
		{
			for(int i=0; i<vertices.size(); i++)
			{
				if(vertices.get(i).getName().equals(name))
				{
					index = i;
					i = vertices.size();
				}
			}
		}
		return index;
    }


    private void sendResquestToRouter(int routerPort, int routerNum) {
        packet = null;
        byte[] payload = ("DATAREQ").getBytes();
        byte[] header = (name + "#" + "R" + routerNum + "#-").getBytes();
        byte[] buffer = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        
        dstAddress = new InetSocketAddress("localhost", routerPort);
        packet = new DatagramPacket(buffer, buffer.length, dstAddress);
        
        try {
            socket.send(packet);
            terminal.println(name + ": Sent Out Router Resquest To " + packet.getSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String receiveRouterData() {
        packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        try {
            socket.receive(packet);
            terminal.println("Received router data packet from: " + packet.getSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (new String(packet.getData()).contains("RDATA_")) {
            return getTableFromPacket();
        }
        return "";
    }

    private String getTableFromPacket() {
    	String data = new String(packet.getData());
        int count = 0;
        int delimCount = 0;
        while (delimCount < 2) {
            if (data.charAt(count) == '#') {
                delimCount++;
            }
            count++;
        }
        int start = count + 7;
        String toReturn = "";
        count = 0;
        int dCount = 0;
        while (dCount < 4) {
            if (data.charAt(start + count) == '\n') {
                dCount++;
            }
            toReturn += data.charAt(start + count);
            count++;
        }
        return toReturn;
    }

    private RConnections parseRouterData(String routerData, String name){
        String[] dataEntries = routerData.split("\n");
        RConnections entry;
        String left = "";
        String right = "";
        String up = "";
        String down = "";

        int leftNum = 0;
        int rightNum = 0;
        int upNum = 0;
        int downNum = 0;

        int leftTime = 0;
        int rightTime = 0;
        int upTime = 0;
        int downTime = 0;


        if (dataEntries[0].contains("R")) {

            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[0].charAt(count) == '#') {
                    delimCount++;
                }
                count++;
            }

            left = dataEntries[0].substring(0,count - 1);
            leftNum = Integer.parseInt(dataEntries[0].substring(1, count - 1));
            int countTwo = 0;
            int delimCountTwo = 0;
            while (delimCountTwo < 3) {
                if (dataEntries[0].charAt(countTwo) == '#') {
                    delimCountTwo++;
                }
                countTwo++;
            }
            int countThree = 0;
            int delimCountThree = 0;
            while (delimCountThree < 4) {
                if (dataEntries[0].charAt(countThree) == '#') {
                    delimCountThree++;
                }
                countThree++;
            }
            leftTime = Integer.parseInt(dataEntries[0].substring(countTwo, countThree - 1));
        }
        if (dataEntries[1].contains("R")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[1].charAt(count) == '#') {
                    delimCount++;
                }
                count++;
            }

            up = dataEntries[1].substring(0,count - 1);
            int countTwo = 0;
            int delimCountTwo = 0;
            while (delimCountTwo < 3) {
                if (dataEntries[1].charAt(countTwo) == '#') {
                    delimCountTwo++;
                }
                countTwo++;
            }
            int countThree = 0;
            int delimCountThree = 0;
            while (delimCountThree < 4) {
                if (dataEntries[1].charAt(countThree) == '#') {
                    delimCountThree++;
                }
                countThree++;
            }
            upTime = Integer.parseInt(dataEntries[1].substring(countTwo, countThree - 1));
        }
        if (dataEntries[2].contains("R")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[2].charAt(count) == '#') {
                    delimCount++;
                }
                count++;
            }

            right = dataEntries[2].substring(0,count - 1);
            rightNum = Integer.parseInt(dataEntries[2].substring(1,count - 1));
            int countTwo = 0;
            int delimCountTwo = 0;
            while (delimCountTwo < 3) {
                if (dataEntries[2].charAt(countTwo) == '#') {
                    delimCountTwo++;
                }
                countTwo++;
            }
            int countThree = 0;
            int delimCountThree = 0;
            while (delimCountThree < 4) {
                if (dataEntries[2].charAt(countThree) == '#') {
                    delimCountThree++;
                }
                countThree++;
            }
            rightTime = Integer.parseInt(dataEntries[2].substring(countTwo, countThree - 1));
        }
        if (dataEntries[3].contains("R")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[3].charAt(count) == '#') {
                    delimCount++;
                }
                count++;
            }

            down = dataEntries[3].substring(0,count - 1);
            downNum = Integer.parseInt(dataEntries[3].substring(1,count - 1));
            int countTwo = 0;
            int delimCountTwo = 0;
            while (delimCountTwo < 3) {
                if (dataEntries[3].charAt(countTwo) == '#') {
                    delimCountTwo++;
                }
                countTwo++;
            }
            int countThree = 0;
            int delimCountThree = 0;
            while (delimCountThree < 4) {
                if (dataEntries[3].charAt(countThree) == '#') {
                    delimCountThree++;
                }
                countThree++;
            }
            downTime = Integer.parseInt(dataEntries[3].substring(countTwo, countThree - 1));
        }
        if (dataEntries[0].contains("c")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[0].charAt(count) == '#') {
                    delimCount++;
                }
                count++;
            }
            left = dataEntries[0].substring(0,count - 1);
        }
        if (dataEntries[1].contains("c")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[1].charAt(count) == '#') {
                    delimCount++;
                }
                count++;
            }
            up = dataEntries[1].substring(0,count - 1);
            upNum = Integer.parseInt(dataEntries[1].substring(1,count - 1));
        }
        if (dataEntries[2].contains("c")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[2].charAt(count) == '#') {
                    delimCount++;
                }
                count++;
            }
            right = dataEntries[2].substring(0,count - 1);
        }
        if (dataEntries[3].contains("c")) {
            int count = 0;
            int delimCount = 0;
            while (delimCount < 1) {
                if (dataEntries[3].charAt(count) == '#') {
                    delimCount++;
                }
                count++;
            }
            down = dataEntries[3].substring(0,count - 1);
        }
        entry = new RConnections(name, left, up, right, down, leftTime, upTime, rightTime, downTime, leftNum, rightNum, upNum, downNum);
        return entry;
    }

    private String getPacketSrc() {

        String data = new String(packet.getData());
        int count = 0;
        while (data.charAt(count) != '#') {
            count++;
        }
        String packetSrc = data.substring(0, count);
        return packetSrc;
    }

    private ArrayList<String> generatePath(String[][] RConnectionTable, String currentRouter, String dstClient) {
		ArrayList<String> path = new ArrayList<String>();
		Graph graph = new Graph(vertices, edges);
		DjikstraAlgorithm calculation = new DjikstraAlgorithm(graph);
		
		int currentRouterIndex = -1;
		int dstClientindex = -1;
		for(int i=0; i<vertices.size(); i++)
		{
			if(vertices.get(i).getName().equals(currentRouter))
			{
				currentRouterIndex = i;
			}
			else if(vertices.get(i).getName().equals(dstClient))
			{
				dstClientindex = i;
			}
		}
		calculation.runCalculation(vertices.get(currentRouterIndex), dstClient);
		path = calculation.generatePath(vertices.get(dstClientindex));
		return path;
    }

    private boolean pathAvailable() {
		for(int i=0; i<sortedPaths.size(); i++)
		{
			if(sortedPaths.containsKey(getPacketDestination()))
			{
				return true;
			}
		}
		return false;
    }

    private String getPacketDestination() {
        String dst = "";
        byte[] packetData = packet.getData();
        String data = new String(packetData);
        int count = 0;
        int firstDelim = 0;
        int secondDelim = 0;
        int delimCount = 0;
        while (delimCount < 2) {
            if (data.charAt(count) == '#') {
                delimCount++;
                if (firstDelim == 0) {
                    firstDelim = count;
                }
                else if (secondDelim == 0) {
                    secondDelim = count;
                }
            }
            count++;
        }
        String dstString = data.substring((firstDelim + 1), (secondDelim));
        terminal.println(name + ": Destination Client Is " + dstString);
        return dstString;
    }

    private String getNextStationFromList(String currentAddress) {
		String finalDst = getPacketDestination();
		ArrayList<String> temp = null;
		if(sortedPaths.containsKey(finalDst))
		{
			temp = sortedPaths.get(finalDst);
		}
		int index = 0;
        for (int i = 0; i < temp.size(); i++) {
            if (temp.get(i).equals(currentAddress)) {
                index = i;
            }
        }
        String nextStation = temp.get(index + 1);
        return nextStation;
    }

}