
package net.ontopia.topicmaps.nav2.core;

import java.net.URL;
import net.ontopia.topicmaps.entry.TopicMapRepositoryIF;
import net.ontopia.topicmaps.entry.UserStoreRegistry;
import net.ontopia.topicmaps.core.TopicMapIF;
import net.ontopia.topicmaps.nav2.core.NavigatorRuntimeException;

/**
 * INTERNAL: Implemented by an object which stores all information and
 * also all handles to configuration holders needed by the
 * navigator web application.
 */
public interface NavigatorApplicationIF {
  
  // -----------------------------------------------------------------------
  // constants defining attribute names for usage
  // within a (request|session|application) Scope of a JSP
  // -----------------------------------------------------------------------

  /**
   * INTERNAL: The application scope attribute under which our main
   * configuration object for the navigator Application is stored.
   *
   * @see net.ontopia.topicmaps.nav2.impl.basic.NavigatorApplication
   */
  public static final String NAV_APP_KEY = "ontopiaNavigatorApplication";

  /**
   * INTERNAL: The request scope attribute under which the root
   * context tag object of the JSP is stored.
   *
   * @see net.ontopia.topicmaps.nav2.taglibs.logic.ContextTag
   */
  public static final String CONTEXT_KEY = "ontopiaContext";

  /**
   * INTERNAL: The session scope attribute for storing information
   * about the user preferences (ie. Model-View-Skin). Note: This is
   * somewhat framework related.
   *
   * @see net.ontopia.topicmaps.nav2.core.UserIF
   */
  public static final String USER_KEY = "ontopiaUser";
  
  // -----------------------------------------------------------------------
  // constants defining attribute names specific inside a
  // lexical scope of a page context
  // -----------------------------------------------------------------------

  /**
   * INTERNAL: Variable name which is used/setup inside a foreach-tag
   * for describing if it is the first iteration we are in.
   */
  public static final String FOREACH_SEQ_FIRST_KEY = "sequence-first";

  /**
   * INTERNAL: Variable name which is used/setup inside a foreach-tag
   * for describing if it is the last iteration we are in.
   */
  public static final String FOREACH_SEQ_LAST_KEY = "sequence-last";

  /**
   * INTERNAL: Variable name which is used/setup inside a foreach-tag
   * for describing the index number iterated currently.
   */
  public static final String FOREACH_SEQ_INDEX_KEY = "sequence-index";

  
  // -----------------------------------------------------------------------
  // constants useful for retrieving/setting up the configuration
  // -----------------------------------------------------------------------

  /**
   * INTERNAL: The context parameter name for the Log4J configuration
   * file which value can be specified in <code>web.xml</code>.
   */
  public static final String LOG4J_CONFIG_KEY = "log4j_config";

  /**
   * INTERNAL: Default value for the Log4J configuration filename.
   * <code>WEB-INF/config/log4j.properties</code>
   */
  public static final String LOG4J_CONFIG_DEFAULT_VALUE =
    "WEB-INF/config/log4j.properties";

  /**
   * INTERNAL: The context parameter name for the Application
   * configuration file which value can be specified in
   * <code>web.xml</code>.
   */
  public static final String APP_CONFIG_KEY = "app_config";

  /**
   * INTERNAL: Default value for the Application configuration
   * filename. <code>WEB-INF/config/application.xml</code>
   */
  public static final String APP_CONFIG_DEFAULT_VALUE =
    "WEB-INF/config/application.xml";

  /**
   * INTERNAL: The context parameter name for the TopicMap Sources
   * configuration file which value can be specified in
   * <code>web.xml</code>.
   */
  public static final String SOURCE_CONFIG_KEY = "source_config";

  /**
   * INTERNAL: The id of the topic maps repository to use..
   */
  public static final String TOPICMAPS_REPOSITORY_ID = "topicmaps_repository_id";

  /**
   * INTERNAL: Default value for the TopicMap Sources configuration
   * filename. <code>WEB-INF/config/tm-sources.xml</code>
   */
  public static final String SOURCE_CONFIG_DEFAULT_VALUE =
    "WEB-INF/config/tm-sources.xml";

