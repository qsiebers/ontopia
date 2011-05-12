package net.ontopia.presto.spi.impl.couchdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.ontopia.presto.spi.PrestoChangeSet;
import net.ontopia.presto.spi.PrestoField;
import net.ontopia.presto.spi.PrestoFieldUsage;
import net.ontopia.presto.spi.PrestoTopic;
import net.ontopia.presto.spi.PrestoType;

public class CouchChangeSet implements PrestoChangeSet {

  private final CouchDataProvider dataProvider;

  private CouchTopic topic;
  private final PrestoType type;

  private final List<Change> changes = new ArrayList<Change>();
  private boolean saved;


  CouchChangeSet(CouchDataProvider dataProvider, CouchTopic topic) {
    this.dataProvider = dataProvider;
    this.topic = topic;
    this.type = null;
  }

  CouchChangeSet(CouchDataProvider dataProvider, PrestoType type) {
    this.dataProvider = dataProvider;
    this.topic = null;
    this.type = type;
  }

  public void setValues(PrestoFieldUsage field, Collection<Object> values) {
    if (saved) {
      throw new RuntimeException("Can only save a changeset once.");
    }
    changes.add(new Change(Change.ChangeType.SET, field, values));
  }

  public void addValues(PrestoFieldUsage field, Collection<Object> values) {
    if (saved) {
      throw new RuntimeException("Can only save a changeset once.");
    }
    changes.add(new Change(Change.ChangeType.ADD, field, values));
  }

  public void removeValues(PrestoFieldUsage field, Collection<Object> values) {
    if (saved) {
      throw new RuntimeException("Can only save a changeset once.");
    }
    changes.add(new Change(Change.ChangeType.REMOVE, field, values));
  }

  public PrestoTopic save() {
    this.saved = true;

    boolean isNew = false;
    if (topic == null) {
      if (type != null) {
        topic = dataProvider.newInstance(type);
        isNew = true;
      } else {
        throw new RuntimeException("No topic and no type. I'm sorry, Dave. I'm afraid I can't do that.");
      }
    }

    List<Change> inverseChanges = new ArrayList<Change>();

    for (Change change : changes) {
      PrestoFieldUsage field = change.getField();
      Collection<Object> values = change.getValues();
      switch(change.getType()) {
      case SET: {
        Collection<Object> existingValues = topic.getValues(field);
        Collection<Object> removableValues = new HashSet<Object>(existingValues);          
        Collection<Object> addableValues = new HashSet<Object>();

        for (Object value : values) {
          removableValues.remove(value);
          if (!existingValues.contains(value)) {
            addableValues.add(value);
          }
        }
        //        System.out.println("-----e> " + existingValues);
        //        System.out.println("-----a> " + addableValues);
        //        System.out.println("-----r> " + removableValues);

        if (!addableValues.isEmpty()) {
          inverseChanges.add(new Change(Change.ChangeType.ADD, field, addableValues));
        }
        if (!removableValues.isEmpty()) {
          inverseChanges.add(new Change(Change.ChangeType.REMOVE, field, removableValues));
        }

        topic.setValue(field, values);        
        break;
      }
      case ADD: {
        topic.addValue(field, values);
        inverseChanges.add(change);
        break;
      }
      case REMOVE: {
        topic.removeValue(field, values);
        inverseChanges.add(change);
        break;
      }
      }
      //      System.out.println("F: " + field.getId());

      // update name property
      if (field.isNameField()) {
        topic.updateNameProperty(values);
      }
    }
    if (isNew) {
      dataProvider.getCouchConnector().create(topic.getData());
      //      System.out.println("C: " + data.get("_id").toString() + " " + data.get("_rev").toString());
    } else {
      dataProvider.getCouchConnector().update(topic.getData());      
      //      System.out.println("U: " + data.get("_id").toString() + " " + data.get("_rev").toString());
    }

    updateInverseFields(isNew, inverseChanges);

    return topic;
  }

  private void updateInverseFields(boolean isNew, List<Change> inverseChanges) {
    // update inverse fields
    for (Change change : inverseChanges) {
      PrestoFieldUsage field = change.getField();
      String inverseFieldId = field.getInverseFieldId();
      if (inverseFieldId != null) {
        //      System.out.println("Ch: " + field.getId() + " " + change.getType() + " " +  values);
        for (Object value : change.getValues()) {
          
          CouchTopic valueTopic = (CouchTopic)value;
          PrestoType type = field.getSchemaProvider().getTypeById(valueTopic.getTypeId());
          PrestoField inverseField = type.getFieldById(inverseFieldId);
          
          switch(change.getType()) {
          case SET: {
            throw new RuntimeException("Should not get one of these.");
          }
          case ADD: {
            //            System.out.println("IF1: " + field.getId() + " " + inverseFieldId + " " + inverseField + " " + valueTopic.getData());
            valueTopic.addValue(inverseField, Collections.singleton(topic));
            //            System.out.println("IF2: " + field.getId() + " " + inverseFieldId + " " + inverseField + " " + valueTopic.getData());
            dataProvider.getCouchConnector().update(valueTopic.getData());      
            //            System.out.println("U2: " + valueData.get("_id").toString() + " " + valueData.get("_rev").toString());
            break;
          }
          case REMOVE: {
            if (!isNew) {
              //              System.out.println("IF1: " + field.getId() + " " + inverseFieldId + " " + inverseField + " " + valueTopic.getData());
              valueTopic.removeValue(inverseField, Collections.singleton(topic));
              //              System.out.println("IF2: " + field.getId() + " " + inverseFieldId + " " + inverseField + " " + valueTopic.getData());
              dataProvider.getCouchConnector().update(valueTopic.getData());      
              //              System.out.println("U2: " + valueData.get("_id").toString() + " " + valueData.get("_rev").toString());
            }
            break;
          }
          }
        }
      }
    }
  }

  private static class Change {

    static enum ChangeType { SET, ADD, REMOVE };

    private ChangeType type;
    private final PrestoFieldUsage field;
    private final Collection<Object> values;

    Change(ChangeType type, PrestoFieldUsage field, Collection<Object> values) {
      this.type = type;
      this.field = field;
      this.values = values;      
    }

    public ChangeType getType() {
      return type;
    }

    public PrestoFieldUsage getField() {
      return field;
    }
    public Collection<Object> getValues() {
      return values;
    }
  }

}
