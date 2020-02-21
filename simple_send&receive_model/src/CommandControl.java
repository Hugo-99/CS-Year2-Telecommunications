import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class CommandControl extends Node implements Runnable {
	static final int DEFAULT_SRC_PORT = 50000; // Port of the client
	static final int DEFAULT_DST_PORT = 50001; // Port of the server
	static final String DEFAULT_DST_NODE = "localhost";	// Name of the host for the server

	static final int HEADER_LENGTH = 4; // Fixed length of the header
	static final int TYPE_POS = 0; // Position of the type within the header


	static final byte TYPE_UNKNOWN = 0;

	static final byte TYPE_CANDC = 1; // Indicating a string payload
	static final int WORK_TIMES_POS = 2;

	static final int WORKERS_NUM_POS = 3;
	static final byte TYPE_ACK = 2;   // Indicating an acknowledgement
	
	static final int CONTENT_LENGTH_POS = 1;
	static final int TYPE_PROGRESS = 3;
	static final int NAME_LENGTH_POS = 1;
	static final int HEADER_LENGTH_WORKER = 2;
	Terminal terminal;
	InetSocketAddress dstAddress;
	
	CommandControl( Terminal terminal, int port , String dstHost, int dstPort) {
		try {
			this.terminal= terminal;
			socket= new DatagramSocket(port);
			dstAddress = new InetSocketAddress(dstHost, dstPort);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}
	@Override
	public synchronized void run() {
		try {
			terminal.println("Waiting for a new work desription");
			this.sendMessage();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public synchronized void sendMessage() throws Exception {
		byte[] data= null;
		byte[] buffer= null;
		DatagramPacket packet= null;
		String input;
		boolean end1 = false;
		boolean end2 = false;
		int workTimes = 0;
		int workers = 0;

		while(!end1)
		{
			terminal.println("Please enter the work times:");
			input = terminal.read("Resquest: ");
		    if(isNumeric(input))
		    {
		    	workTimes = Integer.parseInt(input);
		    	if(workTimes>=0 && workTimes<128)
		    	{
		    		end1 = true;
		    	}
		    }
		    else
		    {
		    	terminal.println("Error. Please enter with only numbers between 0 and 128.\n");
		    }
		}
		terminal.println(Integer.toString(workTimes));
		while(!end2)
		{
			terminal.println("Please enter the workers needed:");
			input = terminal.read("Resquest: ");
		    if(isNumeric(input))
		    {
		    	workers = Integer.parseInt(input);
		    	if(workers>=0 && workers<128)
		    	{
		    		end2 = true;
		    	}
		    }
		    else
		    {
		    	terminal.println("Error. Please enter with only numbers between 0 and 128.\n");
		    }
		}
		terminal.println(Integer.toString(workers));
		
		terminal.println("Please enter the work description:");
		input = terminal.read("Resquest: ");

		buffer = input.getBytes();
		//buffer2 = BigInteger.valueOf(workTimes).toByteArray();
		//result = addAll(buffer1, buffer2);
		
		data = new byte[HEADER_LENGTH + buffer.length];
		data[TYPE_POS] = TYPE_CANDC;
		data[CONTENT_LENGTH_POS] = (byte)buffer.length;
		data[WORK_TIMES_POS] = (byte)workTimes;
		data[WORKERS_NUM_POS] = (byte)workers;
		System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
		
		terminal.println("\nSending work description to Broker...");
		packet= new DatagramPacket(data, data.length);
		packet.setSocketAddress(dstAddress);
		socket.send(packet);
		terminal.println("Work description sent to broker.");
		this.wait();

	}
	@Override
	public synchronized void onReceipt(DatagramPacket packet) {
		byte[] data;
		byte[] buffer;
		data = packet.getData();
		switch(data[TYPE_POS]) {
		case TYPE_ACK:
			terminal.println("Respond received");
			this.notify();
			break;
		case TYPE_PROGRESS:
			int nameLength = (int)data[NAME_LENGTH_POS];
			buffer = new byte[nameLength];
			System.arraycopy(data, HEADER_LENGTH_WORKER, buffer, 0, nameLength);
			String workerName = new String(buffer);
			terminal.println(workerName + " has done the work.");
			this.notify();
			break;
		default:
			terminal.println("Unexpected packet" + packet.toString());
		}
		
	}

	public static boolean isNumeric(String strNum) {
	    try {
	        int d = Integer.parseInt(strNum);
	    } catch (NumberFormatException | NullPointerException nfe) {
	        return false;
	    }
	    return true;
	}
	
}
