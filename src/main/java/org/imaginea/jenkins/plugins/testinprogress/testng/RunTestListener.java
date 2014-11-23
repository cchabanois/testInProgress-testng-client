package org.imaginea.jenkins.plugins.testinprogress.testng;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jenkinsci.testinprogress.messagesender.IMessageSenderFactory;
import org.jenkinsci.testinprogress.messagesender.MessageSender;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.ConstructorOrMethod;
import org.testng.log4testng.Logger;

/**
 * TestListener for a given test context (ie runId)
 * 
 * @author Varun Menon (github id: menonvarun)
 * @author Cedric Chabanois (github id:cchabanois)
 *
 */
public class RunTestListener implements ITestListener {
	private static final Logger LOGGER = Logger
			.getLogger(RunTestListener.class);
	private final ITestContext context;
	private final String runId;
	private MessageSender messageSender;
	private final Map<String, String> testIds = new HashMap<String, String>();
	private long nextTestId = 1;

	public RunTestListener(ITestContext context,
			IMessageSenderFactory messageSenderFactory) {
		this.context = context;
		this.runId = getRunId(context);
		this.messageSender = messageSenderFactory.getMessageSender();
	}

	/**
	 * Gets the run id based on the suite and current xml test name.
	 * 
	 * @param context
	 * @return
	 */
	private String getRunId(ITestContext context) {
		String suiteName = context.getSuite().getName();
		String xmlTestName = context.getCurrentXmlTest().getName();

		if ((xmlTestName == null) || ("".equalsIgnoreCase(xmlTestName))) {
			xmlTestName = "Testng xml test";
		}
		String parentName = suiteName + "-" + xmlTestName;

		return parentName;
	}

	private synchronized String getTestId(String key) {
		String test = testIds.get(key);
		if (test == null) {
			test = Long.toString(nextTestId);
			nextTestId++;
			testIds.put(key, test);
		}
		return test;
	}

	private void sendTestTree(Map<String, ArrayList<String>> classMap) throws IOException {
		Iterator<Entry<String, ArrayList<String>>> it = classMap.entrySet()
				.iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<String>> entry = it.next();
			String className = entry.getKey();

			String clssTreedIdName = runId + ":" + className;

			String classTestId = getTestId(clssTreedIdName);
			ArrayList<String> methods = entry.getValue();

			messageSender.testTree(classTestId, className, getTestId(runId),
					true);

			for (String method : methods) {
				String methodKey = method + "(" + className + ")";

				String mthdTreedIdName = runId + ":" + methodKey;
				String mthdTestId = getTestId(mthdTreedIdName);

				messageSender.testTree(mthdTestId, methodKey, classTestId,
						false);
			}
		}
	}

	private String getIdForMethod(ITestContext context, ITestNGMethod testMethod) {
		String methodKey = getMessageSenderNameForMethod(testMethod);
		String testMethodContextKey = runId + ":" + methodKey;
		String mthdTestId = getTestId(testMethodContextKey);

		return mthdTestId;
	}

	private String getMessageSenderNameForMethod(ITestNGMethod testMethod) {
		ConstructorOrMethod consMethod = testMethod.getConstructorOrMethod();
		String methodName = consMethod.getName();
		String className = consMethod.getDeclaringClass().getName();

		String methodKey = methodName + "(" + className + ")";
		return methodKey;
	}

	@Override
	public void onTestStart(ITestResult result) {
		ITestNGMethod testMethod = result.getMethod();
		String testId = getIdForMethod(result.getTestContext(), testMethod);
		String mthdKey = getMessageSenderNameForMethod(testMethod);

		try {
			messageSender.testStarted(testId, mthdKey, false);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		ITestNGMethod testMethod = result.getMethod();
		String testId = getIdForMethod(context, testMethod);
		String mthdKey = getMessageSenderNameForMethod(testMethod);
		try {
			messageSender.testEnded(testId, mthdKey, false);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void onTestFailure(ITestResult result) {
		ITestNGMethod testMethod = result.getMethod();
		String testId = getIdForMethod(result.getTestContext(), testMethod);
		String mthdKey = getMessageSenderNameForMethod(testMethod);

		String trace = getTrace(result.getThrowable());

		try {
			messageSender.testError(testId, mthdKey, trace);
			messageSender.testEnded(testId, mthdKey, false);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	private String getTrace(Throwable t) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		t.printStackTrace(writer);
		StringBuffer buffer = stringWriter.getBuffer();
		return buffer.toString();
	}

	@Override
	public void onTestSkipped(ITestResult result) {
		ITestNGMethod testMethod = result.getMethod();
		String testId = getIdForMethod(result.getTestContext(), testMethod);
		String mthdKey = getMessageSenderNameForMethod(testMethod);

		try {
			messageSender.testStarted(testId, mthdKey, true);
			messageSender.testEnded(testId, mthdKey, true);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
		// TODO : implement

	}

	@Override
	public void onStart(ITestContext context) {
		try {
			messageSender.init();
		} catch (IOException e) {
			LOGGER.error("Could not initialize TestInProgress message sender",
					e);
			throw new RuntimeIOException(e);
		}
		String parentName = runId;
		String runId = parentName;
		try {
			messageSender.testRunStarted(runId);

			Map<String, ArrayList<String>> classMap = processTestContext();

			String testId = getTestId(parentName);
			messageSender.testTree(testId, context.getCurrentXmlTest()
					.getName(), null, true);
			sendTestTree(classMap);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	private Map<String, ArrayList<String>> processTestContext() {

		Map<String, ArrayList<String>> classMap = new HashMap<String, ArrayList<String>>();
		Collection<ITestNGMethod> testMethods = Arrays.asList(context
				.getAllTestMethods());

		for (ITestNGMethod testMethod : testMethods) {
			ConstructorOrMethod consMethod = testMethod
					.getConstructorOrMethod();
			String methodName = consMethod.getName();
			String className = consMethod.getDeclaringClass().getName();
			ArrayList<String> methodList;
			if (!classMap.containsKey(className)) {
				methodList = new ArrayList<String>();
			} else {
				methodList = classMap.get(className);
			}
			methodList.add(methodName);
			classMap.put(className, methodList);
		}
		return classMap;
	}

	@Override
	public void onFinish(ITestContext context) {
		long elapsedTime = context.getEndDate().getTime()
				- context.getStartDate().getTime();
		try {
			messageSender.testRunEnded(elapsedTime);
			messageSender.shutdown();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

}
