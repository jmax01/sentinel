package sentinel.pages;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebDriver.Timeouts;

import sentinel.exceptions.ConfigurationMappingException;
import sentinel.exceptions.ConfigurationParseException;
import sentinel.exceptions.FileNotFoundException;
import sentinel.exceptions.IOException;
import sentinel.exceptions.MissingConfigurationException;
import sentinel.exceptions.NoSuchFrameException;
import sentinel.exceptions.NoSuchWindowException;
import sentinel.exceptions.PageNotFoundException;
import sentinel.exceptions.URLNotFoundException;
import sentinel.utils.ConfigurationManager;
import sentinel.utils.StringUtils;

/**
 * The Page Manager is a singleton class that manages what page the test is on.
 * Calling setPage with a strong containing the name of the new page calls the
 * Page Factory to create the new page and return it as a Page Object.
 */
public class PageManager {
	private static final Logger log = LogManager.getLogger(PageManager.class);
	// Only one page reference should exist. We aren't doing multi-threading.
	private static Page page = null;
	// Only one page manager can exist.
	private static PageManager instance = null;
	// Page handle for the first window opened.
	private static String parentHandle = null;

	protected static WebDriver driver() {
		return page.driver;
	} // Get the driver for the current page.

	private PageManager() {
		// Exists only to defeat instantiation.
	}

	/**
	 * This method sets a Page Object based on the class name passed to it. This
	 * allows us to operate on pages without knowing they exist when we write step
	 * definitions.
	 * <p>
	 * <b>NOTE:</b> This function is currently only intended for generic step
	 * definitions, as it makes the code complex to write otherwise.
	 * 
	 * @param pageName String Must be an exact string match (including case) to the
	 *                 Page Object name (e.g. BLAMarketingPortalPage).
	 * @return Page Returns a reference to the page in case you want to use it
	 *         immediately.
     * @throws MissingConfigurationException if the requested configuration property has not been set
     * @throws ConfigurationParseException if error thrown while reading configuration file into sentinel
     * @throws ConfigurationMappingException if error thrown while mapping configuration file to sentinel
     * @throws IOException if other error occurs when mapping yml file into sentinel
	 * @throws FileNotFoundException if the sentinel configuration file does not exist.
	 * @throws PageNotFoundException if page could not be set
	 */
	public static Page setPage(String pageName) throws ConfigurationParseException, ConfigurationMappingException, MissingConfigurationException, IOException, FileNotFoundException, PageNotFoundException {
		// Ensure we only have one instance of this class, so that we always
		// return the same driver.
		if (instance == null)
			instance = new PageManager();

		// Get a page from the page factory
		PageManager.page = PageFactory.buildOrRetrievePage(pageName);
		return page;
	}

	/**
	 * This method returns the current Page Object stored in the Page Manager.
	 * 
	 * @return Page the Page Object
	 * @throws PageNotFoundException if the page cannot be found
	 */
	public static Page getPage() throws PageNotFoundException {
		if (instance == null)
			try {
				throw new Exception(
						"Page not created yet. It must be created before it can be used. Make sure you are calling getPage in your BeforeAll step with parameters.");
			} catch (Exception e) {
				log.error("utilities.Page.PageNotInitializedError: " + e.getMessage());
				if (log.getLevel() == Level.TRACE)
					e.printStackTrace();
			}
	
		if(page == null) {
			throw new PageNotFoundException("We could not find the Page you are looking for. Please check the pageObjectPackages configuration in conf/sentinel.yml and make sure it includes directory containing your page object.");
		}
		return page;
	}

	/**
	 * Open the URL passed, set the parent window handle and return it.
	 * 
	 * @param pageURL String url to open
	 * @return String WebDriver handle identifying string - unique for a window and
	 *         for each tab.
	 */
	public static String openPage(String pageURL) {
		open(pageURL);
		return parentHandle = driver().getWindowHandle();
	}

