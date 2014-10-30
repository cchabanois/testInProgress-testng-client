package org.imaginea.jenkins.plugins.testinprogress.testng.utils;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.testinprogress.messagesender.IMessageSenderFactory;
import org.jenkinsci.testinprogress.messagesender.MessageSender;
import org.json.JSONObject;

public class JSONObjectsMessageSenderFactory implements IMessageSenderFactory {
	private List<StringWriter> writers = new ArrayList<StringWriter>();
	
	/**
	 * Get all the messages sent. There will be a JSONObject[] for each MessageSender used (ie for each runId)
	 * @return
	 */
	public synchronized JSONObject[][] getMessages() {
		JSONObject[][] messages = new JSONObject[writers.size()][];
		for (int i = 0; i < writers.size(); i++) {
			String[] stringMessages = writers.get(i).toString().split("\n");
			messages[i] = getMessagesAsJSONObjects(stringMessages);
		}
		return messages;
	}

	private JSONObject[] getMessagesAsJSONObjects(String[] messages) {
		JSONObject[] jsonObjects = new JSONObject[messages.length];
		for (int i = 0; i < messages.length; i++) {
			jsonObjects[i] = new JSONObject(messages[i]);
		}
		return jsonObjects;
	}
	
	public synchronized MessageSender getMessageSender() {
		StringWriter sw = new StringWriter();
		writers.add(sw);
		return new SimpleMessageSender(sw);
	}

	private static class SimpleMessageSender extends MessageSender {

		public SimpleMessageSender(Writer pw) {
			this.writer = pw;
		}

	}

}