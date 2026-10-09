package com.foundations.soil;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public final class OfflineTestRunner {
    public static void main(String[] args) {
        var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();
        var request=LauncherDiscoveryRequestBuilder.request().selectors(DiscoverySelectors.selectPackage("com.foundations.soil"))
            .filters(org.junit.platform.launcher.TagFilter.excludeTags("tfc-runtime"))
            .configurationParameter("junit.jupiter.extensions.autodetection.enabled","true").build();
        launcher.registerTestExecutionListeners(listener);launcher.execute(request);
        listener.getSummary().printTo(new java.io.PrintWriter(System.out,true));
        listener.getSummary().printFailuresTo(new java.io.PrintWriter(System.out,true));
        // Minecraft utility workers may outlive standalone tests; the Gradle worker has its own lifecycle.
        System.exit(listener.getSummary().getTestsFoundCount()==0||listener.getSummary().getTotalFailureCount()>0 ? 1 : 0);
    }
}
