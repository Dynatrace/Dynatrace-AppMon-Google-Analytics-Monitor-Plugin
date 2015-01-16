package com.dynatrace.diagnostics.plugin.GoogleAnalytics;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.google.gdata.client.analytics.AnalyticsService;
import com.google.gdata.client.analytics.DataQuery;
import com.google.gdata.data.analytics.AccountEntry;
import com.google.gdata.data.analytics.AccountFeed;
import com.google.gdata.data.analytics.DataEntry;
import com.google.gdata.data.analytics.DataFeed;
import com.google.gdata.data.analytics.Dimension;
import com.google.gdata.data.analytics.Metric;
import com.google.gdata.util.ServiceException;

public class GoogleAnalyticsMonitorImpl {

	// monitor parameters
	private String website;					// which web site we want to query - a Google Account can have multiple websites subscribed
	private String username, password;		// Google Account username and password
	private Logger log;						// logger to be used
	private boolean logDetails = false;		// Log details?
	private boolean includeHours = false;	// include hours or only days in dimensions

	private String table_id = null;			// internal Google Table ID for the website 
	private String actualWebSite;			// actual monitored website -> in case website is not found we fallback to first subscribed
		
	// store the last retrieved result and when it was retrieved
	private List<DataEntry> secondLatestResults;
	private List<DataEntry> latestResults;
	private Date latestResultsFrom, secondLatestResultsFrom;

	// GA Dimensions and Metrics
	public static final String DATE = "ga:date";
	public static final String HOUR = "ga:hour";
	public static final String EXITS = "ga:exits";
	public static final String NEWVISITS = "ga:newVisits";
	public static final String VISITS = "ga:visits";
	public static final String BOUNCES = "ga:bounces";
	public static final String TIMEONSITE = "ga:timeOnSite";
	public static final String TIMEONPAGE = "ga:timeOnPage";
	public static final String PAGEVIEWS = "ga:pageViews";
	
	/**
	 * Builds a concatenated String list for the query parameters
	 * @param prefix
	 * @param args
	 * @return
	 */
	protected static String metricsList(String prefix, String... args) {
		StringBuffer sb = new StringBuffer();
		for(String arg : args) {
			if(sb.length() > 0) sb.append(",");
			if(prefix != null) sb.append(prefix);
			sb.append(arg);
		}
		
		return sb.toString();
	}
	
	/**
	 * Constructor - does not establish a connection - this is done by retrieveNextFeed
	 * @param website - which website to monitor, e.g.: www.dynatrace.com
	 * @param username - Google Account username
	 * @param password - Google Account password
	 * @param includeHours - if true - data is retrieved with hours on the dimension -> more granular data
	 * @param log - logger to be used for log information
	 * @param logDetails - if true - logs all retrieved data
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ServiceException
	 */
	public GoogleAnalyticsMonitorImpl(String website, String username, String password, boolean includeHours, Logger log, boolean logDetails) throws MalformedURLException, IOException, ServiceException {
		this.website = website;
		this.username = username;
		this.password = password;
		this.log = log;
		this.logDetails = logDetails;
		this.includeHours = includeHours;
	}
	
	/**
	 * Returns the actual website that is monitored - it could be different to website parameter passed in the constructor in case the site was not found
	 * @return
	 */
	public String getActualWebSite() {
		return actualWebSite;
	}
	
	/** 
	 * Requests the account data and looksup the table_id of the requested website
	 * Called internally by getNextFeed
	 */ 
	protected void getAccountFeed(AnalyticsService analyticsService)
		throws IOException, MalformedURLException, ServiceException { 
	 
		// Construct query from a string. 
	    URL queryUrl = new URL( 
	        "https://www.google.com/analytics/feeds/accounts/default?max-results=50"); 
	 
	    // Make request to the API. 
	    AccountFeed accountFeed = analyticsService.getFeed(queryUrl, AccountFeed.class); 
	 
	    // Output the data to the screen. 
	    log.info("-------- Account Feed Results --------");
	    List<AccountEntry> entries = accountFeed.getEntries();
	    for (AccountEntry entry : entries) { 
	      log.info( 
	        "\nAccount Name = " + entry.getProperty("ga:accountName") + 
	        "Profile Name = " + entry.getTitle().getPlainText() + 
	        "Profile Id = " + entry.getProperty("ga:profileId") + 
	        "Table Id = " + entry.getTableId().getValue());
	      
	      if(entry.getTitle().getPlainText().equalsIgnoreCase(website)) {
	    	  table_id = entry.getTableId().getValue();
	    	  actualWebSite = website;
	      }
	    }
	    
	    // default to first website
	    if(table_id == null && entries.size() > 0) {
	    	table_id = entries.get(0).getTableId().getValue();
	    	actualWebSite = entries.get(0).getTitle().getPlainText();
	    }
	} 
	 
