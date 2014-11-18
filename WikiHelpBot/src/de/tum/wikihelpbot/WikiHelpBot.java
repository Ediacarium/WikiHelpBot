package de.tum.wikihelpbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

	public static final Pattern askingPattern = Pattern.compile("(?i)Was ist |What is |Was sind |Wer ist |What are ");
	
	private WikipediaAPIHandler apiHandler;
	
	public WikiHelpBot(String wikiLangDomain) {
		this.apiHandler = new WikipediaAPIHandler(wikiLangDomain);
	}



	private String getWikiHelp(String topic) {
		String wikiResponseString = apiHandler.getWikiSummary(topic);
		
		if (wikiResponseString != null) {
			try {
				return URLDecoder.decode(wikiResponseString, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return wikiResponseString;
			}
		}
		return null;
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
				answers.add(apiHandler.shortenWikiLink(words[i], answer));
			}
		}
		return answers.get(0);
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
