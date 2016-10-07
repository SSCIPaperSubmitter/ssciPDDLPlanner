package asepaper.pddlgenerator;

import asepaper.pddlgenerator.PDDLGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class PlannerCreator {

    private static PDDLGenerator generator;

    public static void main(String []argv) {
        if (argv.length != 2 && argv.length != 1) {
            System.err.println("Expected input: \n[.ttl domain file] [.ttl individual file] or\n[port number]");
            System.exit(-1);
        }
        else {
            if (argv.length == 2) {
                generator = new PDDLGenerator();
                generator.GeneratePlan(argv[0], argv[1], true);
            }
            else {
                // start web server
                Integer port = new Integer(argv[0]);
                Server server = new Server(port);
                PDDLWebService standardHandler = new PDDLWebService();
                server.setHandler(standardHandler);

                try{
                    System.out.println("Serving at port "+port.toString()+" ..");
                    server.start();
                    server.join();
                }catch (Exception ex){
                    System.err.println("Error " + ex.toString());
                    System.exit(-1);
                }
            }

        }
    }

}