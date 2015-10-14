package org.dperegolise;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

/**
 * Class to generate an HTML test report from an Intellij XML test report.
 * This was created because with large log files, Intellij will often run out of memory before
 *  successfully generating the HTML test report- however this is not an issue for XML test reports.
 */
public class JunitToHTML {

    /**
     * Takes one argument in the form of an absoute path to an Intellij XML test report.
     * @param args - One argument, being the abs path to the XML test report.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {

        ClassPathResource r = new ClassPathResource("test-results-template.html");
        File htmltemplateFile = r.getFile();
        String htmlString = FileUtils.readFileToString(htmltemplateFile);

        File XMLFile = new File(args[0]);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(XMLFile);

        doc.getDocumentElement().normalize();

        //Loop tests in suite
        NodeList nList = doc.getElementsByTagName("test");
        String content = "";

        for (int i = 0; i < nList.getLength(); i++) {

            String testTmpl = "<ul>"
                    + "<li class=\"level test\">"
                    + "<span><em class=\"time\">"
                    + "<div class=\"time\">$runduration</div>"
                    + "</em><em class=\"status\">$status</em>$runname</span>"
                    + "<ul><li>$runbody</li></ul>"
                    + "</li>"
                    + "</ul>";
            Node nNode = nList.item(i);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                String runBody = "";
                Element eElement = (Element) nNode;
                NodeList outList = ((Element) nNode).getElementsByTagName("output");
                for(int q = 0; q < outList.getLength(); q++) {
                    Node outNode = outList.item(q);
                    runBody += outNode.getTextContent();
                }

                testTmpl = testTmpl.replace("$runduration", eElement.getAttribute("duration"));
                testTmpl = testTmpl.replace("$runname", eElement.getAttribute("name"));
                testTmpl = testTmpl.replace("$status", eElement.getAttribute("status"));
                testTmpl = testTmpl.replace("$runbody", runBody);
                content += testTmpl;
            }

        }

        // Get suite attributes
        nList = doc.getElementsByTagName("testrun");
        String title = "";
        String duration = "";
        for (int i = 0; i < nList.getLength(); i++) {

            Node nNode = nList.item(i);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                Element eElement = (Element) nNode;
                title = eElement.getAttribute("name");
                duration = eElement.getAttribute("duration");

            }
        }

        //Loop counts
        nList = doc.getElementsByTagName("count");
        String total = "0";
        String passed = "0";
        String failed = "0";
        for (int i = 0; i < nList.getLength(); i++) {

            Node nNode = nList.item(i);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                Element eElement = (Element) nNode;
                if(eElement.getAttribute("name").equals("total")) {
                    total = eElement.getAttribute("value");
                } else if(eElement.getAttribute("name").equals("passed")) {
                    passed = eElement.getAttribute("value");
                } else if(eElement.getAttribute("name").equals("failed")) {
                    failed = eElement.getAttribute("value");
                }
            }
        }

        htmlString = htmlString.replace("$testname", title)
                                .replace("$duration", duration)
                                .replace("$total", total)
                                .replace("$passed", passed)
                                .replace("$failed", failed)
                                .replace("$content", content);

        File newHtmlFile = new File(title + ".html");
        FileUtils.writeStringToFile(newHtmlFile, htmlString);
    }

}
