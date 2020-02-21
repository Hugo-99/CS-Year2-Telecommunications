import tcdIO.Terminal;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class Router extends Node implements GlobalConstants {
	private String name;
	private String routerAddress;
	private int[] portList;
	private int[] dstPortList;
	private ArrayList<FlowTableEntry> flowTable;
	private boolean needToRequestController;
	
	private String payload; 
	private DatagramPacket packet;
	private DatagramPacket CNTPacket;
	private InetSocketAddress dstPacketAddress;
	private ArrayList<DstTableRecord> dstTableRecords = new ArrayList();
	private DatagramSocket[] socketList = new DatagramSocket[ROUTER_PORTS];
	
	private Terminal terminal;
	
	Router(String name, String routerAddress, int[] portList,
			int[] dstPortList, ArrayList<FlowTableEntry> flowTable){
		this.name = name;
		this.routerAddress = routerAddress;
		this.portList = portList;
		this.dstPortList = dstPortList;
		this.flowTable = flowTable;
		this.needToRequestController = false;
		this.payload ="";
		
		try {
			for(int i=0; i<socketList.length; i++)
			{
				socketList[i] = new DatagramSocket(portList[i]);
				socketList[i].setSoTimeout(TIME_DELAY_PERIOD);
			}
		} catch (SocketException e) {
            e.printStackTrace();
        }
		terminal = new Terminal(name);
		terminal.println("\n"+name+" socket ports:");
		printSocketPorts();
		terminal.println("\n"+name+" destination ports:");
		printDstPorts();
		terminal.println("\n"+name+" is now active.\n");
		
		//Initialize listener
		listener = new Listener(name);
		listener.startListener();
		
	}

    //toSend() only called when router needs to query controller
    @Override
    protected synchronized void toSend() {
		if(needToRequestController)
		{
			payload = getPacketPayload();
			String packetDestination = getPacketDestination();
			constructResquestPacket(packetDestination);
			try {
				socketList[4].send(CNTPacket);
			}catch(IOException e) {}
			terminal.println("Unknown path for " + packetDestination);
			terminal.println(name + " sent request to:" + CNTPacket.getSocketAddress()+"\n");
			needToRequestController = false;
		}
	}

    @Override
    protected synchronized void toReceive() {
        packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
        for (DatagramSocket currentSocket : socketList) {
            try {
                String packetDestination = "";
                currentSocket.receive(packet);
                packetDestination = getPacketDestination();
    			terminal.println(name + " received packet from: "+ packet.getSocketAddress()+"\n");
    			if(dstTableContainsDst(packetDestination))
    			{
    				int index = 0;
    				for(int i=0; i<dstTableRecords.size(); i++)
    				{
    					if(dstTableRecords.get(i).getDestination().equals(packetDestination))
    					{
    						index = i;
    					}
    					DstTableRecord entry = dstTableRecords.get(index);
    					sendPacketOn(packetDestination, getPacketPayload(), index, flowTable.get(entry.getCurrentSocketNum()).getPortToSendTo());
    				}
    			}
                else if ((new String(packet.getData())).contains("DATAREQ")) {
                    sendControllerRouterData();
                }
                else if ((new String(packet.getData())).contains("NEXTDST")) { 
                    String nextDest = parseNextDest();
                    String finalDest = getPacketDestination();
                    terminal.println("\nNext Destination From Controller Command: " + nextDest);
                    for (int i = 0; i < flowTable.size(); i++) {
                        if (flowTable.get(i).getEntry().equals(nextDest) && !dstTableContainsDst(nextDest)) {
                        	dstTableRecords.add(new DstTableRecord(packetDestination, nextDest, getIndexInFlowTable(nextDest)));
                            sendPacketOn(finalDest, payload, i, flowTable.get(i).getPortToSendTo());
                            packet = null;
                            i = flowTable.size();
                        }
                    }
                }
                else {
                	needToRequestController = true;
                }
            } catch (IOException e) {}
        }
    }

    private int getIndexInFlowTable(String entry) {
        for (int i = 0; i < flowTable.size(); i++) {
            if (flowTable.get(i).getEntry().equals(entry)) {
                return i;
            }
        }
        return -1;
    }

    private void sendPacketOn(String finalDest, String packetPayload, int portToSendThrough, int portToSendTo) {
        DatagramPacket newPacket = null;
        String s = packetPayload + "-";
        byte[] payload = s.getBytes();
        byte[] header = (name + "#" + finalDest + "#-").getBytes();
        byte[] buffer = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        dstPacketAddress = new InetSocketAddress(routerAddress, portToSendTo);
        newPacket = new DatagramPacket(buffer, buffer.length, dstPacketAddress);
        try {
            socketList[portToSendThrough].send(newPacket);
            terminal.println(name + ": Forward To " + newPacket.getSocketAddress());
        } catch (IOException e) {}
    }

    private String getPacketPayload() {
        int countOne = 0;
        byte[] data = packet.getData();
        while (data[countOne] != '-') {
            countOne++;
        }
        int countTwo = countOne + 1;
        while (data[countTwo] != '-') {
            countTwo++;
        }
        String dataStr = new String(packet.getData());
        return dataStr.substring(countOne + 1, countTwo);
    }

    private void printDstPorts() {
        for (int i = 0; i < portList.length; i++) {
            if (i == 0)
                terminal.println("Left="+String.valueOf(dstPortList[i]));
            else if (i == 1)
                terminal.println("Top="+String.valueOf(dstPortList[i]));
            else if (i == 2)
                terminal.println("Right="+String.valueOf(dstPortList[i]));
            else if (i == 3)
                terminal.println("Bottom="+String.valueOf(dstPortList[i]));
            else if (i == 4)
                terminal.println("Controller="+String.valueOf(dstPortList[i]));
        }
    }

    private void printSocketPorts() {
        for (int i = 0; i < portList.length; i++) {
            if (i == 0)
                terminal.println("Left="+String.valueOf(socketList[i].getLocalPort()));
            else if (i == 1)
                terminal.println("Top="+String.valueOf(socketList[i].getLocalPort()));
            else if (i == 2)
                terminal.println("Right="+String.valueOf(socketList[i].getLocalPort()));
            else if (i == 3)
                terminal.println("Bottom="+String.valueOf(socketList[i].getLocalPort()));
            else if (i == 4)
                terminal.println("Controller="+String.valueOf(socketList[i].getLocalPort()));
        }
    }

    private String flowTableToString () {
        String table = "";
        for (int i = 0; i < flowTable.size(); i++) {
            table += (flowTable.get(i).getEntry() + "#" + flowTable.get(i).getPacketReceivedPort() + "#"
                    + flowTable.get(i).getPortToSendTo() + "#" + flowTable.get(i).getLinkLatency() + "#\n");
        }
        return (table);
    }

    private String getPacketDestination() {

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
        String dstString = data.substring(firstDelim + 1, secondDelim);
        terminal.println(name + ": Destination client is " + dstString);
        return dstString;
    }

    private boolean dstTableContainsDst(String toCompare) {
        if (dstTableRecords.size() == 0) {
            return false;
        }
        DstTableRecord entry;
        for (int i = 0; i < dstTableRecords.size(); i++) {
            entry = dstTableRecords.get(i);
            if (entry.getDestination().contains(toCompare)) {
                return true;
            }
        }
        return false;
    }

    private void constructResquestPacket(String packetDstToQuery) {
        CNTPacket = null;
        String s = RESQUEST_FOR_NEW_PATH;
        byte[] payload = s.getBytes();
        byte[] header = (name + "#" + packetDstToQuery + "#-").getBytes();
        byte[] buffer = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        dstPacketAddress = new InetSocketAddress(routerAddress, dstPortList[4]);
        CNTPacket = new DatagramPacket(buffer, buffer.length, dstPacketAddress);
    }

    private void sendControllerRouterData() {
    	CNTPacket = null;
        String s = "RDATA_" + flowTableToString();
        byte[] payload = s.getBytes();
        byte[] header = (name + "#" + "CNT" + "#-").getBytes();
        byte[] buffer = new byte[header.length + payload.length];
        System.arraycopy(header, 0, buffer, 0, header.length);
        System.arraycopy(payload, 0, buffer, header.length, payload.length);
        dstPacketAddress = new InetSocketAddress(routerAddress, dstPortList[4]);
        CNTPacket = new DatagramPacket(buffer, buffer.length, dstPacketAddress);
        try {
            socketList[0].send(CNTPacket);
            terminal.println("Sending the Controller "+name+"'s flow table "+ "\n");
            terminal.println(name + ": Sent Out Router Data " + CNTPacket.getSocketAddress());
        } catch (IOException e) {}
    }

    private String parseNextDest() {
        byte[] data = packet.getData();
        String pData = new String(data);
        int count = 0;
        while (pData.charAt(count) != '~') {
            count++;
        }
        int countTwo = 0;
        while (pData.charAt(countTwo) != '}') {
            countTwo++;
        }
        return pData.substring(count+1, countTwo);
    }
}