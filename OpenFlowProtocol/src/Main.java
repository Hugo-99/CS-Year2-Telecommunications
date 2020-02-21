
public class Main implements Constants{

	public static void main(String[] args) {
		try {
			
			for (int i = 0; i < 3; i++) 
			{
				new Thread((new Worker(new Terminal("Worker " + (i + 1)), DEFAULT_DST_NODE, DEFAULT_BROKER_PORT, DEFAULT_SRC_PORT - i))).start();
			}
			
			new Thread((new CommandControl(new Terminal("C&C"),DEFAULT_CANDC_PORT,DEFAULT_DST_NODE,DEFAULT_BROKER_PORT))).start();
			Terminal terminal= new Terminal("Broker");
			new Thread((new Broker(terminal, DEFAULT_BROKER_PORT))).run();
			terminal.println("Program completed");
				
		} catch(java.lang.Exception e) {e.printStackTrace();}

	}
}
