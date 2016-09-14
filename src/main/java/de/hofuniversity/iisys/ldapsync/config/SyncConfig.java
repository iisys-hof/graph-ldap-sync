package de.hofuniversity.iisys.ldapsync.config;

import java.util.List;

/**
 * Class containing configuration information for the LDAP synchronization and
 * manipulation process.
 * 
 * @author fholzschuher2
 * 
 */
public class SyncConfig
{
    // access
    private String fUrl;
    private String fContext;
    private String fUser;
    private char[] fPassword;
    private boolean fReadOnly = true;
    private boolean fSubtreeSearch = true;

    // synchronization
    private boolean fSyncOnStart;
    private CycleTypes fInterval;
    private String fTime;
    private int fDay;

    // initial values
    private List<String> fInitialClasses;
    private List<String> fInitialOus;

    // end points
    private List<SyncEndpointConfig> fEndpoints;

    /**
     * @return LDAP URL to connect to
     */
    public String getUrl()
    {
        return fUrl;
    }

    /**
     * @return context under which users are stored
     */
    public String getContext()
    {
        return fContext;
    }

    /**
     * @return user name to authenticate with or null
     */
    public String getUser()
    {
        return fUser;
    }

    /**
     * @return password to authenticate with or null
     */
    public char[] getPassword()
    {
        return fPassword;
    }

    /**
     * @return whether to sync with LDAP on startup
     */
    public boolean getSyncOnStart()
    {
        return fSyncOnStart;
    }

    /**
     * @return general synchronization interval
     */
    public CycleTypes getInterval()
    {
        return fInterval;
    }

    /**
     * @return configuration for end points to synchronize
     */
    public List<SyncEndpointConfig> getEndpoints()
    {
        return fEndpoints;
    }

    /**
     * @param url
     *            LDAP URL to connect to
     */
    public void setUrl(String url)
    {
        fUrl = url;
    }

    /**
     * @param context
     *            context under which users are stored
     */
    public void setContext(String context)
    {
        fContext = context;
    }

    /**
     * @param user
     *            user name to authenticate with or null
     */
    public void setUser(String user)
    {
        fUser = user;
    }

    /**
     * @param password
     *            password to authenticate with
     */
    public void setPassword(char[] password)
    {
        fPassword = password;
    }

    /**
     * @param whether
     *            to sync with LDAP on startup
     */
    public void setSyncOnStart(boolean syncOnStart)
    {
        fSyncOnStart = syncOnStart;
    }

    /**
     * @param interval
     *            general synchronization interval
     */
    public void setInterval(CycleTypes interval)
    {
        fInterval = interval;
    }

    /**
     * @return configuration for end points to synchronize
     */
    public void setEndpoints(List<SyncEndpointConfig> endpoints)
    {
        fEndpoints = endpoints;
    }

    /**
     * @return time of the day at which to sync as HH:MM
     */
    public String getTime()
    {
        return fTime;
    }

    /**
     * @return day of the week (Sunday is 1) or month
     */
    public int getDay()
    {
        return fDay;
    }

    /**
     * @param time
     *            time of the day at which to sync as HH:MM
     */
    public void setTime(String time)
    {
        fTime = time;
    }

    /**
     * @param day
     *            day of the week (Sunday is 1) or month
     */
    public void setDay(int day)
    {
        fDay = day;
    }

    /**
     * @return whether the LDAP service should not be written to (default:true)
     */
    public boolean getReadOnly()
    {
        return fReadOnly;
    }

    /**
     * @param fReadOnly
     *            whether the LDAP service should not be written to
     */
    public void setReadOnly(boolean readOnly)
    {
        fReadOnly = readOnly;
    }

    /**
     * @return whether an ldap subtree search for users should be performed
     */
    public boolean getSubtreeSearch()
    {
        return fSubtreeSearch;
    }
    
    /**
     * @param subtreeSearch whether an ldap subtree search for users should be performed
     */
    public void setSubtreeSearch(boolean subtreeSearch)
    {
        fSubtreeSearch = subtreeSearch;
    }
    
    /**
     * @return initial object classes for user objects
     */
    public List<String> getInitialClasses()
    {
        return fInitialClasses;
    }

    /**
     * @param initialClasses
     *            initial object classes for user objects
     */
    public void setInitialClasses(List<String> initialClasses)
    {
        fInitialClasses = initialClasses;
    }

    /**
     * @return initial organizational units for user objects
     */
    public List<String> getInitialOus()
    {
        return fInitialOus;
    }

    /**
     * @param initialOus
     *            initial organizational units for user objects
     */
    public void setInitialOus(List<String> initialOus)
    {
        fInitialOus = initialOus;
    }
}
