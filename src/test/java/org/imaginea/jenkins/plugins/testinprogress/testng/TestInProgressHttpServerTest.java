package org.imaginea.jenkins.plugins.testinprogress.testng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.jenkinsci.testinprogress.TestInProgressServers;
import org.testng.TestNG;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.Files;
import com.mkyong.testng.examples.helloworld.TestHelloWorld;

public class TestInProgressHttpServerTest {
	private TestInProgressServers testInProgressServers;
	private File testEventsDir;

	@BeforeMethod
	public void setUp() throws Exception {
		testEventsDir = Files.createTempDir();
		testInProgressServers = new TestInProgressServers(testEventsDir, 0, 0);
		testInProgressServers.start();
		System.setProperty("TEST_IN_PROGRESS_PORT", Integer
				.toString(testInProgressServers.getBuildTestEventsServerPort()));
	}

	@AfterMethod
	public void tearDown() throws Exception {
		try {
			System.setProperty("TEST_IN_PROGRESS_PORT", "");
			testInProgressServers.stop();
		} finally {
			delete(testEventsDir);
		}
	}

	@Test
	public void testTestNgTests() {
		runTestNgTests(TestHelloWorld.class);
	}

	private void runTestNgTests(Class<?>... testClasses) {
		TestNG testNG = new TestNG();
		testNG.setUseDefaultListeners(false);
		testNG.setVerbose(0);
		testNG.setTestClasses(testClasses);
		testNG.addListener(new TestNGProgressRunListener());
		testNG.run();
	}

    private void delete(File f) throws IOException {
        if (f.isDirectory()) {
          for (File c : f.listFiles())
            delete(c);
        }
        if (!f.delete())
          throw new FileNotFoundException("Failed to delete file: " + f);
      }	
	
}
