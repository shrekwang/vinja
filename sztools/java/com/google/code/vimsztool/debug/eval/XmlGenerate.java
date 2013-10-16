package com.google.code.vimsztool.debug.eval;

import java.util.ArrayList;
import java.io.StringWriter;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
 
import org.w3c.dom.Document;
import org.w3c.dom.Element;
 
public class XmlGenerate {
 
	public static void main(String argv[]) {
        List<VarNode> nodes = new ArrayList<VarNode>();
        VarNode node = new VarNode("name", "dir", "java.lang.String", "this is value");
        nodes.add(node);
        node = new VarNode("name2", "dir", "java.lang.String", "this is value");
        nodes.add(node);

        String repr = generate(nodes);
        System.out.println(repr);
        

    }

	public static String generate(List<VarNode> nodes) {
 
	  try {
 
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
 
		// root elements
		Document doc = docBuilder.newDocument();
        doc.setXmlStandalone(true);
		Element rootElement = doc.createElement("vars");
		doc.appendChild(rootElement);
 
        for (VarNode varNode : nodes ) {
            // staff elements
            Element nodeElement = doc.createElement("var");
            rootElement.appendChild(nodeElement);
            nodeElement.setAttribute("name", varNode.getName());
            nodeElement.setAttribute("nodetype", varNode.getNodeType());
            nodeElement.setAttribute("javatype", varNode.getJavaType());
            nodeElement.setAttribute("value", varNode.getValue());
            nodeElement.setAttribute("uuid", varNode.getUuid());
            
        }
 
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();

        transformer.transform(source, new StreamResult(writer));
        String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        return output;
 
	  } catch (ParserConfigurationException pce) {
		pce.printStackTrace();
	  } catch (TransformerException tfe) {
		tfe.printStackTrace();
	  }
      return "";
	}
}