	/**
	 * Opens up the URL passed to it.
	 * <p>
	 * <b>TO DO:</b> Return a page object of the page we are on now.
	 * 
	 * @param url String Full URL to navigate to.
	 */
	protected static void open(String url) {
		driver().get(url);
	}

	/**
	 * Closes the current browser tab or window. Does not quit the driver. If this
	 * is the last window open, the next call to the driver will open a new window.
	 */
	public static void close() {
		driver().close();
	}

	/**
	 * Quits the current driver. Subsequent calls to the driver will fail. Should be
	 * used at the end of tests only.
	 */
	public static void quit() {
		driver().quit();
	}

	/**
	 * Navigates to the given URL.
	 * <p>
	 * <b>TO DO:</b> We should be checking the URL to see if we are going to an
	 * existing page, and if so, passing that off as the new page object.
	 * 
	 * @param url String the uniform resource locator
	 * @return Page the current page object for chaining
	 */
	public static Page navigateTo(String url) {
		driver().navigate().to(url);
		return page;
	}

	/**
	 * Emulate clicking the browser's forward button.
	 * <p>
	 * <b>TO DO:</b> We should be checking the URL to see if we are going to an
	 * existing page, and if so, passing that off as the new page object.
	 * 
	 * @return Page the current page object for chaining
	 */
	public static Page navigateForward() {
		driver().navigate().forward();
		return page;
	}

	/**
	 * Emulate clicking the browser's back button.
	 * <p>
	 * <b>TO DO:</b> We should be checking the URL to see if we are going to an
	 * existing page, and if so, passing that off as the new page object.
	 * 
	 * @return Page the current page object for chaining
	 */
	public static Page navigateBack() {
		driver().navigate().back();
		return page;
	}

	/**
	 * Emulate clicking the browser's refresh button.
	 * 
	 * @return Page the current page object for chaining
	 */
	public static Page refresh() {
		driver().navigate().refresh();
		return page;
	}

	/**
	 * Switches focus of the WebDriver to a new window assuming there was only one
	 * before. Finds the handle and passes that to overloaded switchToNewWindow()
	 * method
	 * <p>
	 * <b>Preconditions:</b> Expects a new tab or window to have just been opened,
	 * and for there to be only two.
	 * 
	 * @see sentinel.pages.PageManager#switchToNewWindow()
	 * @return String the window handle we are switching to
	 * @throws NoSuchWindowException if only one window is open or if the parent
	 *                               window can not be found
	 */
	public static String switchToNewWindow() throws NoSuchWindowException {
		String newHandle = null;
		Set<String> handles = driver().getWindowHandles();
		if (handles.size() == 1) {
			String errorMessage = "Only one window is open, therefore we cannot switch to a new window. Please open a new window and try again.";
			log.error(errorMessage);
			throw new NoSuchWindowException(errorMessage);
		}
		if (parentHandle == null) {
			String errorMessage = "Parent Window cannot be found. Please open a window and restart your test.";
			log.error(errorMessage);
			throw new NoSuchWindowException(errorMessage);
		}
		for (String handle : handles) {
			if (!handle.equals(parentHandle)) {
				newHandle = handle;
			}
		}
		switchToNewWindow(newHandle);
		return newHandle;
	}

	/**
	 * Overloads sentinel.pages.PageManager#switchToNewWindow() to
	 * accept a window index. Calls original
	 * sentinel.pages.PageManager#switchToNewWindow() and passes the
	 * index. This will allow for more fine grained control at a later date.
	 * 
	 * @see sentinel.pages.PageManager#switchToNewWindow()
	 * @param index String the window to which we want to switch
	 * @throws NoSuchWindowException if the expected window is already closed
	 */
	public static void switchToNewWindow(String index) throws NoSuchWindowException {
		try {
			driver().switchTo().window(index);
			log.trace(StringUtils.format("Switched to new window {}", index));
		} catch (org.openqa.selenium.NoSuchWindowException e) {
			String errorMessage = StringUtils.format(
					"The expected window is already closed or cannot be found. Please check your intended target:  {}",
					e.getMessage());
			log.error(errorMessage);
			throw new sentinel.exceptions.NoSuchWindowException(errorMessage);
		}
	}

