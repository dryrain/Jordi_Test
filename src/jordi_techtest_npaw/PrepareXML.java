/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jordi_techtest_npaw;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
/**
 *
 * @author Jordi
 */
public class PrepareXML {
    
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
    
}
