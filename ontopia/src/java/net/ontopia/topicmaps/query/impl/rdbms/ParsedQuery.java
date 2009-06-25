// $Id: ParsedQuery.java,v 1.81 2008/07/25 20:12:16 geir.gronmo Exp $

package net.ontopia.topicmaps.query.impl.rdbms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ontopia.persistence.proxy.QueryIF;
import net.ontopia.persistence.proxy.RDBMSStorage;
import net.ontopia.persistence.query.jdo.JDOAnd;
import net.ontopia.persistence.query.jdo.JDOEvaluator;
import net.ontopia.persistence.query.jdo.JDOExpressionIF;
import net.ontopia.persistence.query.jdo.JDONot;
import net.ontopia.persistence.query.jdo.JDOOr;
import net.ontopia.persistence.query.jdo.JDOParameter;
import net.ontopia.persistence.query.jdo.JDOQuery;
import net.ontopia.persistence.query.jdo.JDOValueIF;
import net.ontopia.persistence.query.jdo.JDOVariable;
import net.ontopia.persistence.query.jdo.JDOVisitorIF;
import net.ontopia.persistence.query.sql.SQLGeneratorIF;
import net.ontopia.topicmaps.query.core.InvalidQueryException;
import net.ontopia.topicmaps.query.core.ParsedQueryIF;
import net.ontopia.topicmaps.query.core.QueryResultIF;
import net.ontopia.topicmaps.query.impl.basic.QueryMatches;
import net.ontopia.topicmaps.query.impl.utils.MultiCrossProduct;
import net.ontopia.topicmaps.query.impl.utils.QueryAnalyzer;
import net.ontopia.topicmaps.query.parser.AbstractClause;
import net.ontopia.topicmaps.query.parser.NotClause;
import net.ontopia.topicmaps.query.parser.OrClause;
import net.ontopia.topicmaps.query.parser.Pair;
import net.ontopia.topicmaps.query.parser.PredicateClause;
import net.ontopia.topicmaps.query.parser.TologQuery;
import net.ontopia.topicmaps.query.parser.Variable;
import net.ontopia.utils.OntopiaRuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * INTERNAL: Class used to represent parsed queries. The class wraps a query
 * executer and a tolog query intance (as generated by the parser). The actual
 * query execution is delegated to the query executer.
 */
public final class ParsedQuery implements ParsedQueryIF {

  // Define a logging category.
  static Logger log = LoggerFactory.getLogger(ParsedQuery.class.getName());

  protected TologQuery query;

  protected net.ontopia.topicmaps.query.impl.rdbms.QueryProcessor rprocessor;

  protected net.ontopia.topicmaps.query.impl.basic.QueryProcessor bprocessor;

  protected QueryComponentIF[] components;

  protected QueryIF sqlquery;

  protected int qresult;

  protected boolean has_bclauses = false;

  protected SQLGeneratorIF sqlgen;

  public ParsedQuery(QueryProcessor rprocessor,
      net.ontopia.topicmaps.query.impl.basic.QueryProcessor bprocessor,
      TologQuery query) throws InvalidQueryException {

    // The rdbms query processor
    this.rprocessor = rprocessor;
    // The basic query processor
    this.bprocessor = bprocessor;
    // The tolog query
    this.query = query;

    // RULES:
    //
    // - If query only contains a NOT clause the result is always
    // empty.
    //
    // - Execute JDO predicates before BASIC predicates
    //

    // Create cross products of argument types
    MultiCrossProduct cp = new MultiCrossProduct(new Map[] {
        query.getVariableTypes(), query.getParameterTypes() });
    log.debug("Argument type cross product size: " + cp.getSize());
    
    if (cp.getSize() > 1) {
      // Pass query on to the basic processor
      this.has_bclauses = true;
      this.components = new QueryComponentIF[] { new BasicQueryComponent(query,
          query.getClauses(), this.bprocessor) };
      log
          .debug("Passing on query to basic.QueryProcessor because argument type cross product is "
              + cp.getSize() + " (>1).");

    } else if (cp.getSize() == 1) {
      // Cross product size is 1, so we'll give it a try
      while (cp.nextTuple()) {
        QueryBuilder builder = new QueryBuilder(query, rprocessor);
        builder.setVariables(cp.getMap(0));
        builder.setParameters(cp.getMap(1));
        // ! System.out.println("Variables: " + builder.getVariables());
        // ! System.out.println("Parameters: " + builder.getParameters());
        // Compile query
        compileQuery(builder, query);
        break;
      }
    } else {
      // No valid type combinations, so result must be false.
      this.qresult = -1;
    }
  }

