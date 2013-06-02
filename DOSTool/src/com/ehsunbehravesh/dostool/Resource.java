package com.ehsunbehravesh.dostool;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ehsun.behravesh
 */
public class Resource implements Comparable<Resource> {  
  
  public enum Type {
    GET, POST;
  }

  private URL url;
  private long responseTime;
  private Type type;
  private List<String> inputs;
  
  Resource(URL url) {
    this.url = url;
    type = Type.GET;
    inputs = new ArrayList<>();
  }

  public List<String> getInputs() {
    return inputs;
  }

  public void setInputs(List<String> inputs) {
    this.inputs = inputs;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public URL getUrl() {
    return url;
  }

  public void setUrl(URL url) {
    this.url = url;
  }

  public long getResponseTime() {
    return responseTime;
  }

  public void setResponseTime(long responseTime) {
    this.responseTime = responseTime;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Resource) {
      Resource resource = (Resource) obj;
      if (resource.url.toString().equalsIgnoreCase(url.toString())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int compareTo(Resource resource) {
    return (int) responseTime - (int) resource.responseTime;
  }
  
  public String getInputsAsString() {
    StringBuilder builder = new StringBuilder();
    
    for (int i = 0; i < inputs.size(); i++) {
      String input = inputs.get(i);
      builder.append(input).append("=").append(randomValue(input));
      
      if (i < inputs.size() - 1) {
        builder.append("&");
      }
    }
    
    return builder.toString();
  }
  
  private String randomValue(String name) {
    return name;
  }
}
