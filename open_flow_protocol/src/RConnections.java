import java.util.ArrayList;

public class RConnections {

	private String name;
	private String leftBond;
	private String rightBond;
	private String topBond;
	private String bottomBond;
	private int leftLatency;
	private int rightLatency;
	private int topLatency;
	private int bottomLatency;
	private int leftRNum;
	private int rightRNum;
	private int topRNum;
	private int bottomRNum;
	private String[] bonds = new String[4];
	private int[] latencies = new int[4];
	
	RConnections(String routerName, String leftBond, String rightBond, String topBond, String bottomBond,
				int leftLatency, int rightLatency, int topLatency, int bottomLatency,
				int leftRNum, int rightRNum, int topRNum, int bottomRNum){
		this.name = routerName;
		this.leftBond = leftBond;
		this.rightBond = rightBond;
		this.topBond = topBond;
		this.bottomBond = bottomBond;
		this.leftLatency = leftLatency;
		this.rightLatency = rightLatency;
		this.topLatency = topLatency;
		this.bottomLatency = bottomLatency;
		this.leftRNum = leftRNum;
		this.rightRNum = rightRNum;
		this.topRNum = topRNum;
		this.bottomRNum = bottomRNum;
		
		bonds[0] = leftBond;
		bonds[1] = rightBond;
		bonds[2] = topBond;
		bonds[3] = bottomBond;
		
		latencies[0] = leftLatency;
		latencies[1] = rightLatency;
		latencies[2] = topLatency;
		latencies[3] = bottomLatency;
		
	}
	
	public String getRouterName() {
		return this.name;
	}

	public String[] getBonds(){
		return bonds;
	}
	
	public int[] getLatencies() {
		return latencies;
	}
	
	public void setRouterName(String name) {
		this.name = name;
	}

	public String getLeftBond() {
		return leftBond;
	}

	public void setLeftBond(String leftBond) {
		this.leftBond = leftBond;
	}

	public String getRightBond() {
		return rightBond;
	}

	public void setRightBond(String rightBond) {
		this.rightBond = rightBond;
	}

	public String getTopBond() {
		return topBond;
	}

	public void setTopBond(String topBond) {
		this.topBond = topBond;
	}

	public String getBottomBond() {
		return bottomBond;
	}

	public void setBottomBond(String bottomBond) {
		this.bottomBond = bottomBond;
	}

	public int getLeftLatency() {
		return leftLatency;
	}

	public void setLeftLatency(int leftLatency) {
		this.leftLatency = leftLatency;
	}

	public int getRightLatency() {
		return rightLatency;
	}

	public void setRightLatency(int rightLatency) {
		this.rightLatency = rightLatency;
	}

	public int getTopLatency() {
		return topLatency;
	}

	public void setTopLatency(int topLatency) {
		this.topLatency = topLatency;
	}

	public int getBottomLatency() {
		return bottomLatency;
	}

	public void setBottomLatency(int bottomLatency) {
		this.bottomLatency = bottomLatency;
	}

	public int getLeftRNum() {
		return leftRNum;
	}

	public void setLeftRNum(int leftRNum) {
		this.leftRNum = leftRNum;
	}

	public int getRightRNum() {
		return rightRNum;
	}

	public void setRightRNum(int rightRNum) {
		this.rightRNum = rightRNum;
	}

	public int getTopRNum() {
		return topRNum;
	}

	public void setTopRNum(int topRNum) {
		this.topRNum = topRNum;
	}

	public int getBottomRNum() {
		return bottomRNum;
	}

	public void setBottomRNum(int bottomRNum) {
		this.bottomRNum = bottomRNum;
	}

	@Override
	public String toString() {
		return name +":" + "\nleftBond=" + leftBond + "\nrightBond=" + rightBond + "\ntopBond="
				+ topBond + "\nbottomBond=" + bottomBond;
	}

	
}
