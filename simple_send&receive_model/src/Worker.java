import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 *
 * Client class
 *
 * An instance accepts input from the user, marshalls this into a datagram, sends
 * it to a server instance and then waits for a reply. When a packet has been
 * received, the type of the packet is checked and if it is an acknowledgement,
 * a message is being printed and the waiting main method is being notified.
 *
 */
public class Worker extends Node implements Runnable {
	static final int DEFAULT_SRC_PORT = 50000; // Port of the client
	static final int DEFAULT_DST_PORT = 50001; // Port of the server
	static final String DEFAULT_DST_NODE = "localhost";	// Name of the host for the server

	static final int HEADER_LENGTH = 3; // Fixed length of the header
	static final int TYPE_POS = 0; // Position of the type within the header

	static final byte TYPE_UNKNOWN = 0;

	static final byte TYPE_WORKER = 2; // Indicating a string payload
	static final int LENGTH_POS = 1;
	static final int CONTENT_LENGTH_POS = 2;
	
	static final byte ADDED_WORKER = 2;   // Indicating an acknowledgement
	static final byte REMOVED_WORKER = 3;
	static final int ACTION_POS = 1; // Position of the acknowledgement type in the header
	static final byte ACK_ALLOK = 10; // Indicating that everything is ok
	
	static final int NAME_LENGTH_POS = 3;
	
	static final int YES = 1;
	static final int NO = 0;
	
	static final byte TYPE_ACK = 2;
	static final int FROM_BROKER_HEADER_LENGTH = 3;
	static final byte WORK_DONE = 3;

	Terminal terminal;
	InetSocketAddress dstAddress;
	boolean quit = false;

	/**
	 * Constructor
	 *
	 * Attempts to create socket at given port and create an InetSocketAddress for the destinations
	 */
	Worker(Terminal terminal, String dstHost, int dstPort, int srcPort) {
		try {
			this.terminal= terminal;
			dstAddress= new InetSocketAddress(dstHost, dstPort);
			socket= new DatagramSocket(srcPort);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}


	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		byte[] data;
		byte[] buffer;
		int workTimes;
		String content;

		data = packet.getData();
		if(data[TYPE_POS]==TYPE_ACK)
		{
			switch(data[ACTION_POS]) {
			case ADDED_WORKER:
				terminal.println("Request accpeted. Waiting for a job.");
				this.notify();
				break;
			case REMOVED_WORKER:
				terminal.println("Successfully quit.");
				this.notify();
				break;
			default:
				terminal.println("Unexpected packet" + packet.toString());
			}
		}
		else 
		{
			workTimes = (int)data[ACTION_POS];
			buffer = new byte[data[CONTENT_LENGTH_POS]];
			System.arraycopy(data, FROM_BROKER_HEADER_LENGTH, buffer, 0, buffer.length);
			content = new String(buffer);
			for(int i=0; i<workTimes; i++)
			{
				terminal.println("count " + (i+1) + " :" + content);
			}
			quit = false;
			
			try {
				reportBack(packet, getName());
				sendMessage();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void run() {
		try {
			setName();
			sendMessage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setName() throws Exception {
		String input = terminal.read("Please enter your name: ");
		terminal.setName(input);
	}
	
	public String getName() {
		return terminal.getName();
	}
	
	public void sendMessage() throws Exception {
		byte[] data= null;
		byte[] buffer;
		DatagramPacket packet= null;
		String input;

		buffer = getName().getBytes();
		while(!quit)
		{			
			input = terminal.read("Would you want to work ('Y'/'N'): ");
			if(input.equals("N") || input.equals("n"))
			{
				terminal.println("Request to quit the job.");
				data = new byte[HEADER_LENGTH + buffer.length];
				data[TYPE_POS] = TYPE_WORKER;
				data[ACTION_POS] = NO;
				data[NAME_LENGTH_POS] = (byte)buffer.length;
				System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
				packet= new DatagramPacket(data, data.length);
				packet.setSocketAddress(dstAddress);
				socket.send(packet);
				terminal.println("Message sent");
				//input = terminal.read("Rejoin type 'Y':");
				this.wait();
			}
			else if(input.equals("Y") || input.equals("y"))
			{
				terminal.println("Waiting for broker's respond.");
				data = new byte[HEADER_LENGTH + buffer.length];
				data[TYPE_POS] = TYPE_WORKER;
				data[ACTION_POS] = YES;
				data[NAME_LENGTH_POS] = (byte)buffer.length;
				System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
				packet= new DatagramPacket(data, data.length);
				packet.setSocketAddress(dstAddress);
				socket.send(packet);
				terminal.println("Message sent");
				quit = true;
				this.wait();
			}
			else {
				terminal.println("Please type 'Y' or 'N' only.");
			}
		}
	}

	public synchronized void reportBack(DatagramPacket packet, String name) {
		
		boolean exit=false;
		String decision = terminal.read("Would you want to report back?(Y/N): ");
		while(!exit)
		{
			if(decision.equals("Y") || decision.equals("y"))
			{
				sendAck(packet.getSocketAddress(), name);
				terminal.println("Progress sent.");
				exit=true;
			}
			else if(decision.equals("N") || decision.equals("n"))
			{
				terminal.println("Progress not send.");
				exit=true;
			}
			else
			{
				terminal.println("Please type 'Y' or 'N' only.");
			}
		}

		
	}
	
	public synchronized void sendAck(SocketAddress address, String name) {
		try {
			byte[] data;
			byte[] buffer = name.getBytes();
			data = new byte[HEADER_LENGTH + buffer.length];
			data[TYPE_POS] = TYPE_WORKER;
			data[ACTION_POS] = WORK_DONE;
			data[NAME_LENGTH_POS] = (byte)buffer.length;
			System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
			DatagramPacket ack = new DatagramPacket(data, data.length);
			ack.setSocketAddress(address);
			socket.send(ack);
			this.notify();
		}
		catch(Exception e) {e.printStackTrace();}
	}

}