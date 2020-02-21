import java.util.Random;

public class FlowTableEntry {
	
	private String entry;
	private int packetReceivedPort;
	private int portToSendTo;
	private int linkLatency;
	private Random latencyGenerator;
	
	FlowTableEntry(String entry, int packetReceivedPort, int portToSendTo){
		this.entry = entry;
		this.packetReceivedPort = packetReceivedPort;
		this.portToSendTo = portToSendTo;
		if(entry.contains("R"))
		{
			latencyGenerator = new Random();
			linkLatency = (latencyGenerator.nextInt(10) + 1) + 1;
		}
	}
	
	public int getLinkLatency() {
		return this.linkLatency;
	}
	
	public String getEntry() {
		return this.entry;
	}
	
	public int getPacketReceivedPort() {
		return this.packetReceivedPort;
	}
	
	public int getPortToSendTo() {
		return this.portToSendTo;
	}
	
}
