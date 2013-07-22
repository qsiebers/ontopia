/*
 * #!
 * Ontopia TMRAP
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

package net.ontopia.topicmaps.utils.tmrap;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;

import net.ontopia.infoset.core.LocatorIF;

/**
 * EXPERIMENTAL: An implementation that looks up topics in all
 * the given TopicIndexIFs and returns them.
 */
public class FederatedTopicIndex implements TopicIndexIF {
  protected List indexes;
  
  public FederatedTopicIndex(List indexes) {
    this.indexes = indexes;
  }
  
  public Collection getTopics(Collection indicators,
                              Collection sources,
                              Collection subjects) {
    Collection topics = new ArrayList();
    Iterator it = indexes.iterator();
    while (it.hasNext()) {
      TopicIndexIF index = (TopicIndexIF) it.next();
      topics.addAll(index.getTopics(indicators, sources, subjects));
    }
    return topics;
  }

  public Collection loadRelatedTopics(Collection indicators,
                                      Collection sources,
                                      Collection subjects,
                                      boolean two_step) {
    return getTopics(indicators, sources, subjects);
  }

  public Collection getTopicPages(Collection indicators,
                                  Collection sources,
                                  Collection subjects) {
    Collection pages = new ArrayList();
    Iterator it = indexes.iterator();
    while (it.hasNext()) {
      TopicIndexIF index = (TopicIndexIF) it.next();
      pages.addAll(index.getTopicPages(indicators, sources, subjects));
    }
    return pages;
  }

  public TopicPages getTopicPages2(Collection indicators,
                                   Collection sources,
                                   Collection subjects) {
    TopicPages pages = new TopicPages();    
    Iterator it = indexes.iterator();
    while (it.hasNext()) {
      TopicIndexIF index = (TopicIndexIF) it.next();
      TopicPages currentPages = index.getTopicPages2(indicators, sources, subjects);
      pages.addAll(currentPages);
    }
    return pages;
  }

  public void close() {
    Iterator it = indexes.iterator();
    while (it.hasNext()) {
      TopicIndexIF index = (TopicIndexIF) it.next();
      index.close();
    }
    indexes = null;
  }

}
