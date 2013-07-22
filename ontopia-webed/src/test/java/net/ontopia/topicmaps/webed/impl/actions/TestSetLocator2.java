/*
 * #!
 * Ontopia Webed
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

package net.ontopia.topicmaps.webed.impl.actions;

import java.io.IOException;
import java.util.*;
import net.ontopia.utils.ontojsp.FakeServletRequest;
import net.ontopia.utils.ontojsp.FakeServletResponse;
import net.ontopia.topicmaps.webed.core.*;
import net.ontopia.topicmaps.webed.impl.basic.*;
import net.ontopia.topicmaps.webed.impl.actions.*;
import net.ontopia.topicmaps.webed.impl.actions.occurrence.*;
import net.ontopia.topicmaps.webed.impl.basic.Constants;
import net.ontopia.topicmaps.core.*;
import net.ontopia.topicmaps.utils.*;
import net.ontopia.infoset.core.*;
import net.ontopia.infoset.impl.basic.URILocator;

/**
 * Tests SetLocator2 by overriding the action creation from TestSetLocator
 * and the one test which behaves differently.
 */
public class TestSetLocator2 extends TestSetLocator {
  
  public TestSetLocator2(String name) {
    super(name);
  }

  public void setUp() {
    super.setUp();
    action = new SetLocator2();
  }

  // --- Actions with different behaviour
  
  public void testEmptyURL() throws IOException {
    TopicIF topic = getTopicById(tm, "tromso");
    OccurrenceIF occ = getOccurrenceWithLocator(topic);
    int occsbefore = topic.getOccurrences().size();
    
    ActionParametersIF params = makeParameters(occ, "");
    ActionResponseIF response = makeResponse();
    action.perform(params, response);

    int occsnow = topic.getOccurrences().size();
    assertTrue("Occurrence not deleted from parent topic",
               occsbefore == occsnow + 1);
  }
  
}
