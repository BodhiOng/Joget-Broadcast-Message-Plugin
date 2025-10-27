package org.joget.marketplace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import org.joget.commons.util.LogUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        // Register plugin here
        registrationList.add(context.registerService(BroadcastMessagePlugin.class.getName(), new BroadcastMessagePlugin(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
        
        // Stop the message check scheduler if it's running
        try {
            // Use reflection to access the static method
            Class<?> pluginClass = BroadcastMessagePlugin.class;
            Method stopSchedulerMethod = pluginClass.getDeclaredMethod("stopMessageCheckScheduler");
            stopSchedulerMethod.setAccessible(true);
            stopSchedulerMethod.invoke(null); // null because it's a static method
            LogUtil.info(getClass().getName(), "Successfully stopped message check scheduler during bundle deactivation");
        } catch (Exception e) {
            LogUtil.error(getClass().getName(), e, "Error stopping message check scheduler during bundle deactivation");
        }
    }
}