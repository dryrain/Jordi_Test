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


/**
 *
 * @author Jordi Calduch Casas
 */
public class Jordi_TechTest_NPAW implements Runnable {

    private final Socket m_socket;
    private final int m_num;
    
    private String senderIP;
    private String senderPort;
    
    private String account_code;
    private String targetDevice;
    private String pluginVersion;
    private String pingTime;
    private static int viewCode=0;
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
                OutputStreamWriter out = new OutputStreamWriter( m_socket.getOutputStream() );
                out.write( "Welcome connection #" + m_num + "\n\r" );
                out.flush();
                
                //String message = org.apache.commons.io.IOUtils.toString(rd);
                //String queryParams = getStringFromInputStream(in);
                String line = in.readLine();
                
                //Would be nice to check whether the request is an HTTP GET request
                
                //URL urlToParse = new URL(in.readLine().split(" ")(0));
                //urlToParse.getProtocol();
                //urlToParse.getHost();
                
                //Get the service configuration
                getServiceConfig(); // this can be done every X seconds or minutes to increase the service's speed
                
                //Get the parameters from the request
                Map<String, List<String>> recievedData = getQueryParams(line);
                account_code = recievedData.get("accountCode").get(0);
                targetDevice = recievedData.get("targetDevice").get(0);
                pluginVersion = recievedData.get("pluginVersion").get(0).substring(0, 5); //Caution! Substring here removes trash but should be done safer.
                
                
                //RLEncodedUtils.parse(in.toString());
                //Check accountCode
                if (checkAccCode()){
                    if(checkTargetDevice() && checkPluginVersion()){
                        generateViewCode();
                        PrepareXML.setXML(account_code, pingTime, pingTime);
                        
                        //StringBuilder to prepare/send XML?
                        
                    }else{
                        serviceAnswer(""); //null response
                    }
                }else{
                    serviceAnswer(""); //null response 
                }
                
                
                
                while ( true )
                {
                    //String line = in.readLine();
                    if ( line == null )
                    {
                        System.out.println( m_num + " Closed." );
                        return;
                    }
                    else
                    {
                        //System.out.println( m_num + " Read: " + line );
                        if ( line.equals( "exit" ) )
                        {
                            System.out.println( m_num + " Closing Connection." );
                            return;
                        }
                        //else if ( line.equals( "crash" ) )
                        //{
                        //    System.out.println( m_num + " Simulating a crash of the Server..." );
                        //    Runtime.getRuntime().halt(0);
                        //}
                        else
                        {
                            //System.out.println( m_num + " Write: echo " + line );
                            //out.write( "echo " + line + "\n\r" );
                            //out.flush();
                        }
                    }
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
    
    public void serviceAnswer(String response){
        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.      
    }
    
    private void getServiceConfig() {
       // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    
    
    private boolean checkAccCode() {
        return PrepareXML.checkInServiceConfig(account_code, "accountCode");
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean checkTargetDevice() {
        return PrepareXML.checkInServiceConfig(targetDevice, "device");
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean checkPluginVersion() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return true;
    }

    //We use synchronized to make sure only one thread at a time can generate its unique code. 
    //As a result the speed of the program is lowered
    private synchronized void generateViewCode() {
        //The idea here is to use a simple incremental counter. It provides unique numbers
        //without the need to log and check the other viewCodes provided
        //The only issue here is if the service is RESTARTED. One way to protect 
        //us from this would be storing the viewcode in a configFile or database
        viewCode++;
        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
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
