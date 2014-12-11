# Google Analytics Monitor Plugin

## Overview

![images_community/download/attachments/34799646/icon.png](images_community/download/attachments/34799646/icon.png)  
![images_community/download/attachments/34799646/Dashboard.PNG](images_community/download/attachments/34799646/Dashboard.PNG)

The Google Analytics Monitor plugin enables querying values from any website monitored with Google Analytics.The plugin retrieves values such as PageViews, Vistors, New Visitors, Bounces, Exits, Time
on Page and Time on Site. This plugin allows you to view and correlate these values with application or infrastructure measures collected by dynaTrace.  
Correlating these values allows you to answer questions like: Is the Bounce Rate going up because I have a problem on my landing pages? Do my PageViews go down because of slow performance
transactions?

The illustration on the left shows a dashboard that shows results from 4 Google Monitors. 2 Monitors monitor blog.dynatrace.com - 2 monitor community.dynatrace.com. One of these monitors uses DELTA
mode - the other always retrieves the latest full data entry.

## Plugin Details

| Name | Google Analytics Monitor Plugin
| :--- | :---
| Author | Andreas Grabner (andreas.grabner@dynatrace.com)
| Supported dynaTrace Versions | >= 5.5
| License | [dynaTrace BSD](dynaTraceBSD.txt)
| Support | [Not Supported](https://community.compuwareapm.com/community/display/DL/Support+Levels)
| Release History | 2010-07-27 Initial Release
| Download | [Google Analytics Plugin](com.dynatrace.diagnostics.plugin.GoogleAnalytics_1.0.0.jar)  
| |[Google Analytics FastPack with pre-configured Dashboard and System Profile ](https://community.compuwareapm.com/community/display/DL/Google+Analytics+FastPack)

## Provided Measures

The following image shows the metrics that the monitor provides:

![images_community/download/attachments/34799646/metrics.PNG](images_community/download/attachments/34799646/metrics.PNG)

## Configuration Oracle Monitor

![images_community/download/attachments/34799646/settings.PNG](images_community/download/attachments/34799646/settings.PNG)

The monitor requires the following configuration settings:

  * Google Account Name: The Google Account that has access to Google Analytics Data 

  * Google Account Password: The Google Account password 

  * Website: The website that you want to monitor. You can monitor multiple websites with a single Google account. Default is to monitor the first registered website 

  * Return Last Data Entry: If true - the last full data entry value is returned as result. If false - the delta value to the previous retrieved value is returned. The Delta allows you to get metrics as they come in. Google provides data as granular as one hour. If you specify Last Data Entry you will always get the current total number of the current hour. 

  * Include Hours: If true - the plugin retrieves values with hourly granularity. This setting only has an impact if you specify Return Last Data Entry = true. 

  * Log Detailed Analytics Data: if true - the monitor will log all retrieved measures from the Google API to the monitor log 

## Installation

Import the Plugin into the dynaTrace Server. For details how to do this please refer to the [dynaTrace
documentation](https://community.compuwareapm.com/community.dynatrace.com/community/display/DOCDT32/Manage+and+Develop+Plugins#ManageandDevelopPlugins-ManageandDevelopPlugins).

## Troubleshooting

When running the Plugin to retrieve Delta values (Return Last Data Entry = false), and the schedule you execute the monitor is to frequent it is possible that there is no new data available from
Google Analytics. In this case you may not see values coming in from the monitor on every scheduled monitor interval. Open the Tasks/Monitors Dashlet and check the details of the monitor execution. It
will indicate if there was data available or not:

![images_community/download/attachments/34799646/no_newdata.PNG](images_community/download/attachments/34799646/no_newdata.PNG)

If there are problems with the account credentials or with the web site name please have a look at the monitors log output. Change Log Detailed Analytics Data to true in order to get additional log
output

## Feedback

Please provide feedback on this monitor either by commenting on this page or by comments on the [Community Plugins and Extensions](https://community.compuwareapm.com/community/display/DTFORUM/Community+Plugins+and+Extensions)

