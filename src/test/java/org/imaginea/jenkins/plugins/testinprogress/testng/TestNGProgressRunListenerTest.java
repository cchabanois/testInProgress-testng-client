package org.imaginea.jenkins.plugins.testinprogress.testng;

import static org.imaginea.jenkins.plugins.testinprogress.testng.utils.TestMessageUtils.printTestMessages;
import static org.junit.Assert.*;
import static org.imaginea.jenkins.plugins.testinprogress.testng.utils.TestMessageUtils.*;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.imaginea.jenkins.plugins.testinprogress.testng.utils.JSONObjectsMessageSenderFactory;
import org.jenkinsci.testinprogress.messagesender.SimpleMessageSenderFactory;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.xml.SuiteXmlParser;
import org.testng.xml.XmlSuite;

import com.mkyong.testng.examples.helloworld.TestHelloWorld;
import com.mkyong.testng.examples.parameter.CharUtilsTest;

public class TestNGProgressRunListenerTest {

	@Test
	public void testFirstMessageIsTestRunStart() {
		// Given
		Class<?> testClass = TestHelloWorld.class;

		// When
		JSONObject[] messages = runTests(testClass)[0];

		// Then
		assertEquals("TESTC", messages[0].getString("messageId"));
	}

	@Test
	public void testLatestMessageIsTestRunEnd() {
		// Given
		Class<?> testClass = TestHelloWorld.class;

		// When
		JSONObject[] messages = runTests(testClass)[0];

		// Then
		assertEquals("RUNTIME",
				messages[messages.length - 1].getString("messageId"));
	}

	@Test
	public void testDataProvider() {
		// Given
		Class<?> testClass = CharUtilsTest.class;

		// When
		JSONObject[] messages = runTests(testClass)[0];

		// Then
		printTestMessages(messages);
	}

	@Test
	public void testSuite() {
		// Given
		String resourceName = "testng-suite.xml";

		// When
		JSONObject[][] messages = runTests(resourceName);

		// then
		assertTestMessageMatches(new JSONObject(
				"{runId:'TestAll-order',messageId:'TESTC'}"), messages[0][0],
				JSONCompareMode.LENIENT);
		assertTestMessageMatches(new JSONObject(
				"{runId:'TestAll-database',messageId:'TESTC'}"),
				messages[1][0], JSONCompareMode.LENIENT);
	}

	@Test
	public void testSuiteParallelTests() {
		// Given
		String resourceName = "testng-suite-parallel-tests.xml";

		// When
		JSONObject[][] messages = runTests(resourceName);

		// Then
		String firstTestRunId = messages[0][0].getString("runId");
		String secondTestRunId = messages[1][0].getString("runId");
		assertFalse(firstTestRunId.equals(secondTestRunId));
	}
	
	@Test
	public void testSuiteParallelMethods() {
		// Given
		String resourceName = "testng-suite-parallel-methods.xml";

		// When
		JSONObject[][] messages = runTests(resourceName);

		// Then
		String firstTestRunId = messages[0][0].getString("runId");
		String secondTestRunId = messages[1][0].getString("runId");
		assertFalse(firstTestRunId.equals(secondTestRunId));
	}	
	
	private JSONObject[][] runTests(Class<?>... testClasses) {
		TestNG testNG = new TestNG();
		testNG.setUseDefaultListeners(false);
		testNG.setVerbose(0);
		testNG.setTestClasses(testClasses);
		JSONObjectsMessageSenderFactory jsonObjectsMessageSenderFactory = new JSONObjectsMessageSenderFactory();
		testNG.addListener(new TestNGProgressRunListener(
				jsonObjectsMessageSenderFactory));
		testNG.run();
		return jsonObjectsMessageSenderFactory.getMessages();
	}

	private JSONObject[][] runTests(String resourceName) {
		SuiteXmlParser suiteXmlParser = new SuiteXmlParser();
		InputStream inputStream = getClass().getResourceAsStream(resourceName);
		XmlSuite xmlSuite = suiteXmlParser.parse(resourceName, inputStream,
				true);
		List<XmlSuite> xmlSuites = new ArrayList<XmlSuite>();
		xmlSuites.add(xmlSuite);

		TestNG testNG = new TestNG();
		testNG.setUseDefaultListeners(false);
		testNG.setVerbose(0);
		testNG.setXmlSuites(xmlSuites);
		JSONObjectsMessageSenderFactory jsonObjectsMessageSenderFactory = new JSONObjectsMessageSenderFactory();
		testNG.addListener(new TestNGProgressRunListener(
				jsonObjectsMessageSenderFactory));
		testNG.run();
		return jsonObjectsMessageSenderFactory.getMessages();
	}

}
