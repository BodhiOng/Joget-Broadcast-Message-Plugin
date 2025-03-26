package org.joget.dx82.sample;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(SampleHeaderFilter.class.getName(), new SampleHeaderFilter(), null));
        registrationList.add(context.registerService(SampleWebFilterPlugin.class.getName(), new SampleWebFilterPlugin(), null));
        registrationList.add(context.registerService(SampleChatUiHtmlInjector.class.getName(), new SampleChatUiHtmlInjector(), null));
        registrationList.add(context.registerService(SampleLogUrlUiHtmlInjector.class.getName(), new SampleLogUrlUiHtmlInjector(), null));
        registrationList.add(context.registerService(SampleConsolePagePlugin.class.getName(), new SampleConsolePagePlugin(), null));
        registrationList.add(context.registerService(SampleLoginFormEncryption.class.getName(), new SampleLoginFormEncryption(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}