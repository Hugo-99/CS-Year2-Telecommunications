import java.io.IOError;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import tcdIO.Terminal;

public class Client implements GlobalConstants{

	private String name;
	private String clientAddress;
	private String dstAddress;
	private int clientSrcPort;
	private int clientDstPort;
	private int clientRecvPort;
	private InetSocketAddress dstSocketAddress;
	protected Terminal terminal;
	
	Client(String name, String clientAddress, String dstAddress,
			int clientSrcPort, int clientDstPort, int clientRecvPort){
		
		this.name = name;
		this.clientAddress = clientAddress;
		this.dstAddress = dstAddress;
		this.clientSrcPort = clientSrcPort;
		this.clientDstPort = clientDstPort;
		this.clientRecvPort = clientRecvPort;
		
		terminal = new Terminal(name);
		Sender sender = new Sender();
		Receiver receiver = new Receiver();
	}
	
	private class Sender {
		private DatagramPacket packet;
		private DatagramSocket socket;
		private SenderListener senderListener;
		
		Sender(){
			try {
				dstSocketAddress = new InetSocketAddress(dstAddress, clientDstPort);
				socket = new DatagramSocket(clientSrcPort);
				senderListener = new SenderListener();
				senderListener.startListener();
			}
			catch(java.lang.Exception e) {e.printStackTrace();}
		}
		
		public synchronized void toSend() {
            packet = null;
            terminal.println("\nPlease enter what you are going to send: ");
            String s = terminal.readString() + "-";
            terminal.println("\nEnter in destination in the following format:(Ex. c1/c3)\n");
            String dest = "";


            dest = terminal.readString();

            byte[] payload = s.getBytes();
            byte[] header = (name + "#" + dest + "#-").getBytes();
            byte[] buffer = new byte[header.length + payload.length];
            System.arraycopy(header, 0, buffer, 0, header.length);
            System.arraycopy(payload, 0, buffer, header.length, payload.length);
            packet = new DatagramPacket(buffer, buffer.length, dstSocketAddress);
            try {
                socket.send(packet);
                terminal.println(name + ": Send a Packet to " + packet.getSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }

		}
		
		private class SenderListener extends Thread implements Runnable{
	        
	        SenderListener() {
	        }
			public void startListener() {
                try {
                    new Thread(this).start();
                } catch (IOError exception) {
                    exception.printStackTrace();
                }
			}
			public void run() {
                while (true) {
                    toSend();
                }
			}
		}
	}
	private class Receiver {
		
		private DatagramPacket recvPacket;
		private DatagramSocket recvSocket;
		private ReceiverListener receiverListener;
		
		Receiver(){
			try {
				recvSocket = new DatagramSocket(clientRecvPort);
				receiverListener = new ReceiverListener();
				receiverListener.startListener();
			}
			catch(java.lang.Exception e) {e.printStackTrace();}
		}
		
		public synchronized void toReceive() {
			recvPacket = new DatagramPacket(new byte[PACKET_SIZE],PACKET_SIZE);
			try {
				recvSocket.receive(recvPacket);
				terminal.println("Received packet from " + recvPacket.getAddress());
				terminal.println("\nMessage Received: " + getPacketPayload());
	            terminal.print("\nPlease enter what you are going to send: ");
			}catch(IOException e) {
				
			}catch (NullPointerException e1) {
				//new added
            }
		}
		
		public String getPacketPayload() {
			String input = new String (recvPacket.getData());
			boolean stop = false;
			int count1 = 0;
			while(input.charAt(count1)!='-')
			{
				count1++;
			}
			int count2 = count1+1;
			while(input.charAt(count2)!='-')
			{
				count2++;
			}
			String payload = input.substring(count1+1, count2);
			
			return payload;
		}
		
		private class ReceiverListener extends Thread implements Runnable{
			
			public void startListener() {
                try {
                    new Thread(this).start();
                } catch (IOError exception) {
                    exception.printStackTrace();
                }
			}
			
			public void run() {
                while (true) {
                	toReceive();
                }
			}
			
		}
	}
}
