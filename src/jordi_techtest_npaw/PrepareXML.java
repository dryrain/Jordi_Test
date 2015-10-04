
package jordi_techtest_npaw;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Element;
/**
 *
 * @author Jordi
 */
//The idea behind this class is to be a general Helper through all XML/JSON processing
//Although i didn't have time to do all the methodes more generalistic the best thing
//would be making this class reusable just as a resource for other programs.
public class PrepareXML {
    
    private static String filepathResponseXML = "ResponseXML.xml";
    private static String filepathServiceConfigXML = "src/jordi_techtest_npaw/serviceConfig.xml";
    private static String filepathServiceConfigJSON = "src/jordi_techtest_npaw/serviceConfigJSON.json";
    
    
    public static boolean checkInServiceConfig(String stringToMatch ,String tagToCheck){
        try{
            String filepath = "src/jordi_techtest_npaw/serviceConfig.xml";
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filepath);
            
            NodeList nodelist = doc.getElementsByTagName(tagToCheck);
            
            for (int i = 0 ; i<nodelist.getLength(); i++){
                String stringToCompare = nodelist.item(i).getAttributes().item(0).getTextContent();
                if (stringToMatch.equals(stringToCompare)){
                    System.out.println( "Matched  " + stringToMatch );
                    return true;
                }
            }
            return false;
        
        }catch(ParserConfigurationException | SAXException | IOException  e){
            System.out.println( " Error: " + e.toString() );
            return false;
        }         
    }
    
    
    public static String checkPluginAndGetPing(String accountCode ,String targetDevice, String pluginVersion){
        try {
            String filepath = "src/jordi_techtest_npaw/serviceConfig.xml";
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filepath);
            
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xPath.compile("/config/accountCode[@name='"+accountCode+"']/device[@name='"+targetDevice+"']/pluginVersion");
            //XPathExpression expr = xPath.compile("/config/accountCode[1]/@name");
            String nodeValue = (String)expr.evaluate(doc, XPathConstants.STRING);
            if (nodeValue.equals(pluginVersion)){
               expr = xPath.compile("/config/accountCode[@name='"+accountCode+"']/device[@name='"+targetDevice+"']/pingTime");
               String nodeValue2 = (String)expr.evaluate(doc, XPathConstants.STRING);
               return nodeValue2;
            }else{
                 return "Plugin version not supported";
            }        
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;       
    }
     
    
    public static String[][] clusterSelectorFromXML(String accountCode, String targetDevice, int nClusters){
        try {
            String filepath = "src/jordi_techtest_npaw/serviceConfig.xml";
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filepath);
             
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xPath.compile("/config/accountCode[@name='"+accountCode+"']/device[@name='"+targetDevice+"']/hosts/host");
            NodeList nodeValue = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            nClusters = nodeValue.getLength();
            String[][] sResponse = new String[nClusters][2];
            
            for (int i=1 ;i <= nClusters; i++){
                expr = xPath.compile("/config/accountCode[@name='"+accountCode+"']/device[@name='"+targetDevice+"']/hosts/host["+i+"]");
                nodeValue = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                System.out.println("Node Value :" + nodeValue.item(0).getTextContent());
                Element e = (Element)nodeValue.item(0);
                System.out.println("Node Value :" + e.getAttribute("name"));
                sResponse[i-1][0] = e.getAttribute("name");
                sResponse[i-1][1] = nodeValue.item(0).getTextContent();
            }
            return sResponse;
                  
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static String getXMLResponseAsString(String host, String pingTime, String viewCode){
        try { 
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            
            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("q");
            doc.appendChild(rootElement);
            
            // staff elements
            Element hostElement = doc.createElement("h");
            hostElement.appendChild(doc.createTextNode(host));
            rootElement.appendChild(hostElement);
            
            // firstname elements
            Element pingTimeElement = doc.createElement("pt");
            pingTimeElement.appendChild(doc.createTextNode(pingTime));
            rootElement.appendChild(pingTimeElement);
            
            // lastname elements
            Element viewCodeElement = doc.createElement("c");
            viewCodeElement.appendChild(doc.createTextNode(viewCode));
            rootElement.appendChild(viewCodeElement);
                              
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
            
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            sb.append(output);
            return sb.toString();
                
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
            return (ex.toString());
        } catch (TransformerException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
            return (ex.toString());
        }          
    }   
    
    //NOT USED
    public static void setXML(String hostName, String pingTime, String viewCode) {
        try {
            String filepath = "ResponseXML.xml";
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(filepath);
            
            // Get the element by tag name directly
            Node h = doc.getElementsByTagName("h").item(0);
            Node pt = doc.getElementsByTagName("pt").item(0);
            Node c = doc.getElementsByTagName("c").item(0);
            
            // Update needed Labels
            h.setTextContent(hostName);
            pt.setTextContent(pingTime);
            c.setTextContent(viewCode);
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filepath));
            transformer.transform(source, result);

            System.out.println("Done");
            
        }       
        catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {      
        }
    }
    
    
    //NOT USED
    public static void checkInServiceConfigJSON (){
        JSONParser parser = new JSONParser();
        Object obj;
        try {
            obj = parser.parse(new FileReader("src/jordi_techtest_npaw/serviceConfigJSON.json"));       
            JSONObject config = (JSONObject) obj;
            JSONArray accountCodesList = (JSONArray)config.get("accountCode");
            Iterator <String> iterator = accountCodesList.iterator();
        
        while (iterator.hasNext()){
            String prova = iterator.next().toString();
            System.out.println(iterator.next());
        }
    
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(PrepareXML.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
    
    
}