  /**
   * INTERNAL: The context parameter name for the directory containing
   * all plugins, which value can be specified in
   * <code>web.xml</code>.  <p> Note: If there is no value specified
   * then the application will not fallback to a default value, but
   * use no plugins at all.
   */
  public static final String PLUGINS_ROOTDIR_KEY = "plugins_rootdir";

  // -----------------------------------------------------------------------
  // Attributes for controlling the access rights to the shared store 
  // registry.
  // -----------------------------------------------------------------------

  /**
   * INTERNAL: The context parameter name (see <code>web.xml</code>)
   * for specifying the JNDI repository name, if the TopicMap Sources
   * should be shared between applications running on the same
   * server.<p>
   *
   * @since 1.3.2
   */
  public static final String JNDI_REPOSITORY_KEY = "jndi_repository";


  // -----------------------------------------------------------------------
  // methods that have to be implemented
  // -----------------------------------------------------------------------

  /**
   * INTERNAL: Gets the name of this application.
   *
   * @since 1.3
   */
  public String getName();
  
  /**
   * INTERNAL: Get <code>NavigatorConfigurationIF</code> object which allows
   * access to all application relevant configuration information.
   */
  public NavigatorConfigurationIF getConfiguration();

  /**
   * INTERNAL: Get an object instance of specified class name
   * centralized managed by this instance (application).
   *
   * @param classname String which can be a shortcut for a
   *        full qualified class name (FQCN) specified in the
   *        classmap of <code>NavigatorConfigurationIF</code>
   *        If no entry is found the classname is interpreted
   *        as FQCN.
   *
   * @return An object instance of given class.
   *
   * @see net.ontopia.topicmaps.nav2.core.NavigatorConfigurationIF
   */
  public Object getInstanceOf(String classname)
    throws NavigatorRuntimeException;

  /**
   * INTERNAL: Returns the topic map repository used by the navigator
   * application.
   *
   * @since 2.1
   */
  public TopicMapRepositoryIF getTopicMapRepository();

  /**
   * INTERNAL: Refresh topic map registry.
   */
  public void refreshTopicMapRegistry();

  /**
   * INTERNAL: Refresh application configuration.
   */
  public void refreshAppConfig();

  /**
   * INTERNAL: Get <code>TopicMapIF</code> object for specified TopicMap
   * String Identifier which is in accordance to the identifiers used
   * by the TopicMapRepositoryIF.
   */
  public TopicMapIF getTopicMapById(String topicmapId)
    throws NavigatorRuntimeException;

  /**
   * INTERNAL: Get <code>TopicMapIF</code> object for specified TopicMap
   * String Identifier which is in accordance to the identifiers used
   * by the TopicMapRepositoryIF.
   *
   * @since 2.1
   */
  public TopicMapIF getTopicMapById(String topicmapId, boolean readonly)
    throws NavigatorRuntimeException;
  
  /**
   * INTERNAL: Returns the <code>TopicMapIF</code> object to the
   * navigator application. Note that the topic map object must have
   * been retrieved from the navigator application for this to work
   * properly.
   *
   * @since 2.0.7
   */
  public void returnTopicMap(TopicMapIF topicmap);
  
  /**
   * INTERNAL: Get <code>UserStoreRegistry</code> object which allows
   * access to all available topicmap sources, whether they are
   * already loaded or have to be loaded (managed automatically). The
   * transaction user is bound internally by the navigator
   * application.<p>
   *
   * @since 1.3.4
   * @deprecated
   */
  public UserStoreRegistry getUserStoreRegistry();

  /**
   * INTERNAL: Get the reference ID of the topic map within the
   * application's store registry.<p>
   *
   * @since 1.3.3
   */
  public String getTopicMapRefId(TopicMapIF topicmap);
  
  /**
   * INTERNAL: Return <code>ModuleIF</code> object for specified
   * resource location (given as URL) of module. If the module cannot
   * be found in internal object pool a new instance is created.
   */
  public ModuleIF getModule(URL location)
    throws NavigatorRuntimeException;
  
  /**
   * INTERNAL: Close navigator application and free all resources
   * connected to it.
   *
   * @since 2.1
   */
  public void close();

}
