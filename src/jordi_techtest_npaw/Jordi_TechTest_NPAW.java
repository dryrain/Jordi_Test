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
//Main class. Implements the thread that processes the GET request and provides
//the answer to each client.
//For better optimization, all System.out.println should be removed i favour of a
//better logging solution
public class Jordi_TechTest_NPAW implements Runnable {
    private final Socket m_socket;
    private final int m_num;
    private OutputStreamWriter out;
    
    //private String senderIP;
    //private String senderPort;
    
    private String account_code;
    private String targetDevice;
    private String pluginVersion;
    private String pingTime;
    private static long viewCode=0;
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
                //Getting the GET request
                System.out.println( m_num + " Connected." );
                BufferedReader in = new BufferedReader( new InputStreamReader( m_socket.getInputStream() ) );
                out = new OutputStreamWriter( m_socket.getOutputStream() );               
                String line = in.readLine();
                
                //Would be ideal to check whether the request is an HTTP GET request and the senders IP/port
                //Right now we assume every request is correctly formatted
                //Could try something with the URL library but currently is not working      
//                    URL urlToParse = new URL(line.split(" ")[0]);
//                    urlToParse.getProtocol();
//                    urlToParse.getHost();
     
                //Getting the parameters from the request
                try {
                    Map<String, List<String>> recievedData = getQueryParams(line);
                    account_code = recievedData.get("accountCode").get(0);
                    targetDevice = recievedData.get("targetDevice").get(0);
                    pluginVersion = recievedData.get("pluginVersion").get(0).substring(0, 5); //Caution! Substring here removes trash but should be done safer.
                }catch (NullPointerException ex){
                    System.out.println( "Error: The server couldn't identify the request \" "+line+" \" " );
                    return;
                }
                              
                //****Checking Account/Device/Plugins at the serviceConfig****
                //This could be done all at the same method to gain efficiency.
                //We decided to keep it in separate methods for better understanding and to match the 
                //described functionality of the project.
                //Every thread checks serviceConfig.xml to find the right configuration for the client. 
                //To improve this we could catch every X time the configuration into program variables
                //and thanks to that we woulnd't need to query the XML document every time
                if (checkAccCode()){
                    if(checkTargetDevice()){
                        pingTime = checkPluginVersion();
                        if(pingTime.equals("-1"))pluginVersion="Not Supported!";
                        String[][] hosts = XMLHelper.clusterSelectorFromXML(account_code,targetDevice);
                        host = selectHost(hosts);
                        generateViewCode();
                        serviceAnswer(); //good response
                        
                    }else{
                        serviceBlankAnswer(); //null response
                        System.out.println( "Error: Non matching Target Device!" );
                    }
                }else{
                    serviceBlankAnswer(); //null response
                    System.out.println( "Error: Non matching Account Code!" );
                }          
            }
            finally //We always end up here
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
        //Here we create the XML and then send it. It is done without storing/editing the
        //XML file because we could have concurrency problems with other Threads
        //As it DIDN'T specify to send an HTTP answer, the XML file is written directly to the outputStream
        try {
            String toSend = XMLHelper.getXMLResponseAsString(host,pluginVersion,Long.toString(viewCode));
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
        //Just sending back a blank space
        try {
            out.write(" ");
            out.flush();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Jordi_TechTest_NPAW.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    
    private boolean checkAccCode() {
        return XMLHelper.checkInServiceConfig(account_code, "accountCode");
    }

    private boolean checkTargetDevice() {
        return XMLHelper.checkTargetDevice(account_code, targetDevice);
    }

    private String checkPluginVersion() {
        return XMLHelper.checkPluginAndGetPing(account_code, targetDevice, pluginVersion);
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
        return "Error in Host Selection!";
    }

    //We use synchronized to make sure only one thread at a time can generate its unique code. 
    //As a result the efficiency of the program is lowered.
    private synchronized void generateViewCode() {
        //The idea here is to use a simple incremental counter. It provides unique numbers
        //without the need to log and check the other viewCodes provided
        //The only issue here is if the service is RESTARTED. One way to protect 
        //us from this would be storing the viewcode in a configFile or database     
        viewCode++;  
        System.out.println( "ViewCode value is: " + viewCode );      
    }
      
    
    public static Map<String, List<String>> getQueryParams(String url) {
        //A methode that gets all the parameters from the HTTP GET query
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
