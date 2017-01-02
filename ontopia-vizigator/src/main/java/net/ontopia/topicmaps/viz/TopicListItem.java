/*
 * #!
 * Ontopia Vizigator
 * #-
 * Copyright (C) 2001 - 2013 The Ontopia Project
 * #-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * !#
 */
package net.ontopia.topicmaps.viz;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.ontopia.topicmaps.core.TopicIF;
import net.ontopia.utils.StringifierIF;

/** Utility class to display topics in a List. */
class TopicListItem {

  private String name;

  private TopicIF topic;

  public TopicListItem(String name) {

    this.name = name;
  }

  /**
   * Create a topic list item generating the name from topic using stringifier.
   * @param topic The topic of this list item.
   * @param stringifier Generates the name from the topic. 
   */
  public TopicListItem(TopicIF topic, StringifierIF stringifier) {
    this.topic = topic;
    this.name = stringifier.toString(topic);
  }

  /**
   * Create a topic list item with the specific topic and name.
   * @param topic The topic of this list item.
   * @param name The name of this list item.
   */
  public TopicListItem(TopicIF topic, String name) {
    this.topic = topic;
    this.name = name;
  }

  public TopicIF getTopic() {

    return topic;
  }

  public String toString() {

    return name;
  }

  /**
   * INTERNAL: PRIVATE: Purpose: A simple Comparator which sorts objects based
   * on #toString() Description: Examples:
   */
  static class TopicListItemComparator implements Comparator {

    private static TopicListItemComparator instance = new TopicListItemComparator();

    public int compare(Object first, Object second) {

      if (first == null)
        return 1;
      if (second == null)
        return -1;

      String firstString = first.toString();
      String secondString = second.toString();

      return firstString.compareToIgnoreCase(secondString);
    }

    public static TopicListItemComparator getInstance() {

      return instance;
    }

  }

  public static void sort(List list) {
    
    Collections.sort(list, TopicListItemComparator.getInstance());
    
  }
}
