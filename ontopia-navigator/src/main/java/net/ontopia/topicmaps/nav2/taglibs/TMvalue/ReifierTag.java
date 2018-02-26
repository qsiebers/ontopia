/*
 * #!
 * Ontopia Navigator
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

package net.ontopia.topicmaps.nav2.taglibs.TMvalue;

import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import javax.servlet.jsp.JspTagException;

import net.ontopia.topicmaps.core.ReifiableIF;
import net.ontopia.topicmaps.core.TopicIF;

import net.ontopia.topicmaps.nav2.taglibs.value.BaseValueProducingAndAcceptingTag;

/**
 * INTERNAL: Finds the reifying topic of each topic map object in a
 * collection.
 */
public class ReifierTag extends BaseValueProducingAndAcceptingTag {

  @Override
  public Collection process(Collection tmObjects) throws JspTagException {
    // Find all reifying topics of all topic map objects in collection
    if (tmObjects == null || tmObjects.isEmpty())
      return Collections.EMPTY_SET;
    else {
      ArrayList reifyingTopics = new ArrayList();
      Iterator iter = tmObjects.iterator();
      TopicIF reifyingTopic;
      // Loop over the topic map objects
      while (iter.hasNext()) {
        // Get the topic that reifies the given topic map object
        reifyingTopic = ((ReifiableIF)iter.next()).getReifier();
        // If a topic was found add it to the result list.
        if (reifyingTopic != null)
          reifyingTopics.add(reifyingTopic);    
      }
      // Return all reifiying topics found.
      return reifyingTopics;
    }
  }

}
