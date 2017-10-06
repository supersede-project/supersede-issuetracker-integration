/*
   (C) Copyright 2015-2018 The SUPERSEDE Project Consortium

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

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
	private String[] issues;
	private String applicationId;
	private String tenant;
	private String creationDate;
	private String description;
	private Date date; // for testing purposes, if date changes during jira use,
						// new data was loaded
	private int sentiment;
	private int positive;
	private int negative;

	public XMLFileGenerator() {
		super();
	}

	public XMLFileGenerator(String id, Date date) {
		this.id = id;
		this.date = date;
	}

	public XMLFileGenerator(Alert a) {
		this.id = a.getId();
		this.issues = a.getIssues();
		this.tenant = a.getTenant();
		this.description = a.getDescription();
		this.applicationId = a.getApplicationId();
		this.creationDate = a.getTimestamp();
		this.date = new Date();
		this.sentiment = a.getSentiment();
		this.positive = a.getPositive();
		this.negative = a.getNegative();
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

	public String[] getIssues() {
		return issues;
	}

	public void setIssues(String[] issues) {
		this.issues = issues;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getSentiment() {
		return sentiment;
	}

	public void setSentiment(int sentiment) {
		this.sentiment = sentiment;
	}

	public int getPositive() {
		return positive;
	}

	public void setPositive(int positive) {
		this.positive = positive;
	}

	public int getNegative() {
		return negative;
	}

	public void setNegative(int negative) {
		this.negative = negative;
	}

	public File generateXMLFile() {
		try {
			Document doc = buildXMLData();
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

	/**
	 * Builds the XML file structure
	 * 
	 * Example:
	 * 
	 * <alert id="foo"> <creationDate></creationDate>
	 * <description></description> <applicationId></applicationId>
	 * <timestamp><timestamp> </alert>
	 * 
	 * @return
	 * @throws ParserConfigurationException
	 */
	public Document buildXMLData() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// == ROOT DOCUMENTS SECTION
		// issue root element
		Document doc = docBuilder.newDocument();
		Element rootAlert = doc.createElement("alert");
		doc.appendChild(rootAlert);

		// === ATTRIBUTES SECTION ===
		// set attribute to alert element (<alert id="..">)
		// shorter way
		// alert.setAttribute("id", id);
		Attr attr = doc.createAttribute("id");
		attr.setValue(getId());
		rootAlert.setAttributeNode(attr);

		// description elements
		Element description = doc.createElement("description");
		description.appendChild(doc.createTextNode(getDescription()));
		rootAlert.appendChild(description);

		// application Id elements
		Element priority = doc.createElement("applicationId");
		priority.appendChild(doc.createTextNode(getApplicationId()));
		rootAlert.appendChild(priority);

		// timestamp elements
		Element timestamp = doc.createElement("timestamp");
		timestamp.appendChild(doc.createTextNode(getDate().toString()));
		rootAlert.appendChild(timestamp);

		// sentiment elements
		Element sentiment = doc.createElement("sentiment");
		timestamp.appendChild(doc.createTextNode(String.valueOf(getSentiment())));
		rootAlert.appendChild(sentiment);

		// positive elements
		Element positive = doc.createElement("positive");
		timestamp.appendChild(doc.createTextNode(String.valueOf(getPositive())));
		rootAlert.appendChild(positive);

		// negative elements
		Element negative = doc.createElement("negative");
		timestamp.appendChild(doc.createTextNode(String.valueOf(getNegative())));
		rootAlert.appendChild(negative);

		return doc;

	}

}
