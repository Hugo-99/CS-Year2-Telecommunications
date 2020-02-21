import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;

public class Broker extends Node implements Runnable {
	static final int DEFAULT_PORT = 50001;

	static final int HEADER_LENGTH = 2;
	static final int HEADER_LENGTH_WORKER = 3;
	static final int HEADER_LENGTH_CANDC = 4;
	static final int TYPE_POS = 0;
	
	static final byte TYPE_UNKNOWN = 0;
	
	static final int WORK_TIMES_POS = 2;
	static final int WORKERS_NUM_POS = 3;
	
	static final byte TYPE_CANDC = 1;
	static final byte TYPE_WORKER = 2;
	static final int LENGTH_POS = 1;
	
	static final byte TYPE_ACK = 2;
	static final byte ADD_WORKER = 2;
	static final byte REMOVE_WORKER = 3;
	static final int ACTION_POS = 1;
	static final byte ACK_ALLOK = 10;
	
	static final int REQUEST_YES = 1;
	static final int REQUEST_NO = 0;
	
	static final String WORKER = "Worker ";
	static final String CANDC = "Comand & Control";

	static final byte TYPE_WORK = 0;

	static final int WORK_TIMES_WORKERS = 1;

	static final int TO_WORKERS_HEADER_LENGTH = 3;
	
	static final int NAME_LENGTH_POS = 3;

	static final int CONTENT_LENGTH = 2;

	static final byte WORK_DONE = 3;

	static final byte TYPE_RESULT = 3;
	
	Terminal terminal;
	InetSocketAddress dstAddress;
	ArrayList<SocketAddress> socketsAddress = new ArrayList<SocketAddress>();
	ArrayList<Integer> workTimesList = new ArrayList<Integer>();
	String content;
	int workTimes;
	int workers = 0;
	SocketAddress commandControl;

	Broker(Terminal terminal, int port) {
		try {
			this.terminal= terminal;
			socket= new DatagramSocket(port);
			listener.go();
		}
		catch(java.lang.Exception e) {e.printStackTrace();}
	}

	/**
	 * Assume that incoming packets contain a String and print the string.
	 */
	public synchronized void onReceipt(DatagramPacket packet) {
		try {
			String workerName;
			byte[] data;
			byte[] buffer;
			int sourcePort;
			int nameLength;
			DatagramPacket response;
			SocketAddress theSocket;
			data = packet.getData();			
			switch(data[TYPE_POS]) {
			case TYPE_CANDC:
				
				buffer= new byte[data[LENGTH_POS]];
				System.arraycopy(data, HEADER_LENGTH_CANDC, buffer, 0, buffer.length);
				content= new String(buffer);
				
				terminal.println("Getting content from "+ CANDC);
				terminal.println("print |" + content + "|");
				workTimes = (int)data[WORK_TIMES_POS];
				terminal.println("work times: " + workTimes);
				workers = (int)data[WORKERS_NUM_POS];
				terminal.println("workers needed: " + workers + "\n");
				
				if(socketsAddress.size()!=0)
				{
					workers = sendWork(socketsAddress, workTimes, workers, content);
				}
				
				data = new byte[HEADER_LENGTH];
				data[TYPE_POS] = TYPE_ACK;
				data[ACTION_POS] = ACK_ALLOK;
				
				response = new DatagramPacket(data, data.length);
				response.setSocketAddress(packet.getSocketAddress());
				commandControl = packet.getSocketAddress();
				socket.send(response);
				break;
				
			case TYPE_WORKER:
				sourcePort = packet.getPort();
				theSocket = packet.getSocketAddress();
				nameLength = (int)data[NAME_LENGTH_POS];
				buffer = new byte[nameLength];
				System.arraycopy(data, HEADER_LENGTH_WORKER, buffer, 0, nameLength);
				workerName = new String(buffer);
				terminal.println("Message from "+ workerName);
				
				if(data[ACTION_POS]==WORK_DONE)
				{
					terminal.println("Work completed.");
				}
				else 
				{
					terminal.println("Action: " + ((data[ACTION_POS]==REQUEST_YES)? "Join":"Quit"));
				}
				terminal.println("Port: " + sourcePort +"\n");
				
				buffer = data;
				data = new byte[HEADER_LENGTH];
				data[TYPE_POS] = TYPE_ACK;
				data[ACTION_POS] = (buffer[ACTION_POS]==REQUEST_YES)? ADD_WORKER:REMOVE_WORKER;
				
				if(buffer[ACTION_POS]==REQUEST_YES)
				{
					socketsAddress.add(theSocket);
				}
				else if(buffer[ACTION_POS]==REQUEST_NO && socketsAddress.contains(theSocket))
				{
					socketsAddress.remove(socketsAddress.indexOf(theSocket));
				}
				else if(buffer[ACTION_POS]==WORK_DONE)
				{
					sendAckCANDC(commandControl, workerName);
				}
				
				if(workers!=0 && socketsAddress.size()!=0)
				{
					workers = sendWork(socketsAddress, workTimes, workers, content);
				}
				
				response = new DatagramPacket(data, data.length);
				response.setSocketAddress(packet.getSocketAddress());
				socket.send(response);
				//this.notify();
				break;
			default:
				terminal.println("Unexpected packet" + packet.toString());
			}

		}
		catch(Exception e) {e.printStackTrace();}
	}

	public synchronized void run() {
		try {
			terminal.println("Waiting for contact");
			this.wait();
			//this.sendMessage();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public synchronized int sendWork(ArrayList<SocketAddress> theAddress, int workTimes, int workers, String content) {
		
		int times;
		byte[] data;
		byte[] buffer = content.getBytes();
		
		while(workers!=0 && socketsAddress.size()!=0)
		{
			data = new byte[TO_WORKERS_HEADER_LENGTH + buffer.length];
			data[TYPE_POS] = TYPE_WORK;
			times = workTimes;
			data[WORK_TIMES_WORKERS] = (byte)times;
			data[CONTENT_LENGTH] = (byte)buffer.length;
			System.arraycopy(buffer, 0, data, TO_WORKERS_HEADER_LENGTH, buffer.length);
			DatagramPacket response = new DatagramPacket(data, data.length);
			
			SocketAddress temp = socketsAddress.get(0);
			socketsAddress.remove(temp);
			response.setSocketAddress(temp);
			try {
				socket.send(response);
				this.notify();
			} catch (IOException e) {
				e.printStackTrace();
			}
			workers--;
		}
		
		return workers;
	}

	public synchronized void sendAckCANDC(SocketAddress address, String name) {
		try {
			byte[] data;
			byte[] buffer = name.getBytes();
			data = new byte[HEADER_LENGTH + buffer.length];
			data[TYPE_POS] = TYPE_RESULT;
			data[NAME_LENGTH_POS] = (byte)buffer.length;;
			System.arraycopy(buffer, 0, data, HEADER_LENGTH, buffer.length);
			DatagramPacket ack = new DatagramPacket(data, data.length);
			ack.setSocketAddress(address);
			socket.send(ack);
			this.wait();
		}
		catch(Exception e) {e.printStackTrace();}
	}
}