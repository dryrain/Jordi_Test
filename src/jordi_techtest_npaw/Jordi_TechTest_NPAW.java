package jordi_techtest_npaw;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Jordi Calduch Casas
 */
public class Jordi_TechTest_NPAW implements Runnable {

    private final Socket m_socket;
    private final int m_num;
    private OutputStreamWriter out;
    private int n_clusters = 2;
    
    private String senderIP;
    private String senderPort;
    
    private String account_code;
    private String targetDevice;
    private String pluginVersion;
    private String pingTime;
    private static int viewCode=0;
    //private static String sViewCode=null;
    private String host;
                

    Jordi_TechTest_NPAW( Socket socket, int num )
    {
        m_socket = socket;
        m_num = num;
        
        Thread handler = new Thread( this, "handler-" + m_num );
        handler.start();
    }
    
    @Override
    public void run()
    {
        try
        {
            try
            {
                System.out.println( m_num + " Connected." );
                BufferedReader in = new BufferedReader( new InputStreamReader( m_socket.getInputStream() ) );
                out = new OutputStreamWriter( m_socket.getOutputStream() );
                
                String line = in.readLine();
                
                //Would be ideal to check whether the request is an HTTP GET request and the senders IP
                //Right now we assume every request is correctly formatted
//                try{
//                    URL urlToParse = new URL(in.readLine().split(" ")[0]);
//                    urlToParse.getProtocol();
//                    urlToParse.getHost();
//                }catch(Exception ex){
//                    
//                }
                
                //Get the service configuration
                getServiceConfig(); // this could be done every X seconds or minutes to increase the service's speed
                
                //Get the parameters from the request
                try {
                    Map<String, List<String>> recievedData = getQueryParams(line);
                    account_code = recievedData.get("accountCode").get(0);
                    targetDevice = recievedData.get("targetDevice").get(0);
                    pluginVersion = recievedData.get("pluginVersion").get(0).substring(0, 5); //Caution! Substring here removes trash but should be done safer.
                }catch (NullPointerException ex){
                    Logger.getLogger(Jordi_TechTest_NPAW.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println( "Error: The server couldn't identify the request" );
                }
                              
                //Check Account/Device/Plugins at the serviceConfig
                //This could be done all at the same methode to gain efficiency.
                //We decided to keep it in separate methodes for better understanding and to match the 
                //described functionality of the project.
                if (checkAccCode()){
                    if(checkTargetDevice()){
                        pingTime = checkPluginVersion();
                        String[][] hosts = PrepareXML.clusterSelectorFromXML(account_code,targetDevice,n_clusters);
                        host = selectHost(hosts);
                        generateViewCode();
                        //PrepareXML.checkInServiceConfigJSON();
                        //PrepareXML.setXML(account_code, pingTime, pingTime);
                        serviceAnswer();
                        //StringBuilder to prepare/send XML?
                        
                    }else{
                        serviceBlankAnswer(); //null response
                    }
                }else{
                    serviceBlankAnswer(); //null response 
                }          
            }
            finally
            {
                m_socket.close();
            }
        }
        catch ( IOException e )
        {
            System.out.println( m_num + " Error: " + e.toString() );
        }
    }
    
    public boolean serviceAnswer(){
        try {
            //Here we create the XML and then send it. It is done without storing/editing the
            //XML file because we could have concurrency problems with other Threads
            String toSend = PrepareXML.getXMLResponseAsString(host,pluginVersion,Integer.toString(viewCode));
            System.out.println(toSend);
            out.write(toSend);
            out.flush();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Jordi_TechTest_NPAW.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    public boolean serviceBlankAnswer(){
        try {
            out.write(" ");
            out.flush();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Jordi_TechTest_NPAW.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    private void getServiceConfig() {
       // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    } 
    
    private boolean checkAccCode() {
        return PrepareXML.checkInServiceConfig(account_code, "accountCode");
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean checkTargetDevice() {
        return PrepareXML.checkInServiceConfig(targetDevice, "device");
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String checkPluginVersion() {
        return PrepareXML.checkPluginAndGetPing(account_code, targetDevice, pluginVersion);
    }
    
    
    private String selectHost(String[][] hostsList){
        //The idea here is to increase efficiency using a random number from 1-100 every time
        //we need to select a Host and then comparing it to the criteria set in serverConfig.
        //At the long term the balancing will be real without needing to store any data.
        //CAUTION: the random number must have an uniform distribution
        int hostCount = hostsList.length;
       
        Random randomGenerator = new Random();
        int randomInt = randomGenerator.nextInt(100);
        int baseValue = 0;
        for (int i = 0 ; i < hostCount ; i++){
            int percentValue = Integer.parseInt(hostsList[i][1]);
            int topValue = baseValue + percentValue;
            if ((baseValue) <= randomInt && (topValue) > randomInt){
                //Cluster selected!
                return hostsList[i][0];
            }else{
                baseValue = percentValue;
            }
        }
        return "Error in Host Selection";
    }

    //We use synchronized to make sure only one thread at a time can generate its unique code. 
    //As a result the speed of the program is lowered
    private synchronized void generateViewCode() {
        //The idea here is to use a simple incremental counter. It provides unique numbers
        //without the need to log and check the other viewCodes provided
        //The only issue here is if the service is RESTARTED. One way to protect 
        //us from this would be storing the viewcode in a configFile or database     
        this.viewCode++;  
        System.out.println( "ViewCode value is: " + viewCode );      
    }
      
    public static Map<String, List<String>> getQueryParams(String url) {
        try {
            Map<String, List<String>> params = new HashMap<String, List<String>>();
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String query = urlParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = "";
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], "UTF-8");
                    }

                    List<String> values = params.get(key);
                    if (values == null) {
                        values = new ArrayList<String>();
                        params.put(key, values);
                    }
                    values.add(value);
                }
            }
            return params;
        } catch (UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
            }
    }
    
    
    public static void main( String[] args )
        throws Exception
    {
        int port = 80;
        if ( args.length > 0 )
        {
            port = Integer.parseInt( args[0] );
        }
        System.out.println( "Accepting connections on port: " + port );
        int nextNum = 1;
        ServerSocket serverSocket = new ServerSocket( port );
        while ( true )
        {
            Socket socket = serverSocket.accept();
            Jordi_TechTest_NPAW hw = new Jordi_TechTest_NPAW( socket, nextNum++ );
        }
    }
}