  protected void compileQuery(QueryBuilder builder, TologQuery query)
      throws InvalidQueryException {
    // ! System.out.println("TOLOG: " + query);

    if (log.isDebugEnabled())
      log.debug("TOLOG: " + query);

    // TODO:
    //
    // 1. analyze query clauses and figure out possible variable types
    //
    // 2. weed out predicates and clauses that are have predictable results
    //
    // 3. make cross product for all variable type combinations
    //
    // 4. produce query filter for clause for each variable type binding
    //
    // 5. if there are multiple possible combinations put them in a
    // union-all expression, alternatively execute them serially.
    //
    // 6. bind the topicmap property to all the variables

    List qcomponents = new ArrayList();
    JDOQuery jdoquery = null;
    int bccount = 0;

    // Prescan query
    prescan(builder, query.getClauses());
      
    // Compile clauses
    QueryContext qcontext = compile(builder, query.getClauses());

    // Pass any non-mappable clauses to the basic processor
    if (qcontext.hasClauses()) {
      qcomponents.add(new BasicQueryComponent(query, qcontext.clauses,
          this.bprocessor));
      bccount++;
      this.has_bclauses = true;
      if (log.isDebugEnabled())
        log.debug("BASIC clauses: " + qcontext.clauses);
    }

    // Add JDO components when there are JDO expressions
    if (qcontext.hasExpressions()) {

      // not doing any aggregates or ordering if basic clauses exist
      boolean aggfunc = false;
      boolean orderby = false;

      if (!this.has_bclauses) {

        // TODO: weed out predicates that refer to variables without
        // 1. order by only not ordered by TopicIF name
        orderby = !query.getOrderBy().isEmpty() && isOrderableTypes(query);

        // 2. aggregate functions only when no basic clauses
        aggfunc = !query.getCountedVariables().isEmpty();

        // ! // add basic reduce component
        // ! qcomponents.add(new BasicReduceComponent(query, this.bprocessor));
        // // NEEDED?

        // ! // FIXME: Cannot order topic variables correctly according to
        // ! // current tolog rules. This particularly applies to TopicIFs,
        // ! // since it is expected that they are ordered by name. So if
        // ! // ordered variable is not counted (ie. a number) we leave the
        // ! // ordering to the basic sort component.
        // ! for (int ix = 0; ix < oblist.size(); ix++) {
        // ! Variable var = (Variable)oblist.get(ix);
        // !
        // ! // TODO: only disable orderby if all variables are TopicIFs
        // ! if (!counted.contains(var)) {
        // ! // JDO query is not orderable
        // ! orderby = false;
        // ! break;
        // ! }
        // ! }

        // ! // add basic count component
        // ! if (!aggfunc && !counted.isEmpty()) {
        // ! qcomponents.add(new BasicCountComponent(query, this.bprocessor));
        // ! bccount++;
        // ! }

        // add basic sort component
        if (!orderby && !query.getOrderBy().isEmpty()) {
          qcomponents.add(new BasicSortComponent(query, this.bprocessor));
          bccount++;
        }
      }

      // Compile JDO query
      jdoquery = makeJDOQuery(builder, qcontext, aggfunc, orderby);
      if (log.isDebugEnabled())
        log.debug("JDO: " + jdoquery + " [vars: " + jdoquery.getVariableCount()
            + "]");

      // Evaluate JDO query and reduce query filter
      this.qresult = JDOEvaluator.evaluateExpression(jdoquery.getFilter(),
          this.rprocessor.getMapping(), true);
      if (log.isDebugEnabled())
        log.debug("JDO: evaluation result is " + this.qresult);
    }

    // RDBMS tolog only
    if (bccount == 0 && this.qresult == 0 && jdoquery != null) {
      // FIXME: May not want to wrap in matrix query, but read directly
      // into QueryMatches object.

      RDBMSStorage storage = (RDBMSStorage) this.rprocessor.getTransaction()
          .getStorageAccess().getStorage();
      this.sqlgen = storage.getSQLGenerator();

      // Set limit and offset
      if (this.sqlgen.supportsLimitOffset()) {
        jdoquery.setLimit(query.getLimit());
        jdoquery.setOffset(query.getOffset());
      }

      // Query is non-evaluatable and thus SQL executable
      this.sqlquery = this.rprocessor.getTransaction().createQuery(jdoquery,
          true);
    }

    // BASIC and RDBMS tolog
    else {
      if (jdoquery != null && this.qresult == 0) {
        // Make sure JDO component is evaluated first
        QueryIF matrix = this.rprocessor.getTransaction().createQuery(jdoquery, true);

        String[] colnames = jdoquery.getSelectedColumnNames();
        qcomponents.add(0, new JDOQueryComponent(matrix, colnames));
      }
      this.components = new QueryComponentIF[qcomponents.size()];
      qcomponents.toArray(this.components);
    }
  }

