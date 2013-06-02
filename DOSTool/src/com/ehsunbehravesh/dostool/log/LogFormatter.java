package com.ehsunbehravesh.dostool.log;

import java.text.MessageFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author ehsun7b
 */
public class LogFormatter extends Formatter {

  @Override
  public String format(LogRecord record) {
    StringBuffer result = new StringBuffer();            
    
    result.append("[").append(record.getLevel().getName()).append("]").append(" ")
            .append(MessageFormat.format(record.getMessage(), record.getParameters()))
            .append("\r\n");
    return result.toString();
  }
  
}
