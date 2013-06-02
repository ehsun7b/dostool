package com.ehsunbehravesh.dostool;

import com.ehsunbehravesh.dostool.log.LogUtil;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ehsun7b
 */
public class ResourceResponseTime implements Runnable {

  private static final Logger logger = LogUtil.createLogger();
  
  static {
    logger.setLevel(DOSTool.level);
  }
  
  private static final int TIMES = 3;
  private Resource resource;

  public ResourceResponseTime(Resource resource) {
    this.resource = resource;
  }

  @Override
  public void run() {
    long averageResp = analyseResource(resource, TIMES);
    resource.setResponseTime(averageResp);
    logger.log(Level.INFO, "Response Time of {0}: {1}", new Object[]{resource.getUrl(),
      resource.getResponseTime()});
  }

  private long analyseResource(Resource resource, int times) {
    long sum = 0;
    for (int i = 0; i < times; ++i) {
      sum += responseTimeMillis(resource);
    }

    return sum / times;
  }

  private long responseTimeMillis(Resource resource) {
    int bufferSize = 2048 * 10;
    long time1 = System.currentTimeMillis();
    String urlOfResource = resource.getUrl().toString();
    if (urlOfResource.indexOf('#') > 0) {
      urlOfResource = urlOfResource.substring(0, urlOfResource.indexOf('#'));
    }

    if (urlOfResource.indexOf('?') > 0) {
      urlOfResource += "&ts=" + time1;
    } else {
      urlOfResource += "?ts=" + time1;
    }

    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) new URL(urlOfResource).openConnection();
      connection.setUseCaches(false);
    } catch (MalformedURLException ex) {
      logger.log(Level.WARNING, ex.getMessage());
    } catch (IOException ex) {
      logger.log(Level.WARNING, ex.getMessage());
    }

    try {
      if (resource.getType() == Resource.Type.POST) {
        String content = resource.getInputsAsString();

        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("Content-Length", "" + Integer.toString(content.getBytes().length));

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes(content);
        outputStream.flush();
        outputStream.close();
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, ex.getMessage());
    }

    try {
      if (connection != null) {
        try (InputStream is = connection.getInputStream()) {
          byte[] buffer = new byte[bufferSize];
          int read = -1;
          while ((read = is.read(buffer)) > 0) {
          }
        }
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, ex.getMessage());
    }
    long time2 = System.currentTimeMillis();
    return time2 - time1;
  }
}
