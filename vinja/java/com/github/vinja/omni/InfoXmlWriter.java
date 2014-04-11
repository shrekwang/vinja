package com.github.vinja.omni;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class InfoXmlWriter {

	static TransformerFactory transformerFactory = TransformerFactory.newInstance();
	static DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

	public static String writeMemberInfo(List<MemberInfo> memberInfos) {

		try {
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			// root elements
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("result");
			doc.appendChild(rootElement);

			for (MemberInfo memberInfo : memberInfos) {

				Element memberEle = doc.createElement("member");
				rootElement.appendChild(memberEle);
				memberEle.setAttribute("type", memberInfo.getMemberType());
				memberEle.setAttribute("name", memberInfo.getName());
				//memberEle.setTextContent(memberInfo.getFullDeclaration());
			}

			StringWriter sw = new StringWriter();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(sw);
			transformer.transform(source, result);
			sw.close();
			return sw.toString();

		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";

	}
}