	/**
	 * Closes the child window and returns to using the parent window. Used for
	 * things like PDFs or links opening in a new tab or window that need to be
	 * closed after a test is complete.
	 * 
	 * @return String the handle of the parent window to which we are returning
	 */
	public static String closeChildWindow() {
		close();
		driver().switchTo().window(parentHandle);
		return parentHandle;
	}

	/**
	 * Switches focus of the WebDriver to an iFrame.
	 * 
	 * @throws NoSuchFrameException if no frame is found
	 */
	public static void switchToIFrame() throws NoSuchFrameException {
		try {
			driver().switchTo().frame(0);
			log.trace("Switched to iFrame on current page");
		} catch (org.openqa.selenium.NoSuchFrameException e) {
			String errorMessage = StringUtils.format(
					"No iFrames were found on the current page. Ensure you have the correct page open, and please try again. {}",
					e.getMessage());
			log.error(errorMessage);
			throw new NoSuchFrameException(errorMessage);
		}

	}

	/**
	 * Gets the URL of the page we are currently on by calling the Page Object which
	 * has access to the driver object..
	 * 
	 * @return String the URL of currently active window
	 * @throws URLNotFoundException if URL is not found, or if exception thrown
	 *                              while retrieving the url
	 */
	public static String getCurrentUrl() throws URLNotFoundException, WebDriverException {
		String currentUrl = null;
		try {
			currentUrl = page.getCurrentUrl();
			log.trace(StringUtils.format("Current URL retrieved: {}", currentUrl));
		} catch (WebDriverException e) {
			String errorMessage = StringUtils.format(
					"An error occured when trying to find the current URL for {}. Please check the URL and try again: {}",
					page.getName(), e.getMessage());
			log.error(errorMessage);
			throw new URLNotFoundException(errorMessage);

		}
		if (currentUrl == null) {
			log.error("Current URL not found");
			throw new URLNotFoundException("Current URL could not be found. Please check the URL and try again.");
		}
		return currentUrl;
	}

	/**
	 * Sets the implicit timeout for the current driver. Calls the Configuration
	 * Manager to see if the default timeout or default time unit have been changed
	 * through a property. If not, 10 seconds will be set as the default.
	 * 
	 * @return Timeouts returns to allow object chaining for more complex calls
     * @throws MissingConfigurationException if the requested configuration property has not been set
     * @throws ConfigurationParseException if error thrown while reading configuration file into sentinel
     * @throws ConfigurationMappingException if error thrown while mapping configuration file to sentinel
     * @throws IOException if other error occurs when mapping yml file into sentinel 
	 * @throws FileNotFoundException if the sentinel configuration file does not exist.
	 */
	public static Timeouts setDefaultTimeout() throws ConfigurationParseException, ConfigurationMappingException, MissingConfigurationException, IOException, FileNotFoundException {
		return setTimeout(ConfigurationManager.getDefaultTimeout(), ConfigurationManager.getDefaultTimeUnit());
	}

	/**
	 * Sets the implicit timeout for the current driver in seconds.
	 * 
	 * @param time long the number of seconds to wait before reporting a failure to
	 *             find an element
	 * @return Timeouts returns to allow object chaining for more complex calls
	 */
	public static Timeouts setTimeout(long time) {
		return setTimeout(time, TimeUnit.SECONDS);
	}

	/**
	 * Sets the implicit timeout for the current driver.
	 * 
	 * @param time long the number of seconds to wait before reporting a failure to
	 *             find an element
	 * @param unit java.util.concurrent.TimeUnit the unit of time to wait (e.g.
	 *             seconds, milliseconds, etc)
	 * @return Timeouts returns to allow object chaining for more complex calls
	 */
	public static Timeouts setTimeout(long time, TimeUnit unit) {
		return driver().manage().timeouts().implicitlyWait(time, unit);
	}

