package de.tum.wikihelpbot;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WikiHelpBot {

	public static final Pattern removeHTMLTagsPattern = Pattern.compile("<[^>]*>", Pattern.DOTALL);
	public static final Pattern askingPattern = Pattern.compile("(?i)Was ist |What is |Was sind |Wer ist |What are ");

	private final String wikiLangDomain;

	public WikiHelpBot(String wikiLangDomain) {
		this.wikiLangDomain = wikiLangDomain;
	}

	private InputStream readURL(String url) {
		InputStream wikiResponse = null;
		try {
			URL wikiurl = new URL(url);
			wikiResponse = wikiurl.openStream();

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return wikiResponse;
	}

	private String buildWikiParseURL(String topic) {

		try {
			System.out.println("http://" + wikiLangDomain + ".wikipedia.org/w/api.php?action=parse&format=xml&page=" + URLEncoder.encode(topic, "UTF-8") + "&redirects=&noimages&section=0");
			return "http://" + wikiLangDomain + ".wikipedia.org/w/api.php?action=parse&format=xml&page=" + URLEncoder.encode(topic, "UTF-8") + "&redirects=&noimages&section=0";
		} catch (UnsupportedEncodingException e) {
			return "http://de.wikipedia.org/w/api.php?action=parse&format=xml&page=" + topic + "&redirects=&noimages&section=0";

		}
	}

	private String removeHTMLfromString(String text) {

		String modWikiPageString = removeHTMLTagsPattern.matcher(text).replaceAll("");
		// System.out.println(modWikiPageString);
		return modWikiPageString;
	}

	private boolean resolveRedirection(String textNodeText) {

		boolean isRedirection = textNodeText.indexOf("<div class=\"redirectMsg\">") >= 0;

		if (isRedirection) {
			int redirectStartIndex = textNodeText.indexOf("?title=") + 7;
			int redirectEndIndex = textNodeText.indexOf("&", redirectStartIndex);
			String redirect = textNodeText.substring(redirectStartIndex, redirectEndIndex);
			System.out.println(redirect);
			textNodeText = extractHTMLPageText(readURL(buildWikiParseURL(redirect)));
		}
		return isRedirection;
	}

	private String useTablePage(String pageText) {

		String wikiTableText = pageText;
		int indexOfSisterprojects = pageText.indexOf("<div class=\"sisterproject\"") - 20;

		if (indexOfSisterprojects >= 0) {
			String tableTextwithSeeAlso = pageText.substring(0, indexOfSisterprojects);
			int indexOfSeeAlso = tableTextwithSeeAlso.lastIndexOf("<b>");
			System.out.println("tableTextwithSeeAlso: " + tableTextwithSeeAlso);
			wikiTableText = removeHTMLfromString(tableTextwithSeeAlso.substring(0, indexOfSeeAlso));
		}

		return wikiTableText;
	}
	
	private String handleTextNode(String textNodeText){
		
		System.out.println(textNodeText);
		
		if (resolveRedirection(textNodeText)) {
			System.out.println("resolved text: " + textNodeText);
			return textNodeText;
		}

		int indexOfTableEnd = textNodeText.lastIndexOf("</table>") + 9;
		int indexOfReferences = textNodeText.indexOf("<ol class=\"references\"");
		String HTMLwikiPageText = textNodeText.substring(indexOfTableEnd, indexOfReferences >= 0 ? indexOfReferences : textNodeText.length());
		System.out.println(HTMLwikiPageText);

		String wikiPageText = removeHTMLfromString(HTMLwikiPageText);
		
		if (wikiPageText.matches("[\n ]*")) {
			return useTablePage(textNodeText.substring(0,indexOfTableEnd));
		}
		return wikiPageText;

	}

	private String extractHTMLPageText(InputStream WikiXml) {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document doc;
			doc = db.parse(WikiXml);

			doc.normalize();

			NodeList textNodes = doc.getElementsByTagName("text");
			Node textNode = textNodes.item(0);
			if (textNode != null) {
				return handleTextNode(textNode.getTextContent());
			}
			return "Wikipedia doesn't respond";
		} catch (SAXException e) {
			return "SAX Exception occoured";
		} catch (ParserConfigurationException e1) {
			return "invalid ParserConfig";
		} catch (IOException e) {
			return "IOException occoured";
		}
	}

	private String getWikiHelp(String msg) {
		InputStream wikiResponseStream = readURL(buildWikiParseURL(msg));
		return extractHTMLPageText(wikiResponseStream);
	}

	public String needWikiResponse(String msg) {
		Matcher askingMatcher = askingPattern.matcher(msg);
		if (askingMatcher.find()) {
			int IndexOfQMark = msg.indexOf('?');
			int start = askingMatcher.group().length();
			System.out.println(msg.substring(start - 1, IndexOfQMark >= 0 ? IndexOfQMark : msg.length()).trim());
			return getWikiHelp(msg.substring(start - 1, IndexOfQMark >= 0 ? IndexOfQMark : msg.length()).trim());
		}
		
		
		return msg;
	}

	public static void main(String args[]) throws IOException {

		// boolean exit = false;
		WikiHelpBot helpBot = new WikiHelpBot("de");

		// while(!exit){
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(System.in));
		// String line = reader.readLine();
		// exit = line.equals("exit");
		// String wikiHelp = helpBot.needWikiResponse(line);
		//
		// if(wikiHelp != null){
		// System.out.println("Wikipedia says:");
		// System.out.println(wikiHelp);
		// }
		// }
		//

		System.out.println(helpBot.needWikiResponse("Was sind Bananen"));
		System.out.println(helpBot.needWikiResponse("Was ist Memory"));
		System.out.println(helpBot.needWikiResponse("Was ist Blubb"));

	}
}
