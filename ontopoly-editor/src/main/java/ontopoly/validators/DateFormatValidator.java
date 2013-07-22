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
package ontopoly.validators;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;

import ontopoly.components.AbstractFieldInstancePanel;
import ontopoly.models.FieldInstanceModel;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

public abstract class DateFormatValidator extends AbstractValidator<String> {

  private FieldInstanceModel fieldInstanceModel;
  private final Component component;
  
  public DateFormatValidator(Component component, FieldInstanceModel fieldInstanceModel) {
    this.component = component;
    this.fieldInstanceModel = fieldInstanceModel;    
  }
  
  @Override
  protected void onValidate(IValidatable<String> validatable) {
    final String value = validatable.getValue();
    if (value == null) return;
    try {
      DateFormat df = createDateFormat();
      df.parse(value);
    } catch (ParseException e) {
      String message = Application.get().getResourceSettings().getLocalizer().getString(resourceKey(), (Component)null, 
            new Model<Serializable>(new Serializable() {
              @SuppressWarnings("unused")
              public String getValue() {
                return value;
              }
            }));
      component.error(AbstractFieldInstancePanel.createErrorMessage(fieldInstanceModel, new Model<String>(message)));
    }
  }

  public abstract DateFormat createDateFormat();
  
  @Override
  protected String resourceKey() {
    return "validators.DateFormatValidator";
  }
  
}
