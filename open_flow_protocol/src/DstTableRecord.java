
public class DstTableRecord {
	private String finalDestination;
	private String nextRouter;
	private int currentSocketNum;
	
	DstTableRecord(String finalDestination, String nextRouter, int currentSocketNum){
		this.finalDestination = finalDestination;
		this.nextRouter = nextRouter;
		this.currentSocketNum = currentSocketNum;
	}
	
	public String getDestination() {
		return this.finalDestination;
	}
	
	public String getNextRouter() {
		return this.nextRouter;
	}
	
	public int getCurrentSocketNum() {
		return this.currentSocketNum;
	}
}
