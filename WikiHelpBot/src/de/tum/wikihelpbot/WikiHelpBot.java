package de.tum.wikihelpbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
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
	public static final int maxSummaryCharacterCount = 200;
	
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
//			System.out.println("http://" + wikiLangDomain + ".wikipedia.org/w/api.php?action=parse&format=xml&page=" + URLEncoder.encode(topic, "UTF-8") + "&redirects=&noimages&section=0");
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
			// System.out.println(redirect);
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
			// System.out.println("tableTextwithSeeAlso: " + tableTextwithSeeAlso);
			wikiTableText = tableTextwithSeeAlso.substring(0, indexOfSeeAlso);
		}

		return removeHTMLfromString(wikiTableText);
	}

	private String handleTextNode(String textNodeText) {

		// System.out.println(textNodeText);

		if (resolveRedirection(textNodeText)) {
			// System.out.println("resolved text: " + textNodeText);
			return removeHTMLfromString(textNodeText);
		}

		int indexOfTableEnd = textNodeText.lastIndexOf("</table>") + 9;
		int indexOfReferences = textNodeText.indexOf("<ol class=\"references\"");
		String HTMLwikiPageText = textNodeText.substring(indexOfTableEnd, indexOfReferences >= 0 ? indexOfReferences : textNodeText.length());
		// System.out.println(HTMLwikiPageText);

		String wikiPageText = removeHTMLfromString(HTMLwikiPageText);

		if (wikiPageText.matches("[\n ]*")) {
			return useTablePage(textNodeText.substring(0, indexOfTableEnd));
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
			if (doc.getElementsByTagName("error").item(0) != null) {
				// System.out.println("This page does not exist");
				return null;
			}
			Node textNode = textNodes.item(0);
			if (textNode != null) {
				return handleTextNode(textNode.getTextContent());
			}
			// System.out.println("Wikipedia doesn't respond");
			return null;
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
		String wikiResponseString = extractHTMLPageText(wikiResponseStream);
//		System.out.println("Response: " + wikiResponseString);
		if (wikiResponseString != null) {
			try {
				return URLDecoder.decode(wikiResponseString, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return extractHTMLPageText(wikiResponseStream);
			}
		}
		return null;
	}

	private String shortenWikiLink(String topic, String summary){
		StringBuilder result = new StringBuilder();
		int EndSummaryPosition = maxSummaryCharacterCount > summary.length()?summary.length():maxSummaryCharacterCount;
		if(summary.indexOf('.', EndSummaryPosition) >= 0){
			EndSummaryPosition = summary.indexOf('.', EndSummaryPosition);
		}
		result.append(summary.substring(0, EndSummaryPosition));
		result.append( "\u2026" + System.lineSeparator() + "https://" + wikiLangDomain + ".wikipedia.org/wiki/" + topic);
		
		return result.toString();
	}
	
	public String needWikiResponse(String msg) {
		Matcher askingMatcher = askingPattern.matcher(msg);
		if (askingMatcher.find()) {
			int IndexOfQMark = msg.indexOf('?');
			int start = askingMatcher.group().length();
			 System.out.println(msg.substring(start - 1, IndexOfQMark >= 0 ? IndexOfQMark : msg.length()).trim());
			return getWikiHelp(msg.substring(start - 1, IndexOfQMark >= 0 ? IndexOfQMark : msg.length()).trim());
		}

		String[] words = msg.split(" ");
		List<String> answers = new ArrayList<>();
		System.out.println(words.length);
		for (int i = 0; i < words.length; i++) {
			String answer = getWikiHelp(words[i]);
			if(answer != null){
				answers.add(shortenWikiLink(words[i], answer));
			}
		}
		System.out.println("------------------------------------------");
		double rand = Math.random();
		for (String answer : answers) {
			double answerRating = answer.length() * 0.001;
			if(answerRating > rand){
				System.out.println(answer);
			}
		}
		System.out.println("done.");
		return msg;
	}

	public static void main(String args[]) throws IOException {

		boolean exit = false;
		WikiHelpBot helpBot = new WikiHelpBot("de");

		while (!exit) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line = reader.readLine();
			exit = line.equals("exit");
			String wikiHelp = helpBot.needWikiResponse(line);

			if (wikiHelp != null) {
				System.out.println("Wikipedia says:");
				 System.out.println(wikiHelp);
			}
		}

		// System.out.println(helpBot.needWikiResponse("Was sind Bananen"));
		// System.out.println(helpBot.needWikiResponse("Was ist Memory"));
		// helpBot.needWikiResponse("Was geht ab?");

	}
}