  class JDONamedAggregator implements JDOVisitorIF {

    protected Set varnames = new HashSet(5);
    protected Set parnames = new HashSet(5);

    public void visitable(JDOExpressionIF expr) {
      expr.visit(this);
    }

    public void visitable(JDOExpressionIF[] exprs) {
      for (int i = 0; i < exprs.length; i++) {
        exprs[i].visit(this);
      }
    }

    public void visitable(JDOValueIF value) {
      if (value.getType() == JDOValueIF.VARIABLE) {
        varnames.add(((JDOVariable) value).getName());
      } else if (value.getType() == JDOValueIF.PARAMETER) {
        parnames.add(((JDOParameter) value).getName());
      }
      value.visit(this);
    }

    public void visitable(JDOValueIF[] values) {
      for (int i = 0; i < values.length; i++) {
        visitable(values[i]);
      }
    }
  }

  protected JDOQuery makeJDOQuery(QueryBuilder builder, QueryContext qcontext,
      boolean aggfunc, boolean orderby) throws InvalidQueryException {
    // Do we have any top level expressions?
    if (qcontext.hasExpressions()) {

      // TODO: Do we really have to add topic map filtering expression
      // here? Make sure query is bound to the current topic map.
      //
      // Would it be better to register this stuff with QueryBuilder
      // instead?

      JDONamedAggregator visitor = new JDONamedAggregator();
      Iterator iter1 = qcontext.expressions.iterator();
      while (iter1.hasNext()) {
        ((JDOExpressionIF) iter1.next()).visit(visitor);
        ;
      }

      // Create query for AND'ed expressions
      JDOQuery jdoquery = new JDOQuery();

      // Register variables
      Iterator iter2 = visitor.varnames.iterator();
      while (iter2.hasNext()) {
        String varname = (String) iter2.next();
        Class vartype = builder.getVariableType(varname);
        if (vartype == null)
          throw new InvalidQueryException(
              "Not able to figure out type of variable: $" + varname);
        jdoquery.addVariable(varname, vartype);
      }

      // Register parameters
      Iterator iter3 = visitor.parnames.iterator();
      while (iter3.hasNext()) {
        String parname = (String) iter3.next();
        Class partype = builder.getParameterType(parname);
        if (partype == null)
          throw new InvalidQueryException(
              "Not able to figure out type of parameter: %" + parname + '%');
        jdoquery.addParameter(parname, partype);
      }

      // ! builder.registerJDOVariables(jdoquery);
      // ! builder.registerJDOParameters(jdoquery);

      // ! System.out.println("VARS: " + jdoquery.getVariableNames());
      // ! System.out.println("PARAMS: " + jdoquery.getParameterNames());

      // TODO: Add support for the DISTINCT keyword
      jdoquery.setDistinct(true);

      // ISSUE: no need to do distinct if we have basic clauses
      // ! jdoquery.setDistinct(!this.has_bclauses || !orderby);
      // ! System.out.println("X: " + jdoquery.getDistinct() + " " +
      // this.has_bclauses + " " + orderby);

      // Register select
      if (qcontext.hasClauses())
        builder.registerJDOSelectDependent(jdoquery, visitor.varnames);
      else
        builder.registerJDOSelect(jdoquery, visitor.varnames, aggfunc);

      // Register order by
      if (orderby)
        builder.registerJDOOrderBy(jdoquery, aggfunc);

      jdoquery.setFilter(new JDOAnd(qcontext.expressions));
      return jdoquery;
    } else
      return null;
  }

