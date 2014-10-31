package org.imaginea.jenkins.plugins.testinprogress.testng;

import java.io.IOException;

import org.jenkinsci.testinprogress.messagesender.IMessageSenderFactory;
import org.jenkinsci.testinprogress.messagesender.SocketMessageSenderFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.log4testng.Logger;

/**
 * Using ITestListener implementation of TestNg to implement the logic of
 * test-In-progress jenkins plugin.
 * 
 * @author Varun Menon (github id: menonvarun)
 * @author Cedric Chabanois (github id:cchabanois)
 * 
 */
public class TestNGProgressRunListener implements ITestListener {
	private static final String RUN_TEST_LISTENER_ATTRIBUTE = "runTestListener";
	private static final Logger LOGGER = Logger
			.getLogger(TestNGProgressRunListener.class);
	private final IMessageSenderFactory messageSenderFactory;

	public TestNGProgressRunListener(IMessageSenderFactory messageSenderFactory) {
		this.messageSenderFactory = messageSenderFactory;
	}

	public TestNGProgressRunListener() {
		this.messageSenderFactory = new SocketMessageSenderFactory();
	}

	public void onTestSuccess(final ITestResult result) {
		safeRun(result.getTestContext(), new IRunTestListenerRunnable() {

			@Override
			public void run(RunTestListener runTestListener) throws IOException {
				runTestListener.onTestSuccess(result);
			}
		});
	}

	public void onTestFailure(final ITestResult result) {
		safeRun(result.getTestContext(), new IRunTestListenerRunnable() {

			@Override
			public void run(RunTestListener runTestListener) throws IOException {
				runTestListener.onTestFailure(result);

			}
		});
	}

	public void onTestSkipped(final ITestResult result) {
		safeRun(result.getTestContext(), new IRunTestListenerRunnable() {

			@Override
			public void run(RunTestListener runTestListener) throws IOException {
				runTestListener.onTestSkipped(result);
			}
		});
	}

	public void onTestFailedButWithinSuccessPercentage(final ITestResult result) {
		safeRun(result.getTestContext(), new IRunTestListenerRunnable() {

			@Override
			public void run(RunTestListener runTestListener) throws IOException {
				runTestListener.onTestFailedButWithinSuccessPercentage(result);
			}
		});
	}

	public void onStart(final ITestContext context) {
		RunTestListener runTestListener = new RunTestListener(context,
				messageSenderFactory);
		setRunTestListener(context, runTestListener);
		safeRun(context, new IRunTestListenerRunnable() {

			@Override
			public void run(RunTestListener runTestListener) throws IOException {
				runTestListener.onStart(context);
			}
		});
	}

	public void onFinish(final ITestContext context) {
		safeRun(context, new IRunTestListenerRunnable() {

			@Override
			public void run(RunTestListener runTestListener) throws IOException {
				runTestListener.onFinish(context);

			}
		});
	}

	public void onTestStart(final ITestResult result) {
		safeRun(result.getTestContext(), new IRunTestListenerRunnable() {

			@Override
			public void run(RunTestListener runTestListener) throws IOException {
				runTestListener.onTestStart(result);
			}
		});

	}

	private void safeRun(ITestContext context, IRunTestListenerRunnable runnable) {
		// When parallel="methods", several test methods (and listener methods) can run at the same time
		synchronized (context) {
			RunTestListener runTestListener = getRunTestListener(context);
			if (runTestListener == null) {
				return;
			}
			try {
				runnable.run(runTestListener);
			} catch (Exception e) {
				LOGGER.error(
						"Exception occured while handling test event. The TestInProgress listener has been removed for this run.",
						e);
				removeRunTestListener(context);
			}
		}
	}

	private void setRunTestListener(ITestContext context,
			RunTestListener runTestListener) {
		context.setAttribute(RUN_TEST_LISTENER_ATTRIBUTE, runTestListener);
	}

	private RunTestListener getRunTestListener(ITestContext context) {
		RunTestListener runTestListener = (RunTestListener) context
				.getAttribute(RUN_TEST_LISTENER_ATTRIBUTE);
		return runTestListener;
	}

	private void removeRunTestListener(ITestContext context) {
		context.removeAttribute(RUN_TEST_LISTENER_ATTRIBUTE);
	}

	private static interface IRunTestListenerRunnable {

		public void run(RunTestListener runTestListener) throws IOException;

	}

}