	/** 
	 * Retrieves the next data feed from Google Analytics
	 * Access the latest results or the delta via the getLatestResults or getLatestDelta method
	 */ 
	public void retrieveNextFeed() 
	      throws IOException, MalformedURLException, ServiceException { 
	 
	    // Service Object to work with the Google Analytics Data Export API. 
		AnalyticsService analyticsService = new AnalyticsService("dynaTrace-GAMonitor-v10"); 
	    // Client Login Authorization. 
	    analyticsService.setUserCredentials(username, password);	    
	    getAccountFeed(analyticsService);
		
	    // Create a query using the DataQuery Object. 
	    DataQuery query = new DataQuery(new URL("https://www.google.com/analytics/feeds/data"));
	    
	    // set the date range to include yesterday, today and tomorrow
	    Calendar cal = Calendar.getInstance();
	    secondLatestResultsFrom = latestResultsFrom;
	    latestResultsFrom = cal.getTime();
	    cal.add(Calendar.DAY_OF_MONTH, -1); // yesterday
	    query.setStartDate(String.format("%1$tY-%1$tm-%1$td", cal));
	    cal.add(Calendar.DAY_OF_MONTH, 2); // tomorrow
	    query.setEndDate(String.format("%1$tY-%1$tm-%1$td", cal));
	    
	    // query.setDimensions("ga:pageTitle,ga:pagePath");
	    query.setDimensions(includeHours ? metricsList(null, DATE, HOUR) : metricsList(null, DATE));
	    // query.setMetrics("ga:pageviews,ga:bounces,ga:entrances,ga:exits,ga:newVisits,ga:timeOnPage,ga:timeOnSite,ga:visitors,ga:visits,ga:uniquePageviews");
	    query.setMetrics(metricsList(null, PAGEVIEWS, BOUNCES, NEWVISITS, EXITS, VISITS, TIMEONSITE, TIMEONPAGE));
	    query.setSort(includeHours ? metricsList("-", DATE, HOUR) : metricsList("-", DATE));
	    // query.setMaxResults(10);
	    query.setIds(table_id); 
	 
	    // Make a request to the API. 
	    DataFeed dataFeed = analyticsService.getFeed(query.getUrl(), DataFeed.class); 
	 
	    // Output data to the screen.     
    	secondLatestResults = latestResults;
    	latestResults = dataFeed.getEntries();
    	
    	if(logDetails) {
    		log.info("-- Latest Data Feed Results for " + website + "---");
    		printResults(latestResults, log);
    		if(secondLatestResults != null) {
        		log.info("-- Previous Data Feed Results for " + website + "---");
    			printResults(secondLatestResults, log);
    		}
    	}
	}
	
	/**
	 * Prints all results to the logger
	 * @param results
	 * @param log - can be null
	 * @return log output
	 */
	public static String printResults(List<DataEntry> results, Logger log) {
		if(results == null || results.size() == 0) return "";
		
		StringBuffer sb = new StringBuffer();
		for (DataEntry entry : results) {
			sb.append(printEntry(entry, null));
	    } 		
		
		String logOutput = sb.toString();		
		if(log != null)
			log.info(logOutput);
		
		return logOutput;
	}
		
