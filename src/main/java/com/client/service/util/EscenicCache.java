package com.client.service.util;

import com.client.service.EscenicService;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;


/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 19/02/14
 * Time: 10:49
 * To change this template use File | Settings | File Templates.
 */
public class EscenicCache implements Serializable {

    private int cacheRetentionInMinutes = -1;

    private HashMap<String, HttpGetInfo> urlsRequested = new HashMap<String, HttpGetInfo>();
    TreeMap<Date, String> sortedKeys = new TreeMap<Date, String>();

    private Timer timer = null;

    private Logger mLogger = Logger.getLogger(this.getClass());

    static EscenicCache escenicCache = null;

    public EscenicCache(String configurationFileName)
    {
        {
            try
            {
                Properties properties = new Properties();
                InputStream inputStream = EscenicService.class.getClassLoader().getResourceAsStream(configurationFileName);
                if (inputStream == null)
                {
                    Logger.getLogger(EscenicService.class).error("Configuration file not found. ConfigurationFileName: " + configurationFileName);

                    return;
                }
                properties.load(inputStream);

                String tmpCacheRetentionInMinutes = properties.getProperty("escenic.xmlcache.retentionInMinutes");
                if (tmpCacheRetentionInMinutes == null)
                {
                    Logger.getLogger(EscenicService.class).error("No 'escenic.xmlcache.retentionInMinutes' found. ConfigurationFileName: " + configurationFileName);

                    return;
                }
                cacheRetentionInMinutes = Long.valueOf(tmpCacheRetentionInMinutes).intValue();
            }
            catch (Exception e)
            {
                Logger.getLogger(EscenicService.class).error("Problems to load the configuration file. Exception: " + e.getMessage() + ", ConfigurationFileName: " + configurationFileName);

                return;
            }
        }

        {
            timer = new Timer();

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    cacheClean();
                }
            }, new Date(), 5 * 60 * 1000);  // every 5 minutes
        }
    }

    synchronized public HttpGetInfo getXML(String url)
    {
        HttpGetInfo httpGetInfo = null;


        httpGetInfo = urlsRequested.get(url);

        // mLogger.info("'" + url + "' was " + (httpGetInfo == null ? "not " : "") + "found in cache");


        return httpGetInfo;
    }

    synchronized public void storeXML(String url, HttpGetInfo httpGetInfo)
    {
        if (cacheRetentionInMinutes > 0)
        {
            Date expirationDate = new Date(new Date().getTime() + (cacheRetentionInMinutes * 60 * 1000));

            sortedKeys.put(expirationDate, url);

            urlsRequested.put(url, httpGetInfo);
        }

        // mLogger.info("EscenicCache size: " + urlsRequested.size());
    }

    static public EscenicCache getInstance()
    {
        if (escenicCache == null)
        {
            escenicCache = new EscenicCache(EscenicService.configurationFileName);
        }

        return escenicCache;
    }

    private void cacheClean() {
        Date now = new Date();

        mLogger.info("Checking cache...");

        for (Map.Entry entry : sortedKeys.entrySet()) {
            Date expirationDate = (Date) entry.getKey();
            String url = (String) entry.getValue();

            if (expirationDate.getTime() < now.getTime())
            {
                sortedKeys.remove(expirationDate);
                urlsRequested.remove(url);

                mLogger.info("Removed url: " + url + ", expirationDate: " + expirationDate);
            }
        }
    }
}
