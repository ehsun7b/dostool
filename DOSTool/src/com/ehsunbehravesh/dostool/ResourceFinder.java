package com.ehsunbehravesh.dostool;

import com.ehsunbehravesh.dostool.log.LogUtil;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author ehsun.behravesh
 */
public class ResourceFinder extends Observable implements Runnable {

  private static final Logger logger = LogUtil.createLogger();  
  
  static {
    logger.setLevel(DOSTool.level);
    
  }
  
  private URL url;
  private List<Resource> resources;
  private int analyseThreads;
  private ExecutorService analyseExecutor;

  public ResourceFinder(URL url) {
    this.url = url;
    resources = new ArrayList<>();
    analyseThreads = 10;
    analyseExecutor = Executors.newFixedThreadPool(analyseThreads);
  }

  public ResourceFinder(URL url, int analyseThreads) {
    this.url = url;
    this.analyseThreads = analyseThreads;
    resources = new ArrayList<>();
    analyseExecutor = Executors.newFixedThreadPool(analyseThreads);
  }
  
  @Override
  public void run() {    
    try {
      String contentType = url.openConnection().getContentType();
      if (contentType == null) {
        logger.log(Level.SEVERE, "No internet connection or the host is down! :D");
      } else {
        if (!contentType.startsWith("text")) {
          logger.log(Level.SEVERE, "Content-type must be a subset of text not {0}", contentType);
        } else {
          Document document = Jsoup.connect(url.toString()).get();

          extractLinks(document, resources);
          extractFormActions(document, resources);
          extractImages(document, resources);

          List<Resource> newResources = new ArrayList<>(resources);
          for (Resource resource : newResources) {
            URL url1 = resource.getUrl();
            boolean isText = false;
            if (url1 != null) {
              URLConnection connection = url1.openConnection();
              if (connection != null) {
                String contentType1 = connection.getContentType();
                isText = contentType1 != null && contentType1.startsWith("text");
              }
            }

            if (isText) {
              try {
                document = Jsoup.connect(url1.toString()).get();

                extractLinks(document, resources);
                extractFormActions(document, resources);
                extractImages(document, resources);
              } catch (Exception ex) {
                logger.log(Level.WARNING, ex.getMessage());
              }
            }
          }

          /*
           * Analysing the time of all resources
           */
          analyseResources(resources);

          /*
           * sort the resources
           */
          Collections.sort(resources);
          Collections.reverse(resources);          
        }
      }
    } catch (IOException ex) {
      logger.log(Level.SEVERE, ex.getMessage());
      //ex.printStackTrace();
    }

    setChanged();
    notifyObservers();
  }

  public URL getUrl() {
    return url;
  }

  public List<Resource> getResources() {
    return resources;
  }

  private boolean exists(Resource resource, List<Resource> resources) {
    for (Resource resource1 : resources) {
      if (resource1.equals(resource)) {
        logger.log(Level.INFO, "Repeated resource has not been added. {0}", resource.getUrl());
        return true;
      }
    }
    return false;
  }

  private void extractLinks(Document document, List<Resource> resources) {
    Elements links = document.select("a");

    /*
     * Looking for <a> in the docuement and add them if 
     * on the same server.
     */
    for (Element link : links) {

      String href = link.absUrl("href");
      URL newURL = null;
      try {
        newURL = new URL(href);
      } catch (MalformedURLException ex) {
        logger.log(Level.WARNING, ex.getMessage());
      }
      if (newURL != null) {
        if (inTheSameHost(newURL, url)) {
          logger.log(Level.INFO, "Internal link found: {0}", href);
          Resource resource = new Resource(newURL);
          if (!exists(resource, resources)) {
            resources.add(resource);
          }
        } else {
          logger.log(Level.INFO, "External link found: {0}", href);
        }
      }
    }
  }

  private void extractImages(Document document, List<Resource> resources) {
    Elements images = document.select("img");

    /*
     * Looking for <a> in the docuement and add them if 
     * on the same server.
     */
    for (Element img : images) {

      String src = img.absUrl("src");
      URL newURL = null;
      try {
        newURL = new URL(src);
      } catch (MalformedURLException ex) {
        logger.log(Level.WARNING, ex.getMessage());
      }
      if (newURL != null) {
        if (inTheSameHost(newURL, url)) {
          logger.log(Level.INFO, "Internal image found: {0}", src);
          Resource resource = new Resource(newURL);
          if (!exists(resource, resources)) {
            resources.add(resource);
          }
        } else {
          logger.log(Level.INFO, "External image found: {0}", src);
        }
      }
    }
  }

  private void extractFormActions(Document document, List<Resource> resources) {
    /*
     * Looking for <form> tags and add their action if on the same
     * server.
     */
    Elements forms = document.select("form");
    for (Element form : forms) {
      String action = form.absUrl("action");
      String method = form.attr("method");

      URL newURL = null;
      try {
        newURL = new URL(action);
      } catch (MalformedURLException ex) {
        logger.log(Level.WARNING, ex.getMessage());
      }


      if (newURL != null) {
        if (inTheSameHost(newURL, url)) {
          logger.log(Level.INFO, "Internal form action found: {0}", action);
          Resource resource = new Resource(newURL);
          if (!exists(resource, resources)) {

            /*
             * set the resource type
             */
            if (method != null) {
              if (method.toLowerCase().contains("post")) {
                resource.setType(Resource.Type.POST);
              }
            }
            /*
             * set the resource inputs (form inputs)
             */
            Elements inputElements = form.select("input");

            if (inputElements != null && inputElements.size() > 0) {
              for (Element inputElement : inputElements) {
                String name = inputElement.attr("name");

                resource.getInputs().add(name);
              }
            }

            resources.add(resource);
          }
        } else {
          logger.log(Level.INFO, "External form action found: {0}", action);
        }
      }
    }
  }

  private void analyseResources(List<Resource> resources) throws MalformedURLException, IOException {
    logger.log(Level.INFO, "All resources: {0}", resources.size());
    for (Resource resource : resources) {
      ResourceResponseTime responseTime = new ResourceResponseTime(resource);
      analyseExecutor.execute(responseTime);
    }
    
    analyseExecutor.shutdown();
    while (!analyseExecutor.isTerminated()) {      
      
    }
    
    logger.log(Level.INFO, "Analysing the resources finished.");
  }  

  /*
  public static void main(String[] args) throws MalformedURLException {
    ResourceFinder resourceFinder = new ResourceFinder(new URL(args[0]), Integer.parseInt(args[1]));
    new Thread(resourceFinder).start();
  }
  */

  private boolean inTheSameHost(URL url1, URL url2) {
    String newHost = url1.getHost();
    newHost = newHost.toLowerCase().startsWith("www.") ? newHost.toLowerCase().substring(4) : newHost;

    String host = url2.getHost();
    host = host.toLowerCase().startsWith("www.") ? host.toLowerCase().substring(4) : host;

    return newHost.equalsIgnoreCase(host);
  }
  
}