	/**
	 * Prints an indivdual data entry
	 * @param entry entry to print
	 * @param log can be null
	 * @return log output
	 */
	public static String printEntry(DataEntry entry, Logger log) {
		if(entry == null) return "";
		
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		for(Dimension dim : entry.getDimensions()) {
			if(dim == null) continue;
			sb.append(dim.getName() != null ? dim.getName() : "null");
			sb.append("=");
			sb.append(dim.getName() != null ? dim.getValue() : "null");
			sb.append(",");
		}
		
		for(Metric metric : entry.getMetrics()) {
			if(metric == null) continue;
			sb.append(metric.getName() != null ? metric.getName() : "null");
			sb.append("=");
			sb.append(metric.getValue() != null ? metric.getValue() : "null");
			sb.append(",");
		}
		
		String logOutput = sb.toString();		
		if(log != null)
			log.info(logOutput);
		
		return logOutput;
	}
	
	/**
	 * @return the last retrieved data set by retrieveNextFeed
	 */
	public List<DataEntry> getLatestResults() {
		return latestResults;
	}
	
	/**
	 * Retrieves the DataEntry with the most recent data - filters out all empty ones
	 * @return
	 */
	public DataEntry getMostRecentValidEntry() {
		if(latestResults == null) {
			log.info("-- NO Most Recent Result Entry for " + website + "---");
			return null;			
		}
		
		DataEntry lastValid = null;
		for(DataEntry dataEntry : latestResults) {
			if(!containsMetrics(dataEntry)) continue;
			if(lastValid == null) {lastValid = dataEntry;}
			
			if(isLaterDateTime(dataEntry, lastValid))
				lastValid = dataEntry;
		}
		
		if(lastValid != null) {
			if(logDetails)
				log.info("-- Most Recent Result Entry for " + website + " with time " + lastValid.stringValueOf(DATE) + ":" + lastValid.stringValueOf(HOUR) + "---" + printEntry(lastValid, log));			
		}
		else
			log.warning("---- NO MOST RECENT Entry found -----");
	    
		return lastValid;
	}

	/**
	 * @return true if HOUR and DATE value of firstDate > secondDate
	 */
	private boolean isLaterDateTime(DataEntry firstDate, DataEntry secondDate) {
		long secondDateValue = Long.parseLong(secondDate.stringValueOf(DATE));
		long secondHourValue = includeHours ? Long.parseLong(secondDate.stringValueOf(HOUR)) : 0;
		long firstDateValue = Long.parseLong(firstDate.stringValueOf(DATE));
		long firstHourValue = includeHours ? Long.parseLong(firstDate.stringValueOf(HOUR)) : 0;
		return ((firstDateValue >= secondDateValue) && (firstHourValue > secondHourValue));
	}
	
	/**
	 * @return true if HOUR and DATE value of firstDate == secondDate
	 */
	private boolean isSameDateTime(DataEntry firstDate, DataEntry secondDate) {
		long secondDateValue = Long.parseLong(secondDate.stringValueOf(DATE));
		long secondHourValue = includeHours ? Long.parseLong(secondDate.stringValueOf(HOUR)) : 0;
		long firstDateValue = Long.parseLong(firstDate.stringValueOf(DATE));
		long firstHourValue = includeHours ? Long.parseLong(firstDate.stringValueOf(HOUR)) : 0;
		return ((firstDateValue == secondDateValue) && (firstHourValue == secondHourValue));
	}
	
	/**
	 * @return true for metrics such as DATE and HOUR
	 */
	private boolean ignoreThisMetricInDelta(Metric metric) {
		String metricName = metric.getName();
		return metricName.equalsIgnoreCase(DATE) || metricName.equalsIgnoreCase(HOUR);
	}
	
	/**
	 * Calculates the latest deltas between the last and second to last feed
	 */
	public List<DataEntry> getLatestDeltas() {

		// iterate through the latest results and see what has changed to the previous data set
		// if there is no previous data set we just return the latest data set
		if(secondLatestResults == null) {
			log.info("-- NOT ENOUGH RESULTS FOR DELETE for " + website + "---");
			return null;
		}
		
		ArrayList<DataEntry> deltaEntries = new ArrayList<DataEntry>();
		for(DataEntry latestEntry : latestResults) {
			// lets find this entry based on date and time in the secondLatestResults
			// if it is there and there is a difference -> calculate the delta
			// if the result cannot be found in the secondToLast we probably had a date change
			// only add the delta if we actually have a delta value (> 0)
			
			boolean foundEntry = false;
			
			for(DataEntry secondLatestEntry : secondLatestResults) {
				if(isSameDateTime(latestEntry, secondLatestEntry)) {
					foundEntry = true;
					
					// calculate the delta
					DataEntry deltaEntry = getDelta(latestEntry, secondLatestEntry);
					if(deltaEntry != null)
						deltaEntries.add(deltaEntry);
					break;
				}
			}
			
			// if we havent found it but we actually have data -> add it
			if(!foundEntry && containsMetrics(latestEntry)) {
				deltaEntries.add(latestEntry);					
			}
		}
		
		if(logDetails) {
			log.info("-- Latest Delta Results for " + website + "---");
			printResults(deltaEntries, log);
		}
		
		return deltaEntries;	
	}
	
