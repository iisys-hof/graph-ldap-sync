package de.hofuniversity.iisys.ldapsync;

import java.util.GregorianCalendar;
import java.util.List;

import javax.naming.NamingEnumeration;

import de.hofuniversity.iisys.ldapsync.config.SyncConfig;
import de.hofuniversity.iisys.ldapsync.endpoints.ISyncEndpoint;

/**
 * Scheduler that initiates the synchronization process on a regular basis as
 * configured or when forced. It opens a connection before synchronizing and
 * closes it afterwards.
 * 
 * @author fholzschuher2
 * 
 */
public class SyncScheduler implements Runnable
{
    private static final long TIME_THRESHOLD = 60000;

    private final Object fTrigger;

    private final SyncConfig fConfig;
    private final ILdapConnector fLdap;
    private final ISyncEndpointFactory fFactory;
    private final LdapBuffer fBuffer;

    private long fWaitTime, fNextSync;

    private List<ISyncEndpoint> fEndPoints;

    private boolean fRunning;
    private boolean fForceSync;

    /**
     * Creates a synchronization scheduler that synchronizes according to the
     * given configuration, controlling the given connector, handling the end
     * points from the given factory. Throws a NullPointerException if any
     * parameter is null.
     * 
     * @param config
     *            configuration object to use
     * @param ldap
     *            LDAP connector to control
     * @param factory
     *            factory that delivers end points
     * @param buffer
     *            buffer whose changes to write
     */
    public SyncScheduler(SyncConfig config, ILdapConnector ldap,
        ISyncEndpointFactory factory, LdapBuffer buffer)
    {
        if (config == null)
        {
            throw new NullPointerException("configuration was null");
        }
        if (ldap == null)
        {
            throw new NullPointerException("ldap connector was null");
        }
        if (factory == null)
        {
            throw new NullPointerException("end point factory was null");
        }
        if (buffer == null)
        {
            throw new NullPointerException("ldap buffer was null");
        }

        fTrigger = new Object();
        fConfig = config;
        fLdap = ldap;
        fFactory = factory;
        fBuffer = buffer;

        fForceSync = fConfig.getSyncOnStart();
        fEndPoints = fFactory.createEndpoints();
    }

    public void run()
    {
        fRunning = true;

        while (fRunning)
        {
            // execute synchronizations if forced or time is correct enough
            if (fForceSync || System.currentTimeMillis() >= fNextSync
                || System.currentTimeMillis() - fNextSync < TIME_THRESHOLD)
            {
                fForceSync = false;

                try
                {
                    sync();
                } catch (Exception e)
                {
                    e.printStackTrace();

                    // stop?
                    // fRunning = false;
                }
            }

            // stop if there was an interrupt
            if (!fRunning)
            {
                return;
            }

            // compute time to next cycle
            computeTime();

            try
            {
                synchronized (fTrigger)
                {
                    fTrigger.wait(fWaitTime);
                }
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void sync() throws Exception
    {
        // get current data from LDAP
        System.out.println("connecting to LDAP");
        fLdap.connect();

        // refresh data
        System.out.print("getting users from LDAP");
        long time = System.currentTimeMillis();
        NamingEnumeration data = fLdap.query("", "uid=*");
        fBuffer.setData(data);
        time = System.currentTimeMillis() - time;
        System.out.println(" (" + time + " ms)");

        // refresh all end points
        System.out.println("synchronizing with end points");
        int count = 1;
        for (ISyncEndpoint endPoint : fEndPoints)
        {
            try
            {
                System.out.print("end point " + count + " ...");
                time = System.currentTimeMillis();
                endPoint.sync();
                time = System.currentTimeMillis() - time;
                System.out.println(" done (" + time + " ms)");
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        // write changes in the buffer if there are any
        if (fBuffer.hasChanges())
        {
            System.out.print("writing changes to LDAP ...");
            time = System.currentTimeMillis();
            fBuffer.writeToLdap();
            time = System.currentTimeMillis() - time;
            System.out.println(" done (" + time + " ms)");
        } else
        {
            System.out.println("no changes");
        }

        // discard LDAP connection
        System.out.println("disconnecting from LDAP");
        fLdap.disconnect();
    }

    private void computeTime()
    {
        GregorianCalendar cal = new GregorianCalendar();
        String timeString = fConfig.getTime();
        int hour = cal.get(GregorianCalendar.HOUR_OF_DAY);
        int minute = cal.get(GregorianCalendar.MINUTE);
        int day = cal.get(GregorianCalendar.DAY_OF_WEEK);
        int week = cal.get(GregorianCalendar.WEEK_OF_YEAR);
        int month = cal.get(GregorianCalendar.MONTH);

        if (timeString != null && !timeString.isEmpty())
        {
            String[] time = fConfig.getTime().split(":");
            hour = Integer.parseInt(time[0]);
            minute = Integer.parseInt(time[1]);
        }

        if (fConfig.getDay() > 0)
        {
            day = fConfig.getDay();
        }

        // TODO: short months, invalid times and days

        switch (fConfig.getInterval())
        {
        // next hour, specified or same minute
            case HOURLY:
                cal.set(GregorianCalendar.MINUTE, minute);
                cal.set(GregorianCalendar.HOUR_OF_DAY, hour + 1);
                fNextSync = cal.getTimeInMillis();
                fWaitTime = fNextSync - System.currentTimeMillis();
                break;

            // next day, specified or same time
            case DAILY:
                cal.set(GregorianCalendar.MINUTE, minute);
                cal.set(GregorianCalendar.HOUR_OF_DAY, hour);
                cal.set(GregorianCalendar.DAY_OF_YEAR, day + 1);
                fNextSync = cal.getTimeInMillis();
                fWaitTime = fNextSync - System.currentTimeMillis();
                break;

            // next week, specified or same time and day of week
            case WEEKLY:
                cal.set(GregorianCalendar.MINUTE, minute);
                cal.set(GregorianCalendar.HOUR_OF_DAY, hour);
                cal.set(GregorianCalendar.DAY_OF_WEEK, day);
                cal.set(GregorianCalendar.WEEK_OF_YEAR, week + 1);
                fNextSync = cal.getTimeInMillis();
                fWaitTime = fNextSync - System.currentTimeMillis();
                break;

            // next month, specified or same time and day of month
            case MONTHLY:
                cal.set(GregorianCalendar.MINUTE, minute);
                cal.set(GregorianCalendar.HOUR_OF_DAY, hour);
                cal.set(GregorianCalendar.DAY_OF_MONTH, day);
                cal.set(GregorianCalendar.MONTH, month + 1);
                fNextSync = cal.getTimeInMillis();
                fWaitTime = fNextSync - System.currentTimeMillis();
                break;

            // synchronization can only be triggered manually
            case MANUAL:
                fWaitTime = Long.MAX_VALUE;
                fNextSync = Long.MAX_VALUE;
                break;
        }
    }

    /**
     * @return whether the scheduler is currently active
     */
    public boolean isRunning()
    {
        return fRunning;
    }

    /**
     * Stops the scheduler if it is running.
     */
    public void stop()
    {
        fRunning = false;

        synchronized (fTrigger)
        {
            fTrigger.notify();
        }
    }

    /**
     * Causes the scheduler to perform an unscheduled synchronization.
     */
    public void forceSync()
    {
        fForceSync = true;

        synchronized (fTrigger)
        {
            fTrigger.notify();
        }
    }
}