  public List getClauses() {
    return query.getClauses();
  }

  // / ParsedQueryIF implementation [the class does not implement the interface]

  public List getSelectedVariables() {
    return getVariables(query.getSelectedVariables());
  }

  public Collection getAllVariables() {
    return getVariables(query.getAllVariables());
  }

  public Collection getCountedVariables() {
    return getVariables(query.getCountedVariables());
  }

  public List getOrderBy() {
    return getVariables(query.getOrderBy());
  }

  public boolean isOrderedAscending(String name) {
    return query.isOrderedAscending(name);
  }

  protected List getVariables(Collection varnames) {
    List results = new ArrayList(varnames.size());
    Iterator iter = varnames.iterator();
    while (iter.hasNext()) {
      results.add(((Variable) iter.next()).getName());
    }
    return results;
  }

  public QueryResultIF execute() throws InvalidQueryException {
    return execute(null);
  }

  public QueryResultIF execute(Map arguments) throws InvalidQueryException {
    // sanity-check arguments
    QueryAnalyzer.verifyParameters(query, arguments);

    // flush transaction, need to make sure that all dirty data is stored
    rprocessor.getTransaction().flush();

    if (this.sqlquery != null) {
      try {

        // since all clauses mapped to native query equivalents we can
        // execute the entire query in one go and return the result,
        // with having to go via a QueryMatches instance.
        net.ontopia.persistence.proxy.QueryResultIF qresult =
        // execute with named arguments
        (net.ontopia.persistence.proxy.QueryResultIF) (arguments == null ? this.sqlquery
            .executeQuery()
            : this.sqlquery.executeQuery(arguments));

        // figure out if OFFSET/LIMIT is to be done in resultset or natively in
        // database
        if (this.sqlgen != null && !this.sqlgen.supportsLimitOffset()) {
          return new QueryResult(qresult, query.getSelectedVariableNames(),
              query.getLimit(), query.getOffset());
        } else {
          return new QueryResult(qresult, query.getSelectedVariableNames());
        }
      } catch (Exception e) {
        throw new OntopiaRuntimeException(e);
      }
    } else {
      // if query succeed its pre-evaluation return a boolean result.
      if (this.qresult == 1)
        return new BooleanQueryResult(query.getSelectedVariableNames(), true);
      else if (this.qresult == -1)
        return new BooleanQueryResult(query.getSelectedVariableNames(), false);

      // ISSUE: Do we need to do anything with the arguments here?
      // TODO: This is _not_ thread safe

      // set query arguments
      if (arguments != null)
        query.setArguments(arguments);

      // prepare query matches instance
      QueryMatches matches = prepareQueryMatches(arguments);

      if (log.isDebugEnabled())
        log.debug("Components: " + Arrays.asList(components));

      // loop over query components and let them process the query matches.
      if (this.components != null) {
        for (int i = 0; i < this.components.length; i++) {
          matches = this.components[i].satisfy(matches, arguments);
        }
      }

      // clear query arguments
      if (arguments != null)
        query.setArguments(null);

      // wrap QueryMatches in a QueryResultIF.
      return new net.ontopia.topicmaps.query.impl.basic.QueryResult(matches,
          query.getLimit(), query.getOffset());
    }
  }

