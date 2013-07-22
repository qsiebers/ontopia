/*
 * #!
 * Ontopoly Editor
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
package ontopoly.components;

import ontopoly.model.FieldAssignment;
import ontopoly.model.FieldDefinition;
import ontopoly.model.FieldInstance;
import ontopoly.models.FieldDefinitionModel;
import ontopoly.models.FieldInstanceModel;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

public class FieldInstanceErrorPanel extends AbstractFieldInstancePanel {

  public FieldInstanceErrorPanel(String id, final FieldInstanceModel fieldInstanceModel, Exception e) {
    super(id, fieldInstanceModel);

    FieldInstance fieldInstance = fieldInstanceModel.getFieldInstance();
    FieldAssignment fieldAssignment = fieldInstance.getFieldAssignment();
    FieldDefinition fieldDefinition = fieldAssignment.getFieldDefinition(); 

    add(new FieldDefinitionLabel("fieldLabel", new FieldDefinitionModel(fieldDefinition)));

    error(AbstractFieldInstancePanel.createErrorMessage(fieldInstanceModel, e));

    // set up container
    this.fieldValuesContainer = new WebMarkupContainer("fieldValuesContainer");
    fieldValuesContainer.setOutputMarkupId(true);    
    add(fieldValuesContainer);

    // add feedback panel
    this.feedbackPanel = new FeedbackPanel("feedback", new AbstractFieldInstancePanelFeedbackMessageFilter());
    feedbackPanel.setOutputMarkupId(true);
    fieldValuesContainer.add(feedbackPanel);
  }
  
}
