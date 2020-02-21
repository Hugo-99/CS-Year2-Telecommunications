import tcdIO.Terminal;

import java.util.ArrayList;

public class Main implements GlobalConstants {

    public static void main(String[] args) {


        GlobalVariables.height = DEFAULT_ROUTER_COUNT_X;
        GlobalVariables.width = DEFAULT_ROUTER_COUNT_Y;
        int routerCurrentPortNum = STARTING_ROUTERS_PORT;
        int clientCount = 0;
        int[] routerSrcPorts = new int[ROUTER_PORTS];
        int[] routerDstPorts = new int[ROUTER_PORTS];
        ArrayList<FlowTableEntry> routingTableEntries = new ArrayList<FlowTableEntry>();
        Terminal terminal;
        terminal = new Terminal("Setup");
        terminal.println("Currently is just working for a 2x2 router map,");
        terminal.println("the data transmission is just working for c1-c2 and c2-c3.");
        terminal.println("_______________________________________________\n");
        terminal.println("Enter in the width of the map: ");
        GlobalVariables.width = terminal.readInt();
        terminal.println("Enter in the height of the map: ");
        GlobalVariables.height = terminal.readInt();

        Router routers[][] = new Router[GlobalVariables.height][GlobalVariables.width];
        Controller controller = new Controller("CNT", "localhost", CONTROLLER_PORT);


        int routerCount = 0;
        for (int i = 0; i < GlobalVariables.height; i++) 
        {
            for (int j = 0; j < GlobalVariables.width; j++) 
            {
            	routerSrcPorts = setUpRouterSrcPorts(routerCurrentPortNum);
                routerDstPorts = setUpRouterDstPort(routerSrcPorts);
                if (j == 0) {
                    routerDstPorts[0] = 0;

                    //put a client on top left corner of grid
                    if (i == 0) {
                        Client clientOne = new Client("c1", "localhost", "localhost", STARTING_CLIENTS_PORT + 50, routerSrcPorts[0], STARTING_CLIENTS_PORT);
                        routerDstPorts[0] = STARTING_CLIENTS_PORT;
                        clientCount++;
                    }
                    if (i == GlobalVariables.height - 1 && GlobalVariables.height > 1) {
                    	Client clientTwo = new Client("c2", "localhost", "localhost", STARTING_CLIENTS_PORT + 51, routerSrcPorts[0], STARTING_CLIENTS_PORT + 1);
                        routerDstPorts[0] = STARTING_CLIENTS_PORT + 1;
                        clientCount++;
                    }
                }
                if (j == GlobalVariables.width - 1) {
                    routerDstPorts[2] = 0;

                    //put a client on bottom right
                    if (i == GlobalVariables.height - 1 && GlobalVariables.height > 1) {
                    	Client clientThree = new Client("c3", "localhost", "localhost", STARTING_CLIENTS_PORT + 52, routerSrcPorts[2], STARTING_CLIENTS_PORT + 2);
                        routerDstPorts[2] = STARTING_CLIENTS_PORT  + 2;
                        clientCount++;
                    }
                }
                if (i == 0) {
                    routerDstPorts[1] = 0;
                }
                if (i == GlobalVariables.height - 1) {
                    routerDstPorts[3] = 0;
                }
                String name = "R" + routerCount;
                routers[i][j] = new Router(name, "localhost",
                        routerSrcPorts, routerDstPorts, createRoutingTable(routingTableEntries, routerSrcPorts,
                        routerDstPorts, routerCount, clientCount));
            
                routerCurrentPortNum+=5;
                routerCount++;

            }
        }
    }

	public static int[] setUpRouterDstPort(int[] routerSrcPorts) {
		int[] routerDstPorts = new int[5];
        routerDstPorts[0] = routerSrcPorts[0] - 3;                                                     
        routerDstPorts[1] = ((routerSrcPorts[1] - (ROUTER_PORTS * GlobalVariables.width)) -2) + 4;     
        routerDstPorts[2] = routerSrcPorts[2] + 3;                                                     
        routerDstPorts[3] = ((routerSrcPorts[3] + (ROUTER_PORTS * GlobalVariables.width)) - 2);          
        routerDstPorts[4] = CONTROLLER_PORT;
		return routerDstPorts;
	}
	
	public static int[] setUpRouterSrcPorts(int startingPort) {
		int[] routerSrcPort = new int[5];
		routerSrcPort[0] = startingPort + 1;
		routerSrcPort[1] = startingPort + 2;
		routerSrcPort[2] = startingPort + 3;
		routerSrcPort[3] = startingPort + 4;
		routerSrcPort[4] = startingPort + 5;
		return routerSrcPort;
	}

    public static ArrayList<FlowTableEntry> createRoutingTable(ArrayList<FlowTableEntry> routingTableEntries,
                                                                  int[] routerSrcPorts, int[] routerDstPorts, int routerCount,
                                                                  int clientCount) {
        routingTableEntries = new ArrayList<FlowTableEntry>();
        for (int i = 0; i < ROUTER_PORTS; i++) {

            if (routerDstPorts[i] != 0 && routerDstPorts[i] >= STARTING_ROUTERS_PORT) {
                int routerNum = 0;
                if (i == 0) {
                    routerNum = routerCount - 1;
                }
                else if (i == 1) {
                    routerNum = routerCount - GlobalVariables.width;
                }
                else if (i == 2) {
                    routerNum = routerCount + 1;
                }
                else if (i == 3) {
                    routerNum = routerCount + GlobalVariables.width;
                }
                routingTableEntries.add(new FlowTableEntry(("R" + routerNum), routerSrcPorts[i], routerDstPorts[i]));
            }
            else if (routerDstPorts[i] >= STARTING_CLIENTS_PORT && routerDstPorts[i] < STARTING_CLIENTS_PORT + 5) {
                routingTableEntries.add(new FlowTableEntry(("c" + clientCount), routerSrcPorts[i], routerDstPorts[i]));
            }
            else if (routerDstPorts[i] == CONTROLLER_PORT) {
                routingTableEntries.add(new FlowTableEntry(("CNT"), routerSrcPorts[i], routerDstPorts[i]));
            }
            else if (routerDstPorts[i] == 0) {
                routingTableEntries.add(new FlowTableEntry(("0"), routerSrcPorts[i], routerDstPorts[i]));
            }
        }
        return routingTableEntries;
    }
}