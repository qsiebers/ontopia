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

import net.ontopia.topicmaps.core.OccurrenceIF;
import net.ontopia.utils.ObjectUtils;
import ontopoly.model.FieldInstance;
import ontopoly.models.FieldValueModel;
import ontopoly.pages.AbstractOntopolyPage;
import ontopoly.validators.ExternalValidation;
import ontopoly.validators.RegexValidator;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;


public class FieldInstanceNumberField extends TextField<String> {

  private FieldValueModel fieldValueModel;
  private String oldValue;
  private String cols = "20";
  
  public FieldInstanceNumberField(String id, FieldValueModel fieldValueModel) {
    super(id);
    this.fieldValueModel = fieldValueModel;
    
    OccurrenceIF occ = (OccurrenceIF)fieldValueModel.getObject();
    this.oldValue = (occ == null ? null : occ.getValue());
    setModel(new Model<String>(oldValue));
    
    add(new RegexValidator(this, fieldValueModel.getFieldInstanceModel()) {
      @Override
      protected String getRegex() {
        return "^[-+]?\\d+(\\.\\d+)?$";   
      }
      @Override
      protected String resourceKeyInvalidValue() {
        return "validators.RegexValidator.number";
      }
    });

    // validate field using registered validators
    ExternalValidation.validate(this, oldValue);
  }

  public void setCols(int cols) {
    this.cols = Integer.toString(cols);
  }
  
  @Override
  protected void onComponentTag(ComponentTag tag) {
    tag.setName("input");
    tag.put("type", "text");
    tag.put("size", cols);
    super.onComponentTag(tag);
  }
  
  @Override
  protected void onModelChanged() {
    super.onModelChanged();
    String newValue = (String)getModelObject();
    if (ObjectUtils.equals(newValue, oldValue)) return;
    AbstractOntopolyPage page = (AbstractOntopolyPage)getPage();
    FieldInstance fieldInstance = fieldValueModel.getFieldInstanceModel().getFieldInstance();
    if (fieldValueModel.isExistingValue() && oldValue != null)
      fieldInstance.removeValue(oldValue, page.getListener());
    if (newValue != null && !newValue.equals("")) {
      fieldInstance.addValue(newValue, page.getListener());
      fieldValueModel.setExistingValue(newValue);
    }
    oldValue = newValue;
  }
  
}
