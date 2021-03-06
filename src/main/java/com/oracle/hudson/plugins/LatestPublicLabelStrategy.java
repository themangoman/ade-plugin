package com.oracle.hudson.plugins;

import hudson.Launcher;
import hudson.Proc;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 * We need a "strategy" for determining the latest public label.  This should try to avoid returning labels that are
 * in the progress of being built.  In this implementation, we fall back to using the "-latest" option with the
 * appropriate warning.
 * 
 * @author tagarwal
 *
 */
class LatestPublicLabelStrategy {
	
	String[] getCommand(
		@SuppressWarnings("rawtypes") AbstractBuild build,
		Launcher launcher,
		BuildListener listener,
		AdeViewLauncherDecorator ade) {
		
		try {
			return new String[] {
					"ade",
					"createview",
					"-force",
					"-label",
					getLatestPublicLabel(launcher, listener, build, ade),
					ade.getViewName(build)};
		} catch (Exception e) {
			// fall back to the -latest strategy
			return new String[] {
					"ade",
					"createview",
					"-force",
					"-latest",
					"-series",
					ade.getSeries(),
					ade.getViewName(build)};
		}
	}

	/*
	 * when choosing the latest label, use the -public option of 
	 * showlabels to determine which label to use.  This prevents the issue
	 * where some users can see labels while they're still being built.
	 * 
	 * added by tagarwal
	 */
	private String getLatestPublicLabel(Launcher launcher, BuildListener listener, AbstractBuild build, AdeViewLauncherDecorator ade)
			throws IOException, InterruptedException {
		//first try to figure out what is the latest label to which we can refresh
		String[] latestLabelsCmds = new String[] {"ade","showlabels","-series",ade.getSeries(),"-latest","-public"};

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		Proc proc1 = launcher.launch().cmds(latestLabelsCmds).stdout(out).start();

		proc1.join();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		//only interested in the last line
		String latestPublicLabel = null, tmp;
		while ((tmp = br.readLine()) != null){
			latestPublicLabel = tmp;
		}

		listener.getLogger().println("The latest public label is " + latestPublicLabel);
		
		if (!latestPublicLabel.matches(ade.getSeries() + "_[0-9]*\\.[0-9]*.*")){
			launcher.kill(ade.getEnvOverrides(build));
		}
		return latestPublicLabel;
	}
}
