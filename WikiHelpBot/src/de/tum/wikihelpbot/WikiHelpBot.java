package de.tum.wikihelpbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketException;
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

	public static final Pattern extractTextfromXMLPattern = Pattern.compile("<text xml:space=\"preserve\">(.*)</text>");
	public static final Pattern removeTableandReferencesPattern = Pattern.compile("</table>(.*)<ol class=\"references\"",Pattern.DOTALL);
	public static final Pattern removeHTMLTagsPattern = Pattern.compile("<.*>(.*)<.*>");
	public static final Pattern getTablePattern = Pattern.compile("<table>(.*)</table>");
	
	private static InputStream readURL(String url){
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
	
	private static String buildWikiParseURL(String topic){
		String result;
		try {
			return result = "http://de.wikipedia.org/w/api.php?action=parse&format=xml&page="+URLEncoder.encode(topic,"UTF-8")+"&redirects=&noimages&section=0";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "http://de.wikipedia.org/w/api.php?action=parse&format=xml&page="+topic+"&redirects=&noimages&section=0";
	}
	
	private static String removeHTMLfromString(String text){
		
		StringBuilder modWikiPageText = new StringBuilder(text);
		
		int startTag = 0;
		while((startTag = modWikiPageText.indexOf("<")) >= 0){
			int endTag = modWikiPageText.indexOf(">", startTag);
			
			modWikiPageText.delete(startTag, endTag+1);
		}
			
			String modWikiPageString = modWikiPageText.toString();
			boolean replaced ;
		do{
			String neu = modWikiPageString.replaceAll("\n\n", "\n");
			replaced = !neu.equals(modWikiPageString);
			modWikiPageString = neu;
		}while(replaced);
		return modWikiPageString;
	}
	
	
	private static String extractHTMLPageText(InputStream WikiXml) throws SAXException, IOException, ParserConfigurationException{
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(WikiXml);
		doc.normalize();
		
		NodeList textNodes = doc.getElementsByTagName("text");
		Node textNode = textNodes.item(0);
		if(textNode != null){
		String textNodeText = textNode.getTextContent();
		System.out.println(textNodeText);
		if(textNodeText.indexOf("<div class=\"redirectMsg\">") >= 0){
			int redirectStartIndex = textNodeText.indexOf("?title=")+7;
			int redirectEndIndex = textNodeText.indexOf("&",redirectStartIndex);
			String redirect = textNodeText.substring(redirectStartIndex, redirectEndIndex);
			System.out.println(redirect);
			return extractHTMLPageText(readURL(buildWikiParseURL(redirect)));
		}
//		int indexOfTableEnd = textNodeText.lastIndexOf("</table>")+9;
//		int indexOfReferences = textNodeText.indexOf("<ol class=\"references\"");
//		String HTMLwikiPageText = textNodeText.substring(indexOfTableEnd,indexOfReferences >= 0?indexOfReferences:textNodeText.length());
		
		Matcher HTMLwikiPageTextMatcher = removeTableandReferencesPattern.matcher(textNodeText);
		HTMLwikiPageTextMatcher.find();
		String HTMLwikiPageText = HTMLwikiPageTextMatcher.group(1);
		System.out.println(HTMLwikiPageText);
		
		String wikiPageText = removeHTMLfromString(HTMLwikiPageText);
		if(wikiPageText.matches("[\n ]*")){
			//return removeHTMLfromString(textNodeText.substring(0,indexOfTableEnd));
		}
		return wikiPageText;
	
		}
		return "Wikipedia doesn't respond";
	}
	
	public static String getWikiHelp(String msg){
		
		//String[] split = msg.split("Was ist");
		
		InputStream wikiResponseStream = readURL(buildWikiParseURL(msg));
		try {
			return extractHTMLPageText(wikiResponseStream);
		} catch (SAXException e) {
		} catch (IOException e) {
		} catch (ParserConfigurationException e) {
		}
		
		System.out.println("Got wiki response");
		
		String wikiLine;
		String result = new String("");

		System.out.println("done.");
		return null;
	}
	
	public static String needWikiResponse(String msg){
		Pattern pattern = Pattern.compile("(?i)Was ist |What is |Was sind |Wer ist |What are ");
		Matcher matcher = pattern.matcher(msg);
		if(matcher.find()){
			int IndexOfQMark = msg.indexOf('?');
			int start = matcher.group().length();
			System.out.println(msg.substring(start-1, IndexOfQMark >= 0? IndexOfQMark:msg.length()).trim());
			return getWikiHelp(msg.substring(start-1, IndexOfQMark >= 0? IndexOfQMark:msg.length()).trim());
		}
		return msg;
	}
	
	public static void main(String args[]) throws IOException{
		
		boolean exit = false;
		
		while(!exit){
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line = reader.readLine();
			exit = line.equals("exit");
			String wikiHelp = needWikiResponse(line);
			if(wikiHelp != null){
				System.out.println("Wikipedia says:");
				System.out.println(wikiHelp);
			}
		}
		
	}
}
