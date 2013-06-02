/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ehsunbehravesh.dostool.log;

import com.ehsunbehravesh.dostool.ResourceFinder;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 *
 * @author ehsun7b
 */
public class LogUtil {

  public static Logger createLogger() {
    Logger logger = Logger.getLogger(ResourceFinder.class.getName());
    logger.setUseParentHandlers(false);
    Handler handler = new ConsoleHandler();
    handler.setFormatter(new LogFormatter());
    while (logger.getHandlers().length > 0) {
      logger.removeHandler(logger.getHandlers()[0]);
    }
    logger.addHandler(handler);
    return logger;
  }
}
