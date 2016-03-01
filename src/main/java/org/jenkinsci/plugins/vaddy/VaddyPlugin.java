package org.jenkinsci.plugins.vaddy;

import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

public class VaddyPlugin
extends Builder
{
private final String host;
private final String userId;
private final String authKey;
private final String crawlId;
private final String apiServerUrl;
private final String proxyHost;
private final String proxyPort;

// Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
@DataBoundConstructor
public VaddyPlugin( String host, String userId, String authKey, String crawlId, String apiServerUrl, String proxyHost, String proxyPort )
{
this.host = host;
this.userId = userId;
this.authKey = authKey;
this.crawlId = crawlId;
this.apiServerUrl = apiServerUrl;
this.proxyHost = proxyHost;
this.proxyPort = proxyPort;
}
//--------------------------------------------------------------------------------
@Override
public boolean perform( AbstractBuild build, Launcher launcher, BuildListener listener )
{
MPluginImpl impl = new MPluginImpl( host, userId, authKey, crawlId, apiServerUrl, proxyHost, proxyPort );
impl.setLogStream( listener.getLogger() );
if( impl.execute() )
	{
	return true;
	}
else
	{
	return false;
	}
}
//--------------------------------------------------------------------------------
@Override
public DescriptorImpl getDescriptor()
{
return ( DescriptorImpl )super.getDescriptor();
}
//--------------------------------------------------------------------------------
@Extension // This indicates to Jenkins that this is an implementation of an extension point.
public static final class DescriptorImpl
extends BuildStepDescriptor< Builder >
{
public DescriptorImpl()
{
load();
}
//--------------------------------------------------------------------------------
public boolean isApplicable( Class<? extends AbstractProject> aClass )
{
return true;
}
//--------------------------------------------------------------------------------
public String getDisplayName()
{
return "Vaddy";
}
//--------------------------------------------------------------------------------
@Override
public boolean configure( StaplerRequest req, JSONObject formData )
throws FormException
{
save();
return super.configure( req, formData );
}
//--------------------------------------------------------------------------------
}

public String getHost()
{

return host;
}

public String getAuthKey()
{

return authKey;
}

public String getApiServerUrl()
{

return apiServerUrl;
}

public String getProxyHost()
{

return proxyHost;
}

public String getProxyPort()
{

return proxyPort;
}

public String getUserId()
{

return userId;
}

public String getCrawlId()
{

return crawlId;
}

}


