package eu.supersede.jira.plugins.servlet;

import java.util.Date;
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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLFileGenerator {

	private String id;
	private Date date;

	public XMLFileGenerator() {
		super();
	}

	public XMLFileGenerator(String id, Date date) {
		this.id = id;
		this.date = date;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public File generateXMLFile() {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// == ROOT DOCUMENTS SECTION
			// issue root element
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("jira");
			doc.appendChild(rootElement);

			// issue element
			Element issue = doc.createElement("issue");
			rootElement.appendChild(issue);

			// === ATTRIBUTES SECTION ===
			// set attribute to issue element (<issue id="..">)
			// shorter way
			// issue.setAttribute("id", id);
			Attr attr = doc.createAttribute("id");
			attr.setValue(id);
			issue.setAttributeNode(attr);

			// timestamp elements
			Element timestamp = doc.createElement("timestamp");
			timestamp.appendChild(doc.createTextNode(date.toString()));
			issue.appendChild(timestamp);

			// type elements (HARDCODED)
			Element type = doc.createElement("type");
			type.appendChild(doc.createTextNode("JIRA ISSUE"));
			issue.appendChild(type);

			// priority elements (HARDCODED)
			Element priority = doc.createElement("priority");
			priority.appendChild(doc.createTextNode("100"));
			issue.appendChild(priority);

			// === OUTPUT SECTION ===
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			File file = File.createTempFile("file", ".xml");
			StreamResult result = new StreamResult(file);

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

			System.out.println("File saved!");
			return file;
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (TransformerException tfe) {
			tfe.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
