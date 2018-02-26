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

import net.ontopia.topicmaps.core.TopicIF;

public class DoSetInASFilterState implements RecoveryObjectIF {
  private TopicIF scope;
  private boolean useInFilter;

  public DoSetInASFilterState(TopicIF scope, boolean useInFilter) {
    this.scope = scope;
    this.useInFilter = useInFilter;
  }

  @Override
  public void execute(TopicMapView view) {
    view.controller.getConfigurationManager()
        .setInAssociationScopeFilter(scope, useInFilter);
    view.controller.getVizPanel().getAssociationScopeFilterMenu().
        setInAssociationScopeFilter(scope, useInFilter);
  }
}