	/**
	 * Sets page load timeout on web driver instance to the given time in the given
	 * unit of time.
	 * 
	 * @param time long the number used to set the timeout
	 * @param unit TimeUnit the measurement of time in which the timeout will be set
	 *             (defaults to seconds)
	 * @return Timeouts
	 */
	public static Timeouts setPageLoadTimeout(long time, TimeUnit unit) {
		return driver().manage().timeouts().pageLoadTimeout(time, unit);
	}

	/**
	 * Overloads waitForPageLoad method with no parameters, passthrough to
	 * waitForPageLoad with default timeout.
	 * 
	 * @return true if page loads, throws exception if an error occurs or page load
	 *         times out
     * @throws MissingConfigurationException if the requested configuration property has not been set
     * @throws ConfigurationParseException if error thrown while reading configuration file into sentinel
     * @throws ConfigurationMappingException if error thrown while mapping configuration file to sentinel
     * @throws IOException if other error occurs when mapping yml file into sentinel
	 * @throws TimeoutException if page load times out.
	 * @throws InterruptedException if exception if thrown during Thread.sleep() action
	 * @throws FileNotFoundException if the sentinel configuration file does not exist.
	 */
	public static boolean waitForPageLoad() throws TimeoutException, InterruptedException, ConfigurationParseException, ConfigurationMappingException, MissingConfigurationException, IOException, FileNotFoundException {
		return waitForPageLoad(ConfigurationManager.getDefaultTimeout());
	}

	/**
	 * Overloads waitForPageLoad method with one parameters, passthrough to
	 * waitForPageLoad with the given timeout and the default TimeUnit of seconds.
	 * 
	 * @param seconds long the number of seconds to wait before the page times out
	 * @return true if page loads, throws exception if an error occurs or page load
	 *         times out
	 * @throws TimeoutException if page load times out.
	 * @throws InterruptedException if exception if thrown during Thread.sleep() action
	 */
	public static boolean waitForPageLoad(long seconds) throws TimeoutException, InterruptedException {
		return waitForPageLoad(seconds, TimeUnit.SECONDS);
	}

	/**
	 * Sets a pageLoadTimeout and Interfaces with isPageLoaded to continually test if
	 * a page is loaded until it returns true or times out
	 * 
	 * @see PageManager#isPageLoaded()
	 * TODO: It looks like this is not actually using the passed parameters
	 * 
	 * @param time long the amount of time to wait
	 * @param unit TimeUnit the unit of time to wait for the given time value
	 * @return boolean always returns true, will throw exception if page does not
	 *          load
	 * @throws TimeoutException     if timeout occurs before the page has loaded
	 *                              (default timeout: 10000 milliseconds)
	 * @throws InterruptedException if exception if thrown during Thread.sleep()
	 *                              action
	 */
	public static boolean waitForPageLoad(long time, TimeUnit unit) throws TimeoutException, InterruptedException {
		setPageLoadTimeout(time, unit);
		while (!isPageLoaded()) {
			Thread.sleep(200);
			continue;
		}
		return true;
	}

	/**
	 * Returns true if document.readyState is complete for the current driver,
	 * meaning the page has loaded successfully. Uses the driver's pageLoadTimeout
	 * setting to throw a TimeoutException if the body element cannot be found on
	 * the page.
	 * 
	 * @return boolean true if page has loaded, false if not
	 * @throws TimeoutException if timeout occurs before page has finished loading
	 */
	private static boolean isPageLoaded() throws TimeoutException {
		try {
			// TimeoutException is triggered by using driver().findElement
			driver().findElement(By.tagName("body"));
		} catch (TimeoutException e) {
			throw new TimeoutException(
					"This page timed out before it could finish loading. Please increae the timeout, ensure the page you are loading exists, or check your internet connection and try agin.");
		}
		// if we've gotten this far, we haven't timed out so return the
		// document.readyState check
		return ((JavascriptExecutor) driver()).executeScript("return document.readyState").equals("complete");
	}
}
