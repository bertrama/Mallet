import java.util.Map;
import java.util.HashMap;
import java.security.ProtectionDomain;
import java.net.URL;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;


public class Main
{
   private static String host           = "127.0.0.1";
   private static Integer httpPort      = 8080;
   private static String models         = "models";
   private static String defaultModel   = "";

   private static void parseArgs(String[] args) {
      // Pull in the system properties.
      if (System.getProperty("servlet.host") != null) {
         host = System.getProperty("servlet.host");
      }
      if (System.getProperty("servlet.port") != null) {
         httpPort = Integer.parseInt(System.getProperty("servlet.port"));
      }

      // Override with command line arguments.
      for (int i = 0 ; i < args.length ; ++i) {
         if (args[i].startsWith("--httpPort=")) {
            httpPort = Integer.parseInt(args[i].substring(11, args[i].length()));
         }
         else if (args[i].startsWith("--host=")) {
            host = args[i].substring(7, args[i].length());
         }
         else if (args[i].startsWith("--models=")) {
            models = args[i].substring(9, args[i].length());
         }
         else if (args[i].startsWith("--defaultModel=")) {
            defaultModel = args[i].substring(15, args[i].length());
         }
         else {
            System.err.println("Run with java -jar mallet.war [options]");
            System.err.println("Options:");
            System.err.println("  --host=IP");
            System.err.println("  --httpPort=PORT");
            System.err.println("  --models=/path/to/models");
            System.err.println("  --defaultModel=modelName");
            System.err.println("");
            System.err.println("  Unrecognized argument: " + args[i]);
            System.exit(255);
         }
      }

      // Write back to system properties.
      System.setProperty("servlet.host", host);
      System.setProperty("servlet.port", httpPort.toString());
      System.setProperty("classifier.models.dir", models);
      System.setProperty("classifier.models.default", defaultModel);
   }

   public static void main(String[] args) throws Exception
   {
      parseArgs(args);
      Server server = new Server();

      if (httpPort > 0) {
         SelectChannelConnector httpConnector = new SelectChannelConnector();
         httpConnector.setPort(httpPort);
         httpConnector.setHost(host);
         server.addConnector(httpConnector);
      }

      ProtectionDomain domain = Main.class.getProtectionDomain();
      URL location = domain.getCodeSource().getLocation();
      WebAppContext webapp = new WebAppContext();
      webapp.setContextPath("/");
      webapp.setWar(location.toExternalForm());
      server.setHandler(webapp);
      server.start();
      server.join();
   }
}