	/**
	 * Merges multiple deltas into one -> this can happen if we have deltas over hour boundaries
	 * @return
	 */
	public DataEntry getLatestDeltasMerged() {
		List<DataEntry> deltaEntries = getLatestDeltas();
		if(deltaEntries == null || (deltaEntries.size() == 0)) {
			log.info("-- NOT ENOUGH RESULTS FOR DETLTA MERGE for " + website + "---");
			return null;			
		}
		
		DataEntry mergedEntry = deltaEntries.get(0);
		for(int i=1;i<deltaEntries.size();i++) {
			DataEntry entryToMerge = deltaEntries.get(i);
			
			for(Metric metric : mergedEntry.getMetrics()) {
				String metricName = metric.getName();
				// date and hour will always contain a value - so we ignore them
				if(ignoreThisMetricInDelta(metric)) continue;
				
				long mergedValue = metric.longValue() + entryToMerge.longValueOf(metricName);
				metric.setValue(String.valueOf(mergedValue));
			}
		}
		
		if(logDetails) {
			log.info("-- Latest Delta Merged Result for " + website + "---" + printEntry(mergedEntry, null));			
		}
		return mergedEntry;
	}
	
	/**
	 * returns true if the data entry contains valid data
	 * @param latestEntry
	 * @return
	 */
	private boolean containsMetrics(DataEntry latestEntry) {
		for(Metric metric : latestEntry.getMetrics()) {
			// date and hour will always contain a value - so we ignore them
			// if(name.equalsIgnoreCase(DATE) || name.equalsIgnoreCase(HOUR)) continue;
			if(ignoreThisMetricInDelta(metric)) continue;
			
			// do we have a value > 0? then we are valid
			if(metric.longValue() > 0)
				return true;
		}
		return false;
	}

	/**
	 * returns a new DataEntry in case there is a delta between the two passed entries
	 * the returned DataEntry contains delta values
	 * @param latestEntry
	 * @param secondLatestEntry
	 * @return
	 */
	private DataEntry getDelta(DataEntry latestEntry,
			DataEntry secondLatestEntry) {

		DataEntry deltaEntry = new DataEntry();
		boolean deltaExists = false;
		
		for(Dimension dim : latestEntry.getDimensions()) {
			deltaEntry.addDimension(dim);
		}
				
		for(Metric metric : latestEntry.getMetrics()) {
			String metricName = metric.getName();
			
			// ignore those DATE and HOUR METRICS
			if(ignoreThisMetricInDelta(metric)) {
				continue;
			}
			
			// is there a difference?
			long latestValue = metric.longValue();
			long secondLatestValue = secondLatestEntry.longValueOf(metricName);
			long deltaValue = latestValue - secondLatestValue;
			if(deltaValue > 0) 
				deltaExists = true;
			else if(deltaValue < 0)
				log.warning("Negative delta on " + metricName + ":" + latestValue + "/" + secondLatestValue + "-" + latestEntry.stringValueOf(DATE) + ":" + latestEntry.stringValueOf(HOUR) + "/" + secondLatestEntry.stringValueOf(HOUR) + ":" + secondLatestEntry.stringValueOf(HOUR));
			
			// lets calculate the difference
			Metric deltaMetric = new Metric();
			deltaMetric.setName(metricName);
			deltaMetric.setType(metric.getType());
			deltaMetric.setValue(String.valueOf(deltaValue));
			deltaMetric.setConfidenceInterval(metric.getConfidenceInterval());
			deltaMetric.setImmutable(metric.isImmutable());
			deltaEntry.addMetric(deltaMetric);
		}
		
		return deltaExists ? deltaEntry : null;
	}
}