package de.tum.wikihelpbot;

import bridgempp.bot.example.BridgeMPPBotExample.Message;


/**
 * 
 * @author Ediacarium
 *
 * The WikiHelpBot integration class for BridgeMPP
 *
 */
public class WikiHelpBotBrideMPPIntegration implements bridgempp.bot.example.BridgeMPPBotExample.Bot {

	public static final String Name = "WikiHelpBot";

	WikiHelpBot helpBot;

	public WikiHelpBotBrideMPPIntegration() {
		this.helpBot = new WikiHelpBot("de");
	}

	@Override
	public bridgempp.bot.example.BridgeMPPBotExample.Message messageRecieved(bridgempp.bot.example.BridgeMPPBotExample.Message message) {

		String wikiBotMessage = helpBot.getWikiBotWisdom(message.message);

		return wikiBotMessage == null ? null : new Message(Name, helpBot.needWikiResponse(message.message));
	}

}