  protected QueryMatches prepareQueryMatches(Map arguments) {
    if (this.has_bclauses)
      return bprocessor.createInitialMatches(query, arguments);
    else
      return bprocessor.createInitialMatches(query, query
          .getSelectedVariables(), arguments);
  }

  class QueryContext {

    protected List clauses = new ArrayList();

    protected List expressions = new ArrayList();

    public boolean hasClauses() {
      return !clauses.isEmpty();
    }

    public boolean hasExpressions() {
      return !expressions.isEmpty();
    }
  }

  protected void prescan(QueryBuilder builder, List clauses) {

    // detect variables that map to multiple columns    
    for (int ix = 0; ix < clauses.size(); ix++) {
      AbstractClause theClause = (AbstractClause) clauses.get(ix);

      if (theClause instanceof PredicateClause) {
        PredicateClause pclause = (PredicateClause) theClause;
        if (pclause.getPredicate() instanceof JDOPredicateIF) {
          JDOPredicateIF pred = (JDOPredicateIF) pclause.getPredicate();
          pred.prescan(builder, pclause.getArguments());
        }

      } else if (theClause instanceof OrClause) {
        OrClause clause = (OrClause) theClause;
        List alternatives = clause.getAlternatives();
        int len = alternatives.size();
        for (int i = 0; i < len; i++) {
          prescan(builder, (List) alternatives.get(i));
        }

      } else if (theClause instanceof NotClause) {
        NotClause clause = (NotClause) theClause;
        prescan(builder, clause.getClauses());

      } else
        throw new OntopiaRuntimeException("Unknown clause type:" + theClause.getClass());
    }
  }

  protected boolean isSupportedArguments(QueryBuilder builder, List arguments) {
    int len = arguments.size();
    for (int i=0; i < len; i++) {
      Object arg = arguments.get(i);
      if (arg instanceof Variable
          && !builder.isSupportedVariable((Variable)arg))
        return false;
      else if (arg instanceof Pair) {
        Pair pair = (Pair)arg;
        if (pair.getFirst() instanceof Variable
            && !builder.isSupportedVariable((Variable)pair.getFirst()))
          return false;
        if (pair.getSecond() instanceof Variable
            && !builder.isSupportedVariable((Variable)pair.getSecond()))
          return false;
      }
    }
    return true;
  }
  
