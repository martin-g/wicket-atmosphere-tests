package org.atmosphere.samples.wicket;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.SharedResources;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.time.Duration;

@SuppressWarnings("serial")
public abstract class AtmosphereCometBehavior<Task> extends AbstractBehavior {
	
	private final AtomicBoolean COMET_IFRAME_RENDERED = new AtomicBoolean(false);
	
	private Component component;
	
	protected abstract Callable<Task> getTask();
	
	protected abstract Duration getInterval();
	
	/* (non-Javadoc)
	 * @see org.apache.wicket.behavior.AbstractBehavior#bind(org.apache.wicket.Component)
	 */
	@Override
	public void bind(Component component) {
		super.bind(component);
		
		this.component = component;
		this.component.setOutputMarkupId(true);
	}

	/* (non-Javadoc)
	 * @see org.apache.wicket.behavior.AbstractBehavior#renderHead(org.apache.wicket.markup.html.IHeaderResponse)
	 */
	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		
		response.renderJavascriptReference("http://github.com/Atmosphere/atmosphere/raw/master/modules/jquery/src/main/webapp/jquery/jquery-1.4.2.js");
		response.renderJavascriptReference("http://github.com/Atmosphere/atmosphere/raw/master/modules/jquery/src/main/webapp/jquery/jquery.atmosphere.js");
		String markupId = component.getMarkupId();
		
		final String resourceName = "comet_" + markupId;
		
		SharedResources sharedResources = Application.get().getSharedResources();
		sharedResources.add(resourceName, new CometResource<Task>(getTask(), getInterval()));
		CharSequence callbackUrl = component.urlFor(new SharedResourceReference(resourceName), null);
		
		response.renderJavascript("var COMET_CALLBACK_URL_" + markupId + "= '" + callbackUrl + "';", "atmosphere.comet.callbackUrl."+markupId);
		
		PackagedTextTemplate atmosphereTextTemplate = new PackagedTextTemplate(AtmosphereCometBehavior.class, "AtmosphereTextTemplate.js");
		MiniMap<String, String> variables = new MiniMap<String, String>(2);
		variables.put("callbackUrl", callbackUrl.toString());
		variables.put("componentId", component.getMarkupId());
		String atmosphereJs = atmosphereTextTemplate.asString(variables);
		response.renderJavascript(atmosphereJs, "atmosphere.comet."+markupId);
	}

	/* (non-Javadoc)
	 * @see org.apache.wicket.behavior.AbstractBehavior#onRendered(org.apache.wicket.Component)
	 */
	@Override
	public void onRendered(Component component) {
		super.onRendered(component);
		
		if (COMET_IFRAME_RENDERED.compareAndSet(false, true)) {
			// render the <iframe> used by jQuery.atmosphere
			Response response = component.getResponse();
			response.write("<div><iframe id=\"cometFrame\" name=\"cometFrame\" width=\"0\" height=\"0\" border=\"0\" style=\"border-width: 0px\"></iframe></div>");
		}
	}

}