  protected QueryContext compile(QueryBuilder builder, List clauses)
      throws InvalidQueryException {
    QueryContext qcontext = new QueryContext();

    for (int ix = 0; ix < clauses.size(); ix++) {
      AbstractClause theClause = (AbstractClause) clauses.get(ix);

      if (theClause instanceof PredicateClause) {
        PredicateClause pclause = (PredicateClause) theClause;
        // FIXME: If predicate is recursive we need to break up the
        // query at this point.
        if (pclause.getPredicate() instanceof JDOPredicateIF) {
          JDOPredicateIF pred = (JDOPredicateIF) pclause.getPredicate();

          // Either create JDOExpressionIFs or embed in basic executer
          List args = pclause.getArguments();
          boolean hadexpr = (isSupportedArguments(builder, args) &&
                             pred.buildQuery(builder, qcontext.expressions, args));
          if (!hadexpr) 
            qcontext.clauses.add(pclause);
        } else {
          // Predicate not an rdbms-tolog predicate, so we'll evaluate it with
          // basic-tolog instead.
          qcontext.clauses.add(pclause);
        }

      } else if (theClause instanceof OrClause) {
        OrClause clause = (OrClause) theClause;

        boolean broken = false;
        List exprs = new ArrayList();
        List alternatives = clause.getAlternatives();
        int len = alternatives.size();
        if (len == 1 || clause.getShortCircuit())
          // optional clause
          broken = true;
        else {
          // ordinary OR
          for (int i = 0; !broken && i < len; i++) {
            QueryContext _qcontext = compile(builder, (List) alternatives
                .get(i));

            if (_qcontext.hasClauses())
              broken = true;
            else
              exprs.add(new JDOAnd(_qcontext.expressions));
          }
        }

        // Map expression
        if (broken)
          qcontext.clauses.add(clause);
        else
          qcontext.expressions.add(new JDOOr(exprs));

      } else if (theClause instanceof NotClause) {
        NotClause clause = (NotClause) theClause;

        // TODO: Variables should be local to the JDONot in this
        // case. Unfortunately they get lost at this time, because the
        // query builder used is discarded.
        //
        // Variables have to be put onto a stack, so that one can
        // restrict their "validity".

        // Compile NOT clause
        QueryContext _qcontext = compile(builder, clause.getClauses());

        // Map expression
        if (!_qcontext.hasClauses() && _qcontext.hasExpressions())
          qcontext.expressions
              .add(new JDONot(new JDOAnd(_qcontext.expressions)));
        else
          qcontext.clauses.add(clause);

      } else
        throw new OntopiaRuntimeException("Unknown clause type:"
            + theClause.getClass());

    }

    // If qcontext.expressions contains only non-independent clauses
    // (e.g. NOTs) then let basic processor process them.
    int size = qcontext.expressions.size();
    if (size > 0) {
      boolean indeps = false;
      for (int i = 0; i < size; i++) {
        JDOExpressionIF expr = (JDOExpressionIF) qcontext.expressions.get(i);
        if (isIndependent(expr)) {
          indeps = true;
          break;
        }
      }
      if (!indeps) {
        qcontext.clauses = clauses;
        qcontext.expressions.clear();
      }
    }

    return qcontext;
  }

  protected boolean isIndependent(JDOExpressionIF expr) {
    // Note: This is a crude way of avoiding queries with too few
    // filtering expressions. It will also avoid the case when the query
    // only contains JDONots.

    switch (expr.getType()) {
    case JDOExpressionIF.AND: {
      JDOExpressionIF[] exprs = ((JDOAnd) expr).getExpressions();
      for (int i = 0; i < exprs.length; i++) {
        if (isIndependent(exprs[i]))
          return true;
        ;
      }
      return false;
    }
    case JDOExpressionIF.OR: {
      JDOExpressionIF[] exprs = ((JDOOr) expr).getExpressions();
      for (int i = 0; i < exprs.length; i++) {
        if (isIndependent(exprs[i]))
          return true;
        ;
      }
    }
    // case JDOExpressionIF.NOT_EQUAL:
    case JDOExpressionIF.NOT: {
      return false;
      // ! return isIndependent(((JDONot)expr).getExpression());
    }
    }
    // All other expressions are independent.
    return true;
  }

  protected boolean isOrderableTypes(TologQuery query) {
    List oblist = query.getOrderBy();
    int size = oblist.size();
    for (int i = 0; i < size; i++) {
      Variable var = (Variable) oblist.get(i);
      String varname = var.getName();
      Object[] vartypes = (Object[]) query.getVariableTypes().get(varname);
      for (int x = 0; x < vartypes.length; x++) {
        if (net.ontopia.topicmaps.core.TopicIF.class.equals(vartypes[x])
            && !query.getCountedVariables().contains(var))
          return false;
      }
    }
    return true;
  }

  // / java.lang.Object implementation

  public String toString() {
    return query.toString();
  }

}
