package org.atmosphere.samples.wicket;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.time.Duration;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.Meteor;
import org.atmosphere.util.LoggerUtils;

public class CometResource<Task> implements IResource, AtmosphereResourceEventListener {

	private static final long serialVersionUID = 1L;

	private final AtomicBoolean scheduleStarted = new AtomicBoolean(false);
	
	private final Callable<Task> task;
	
	private final Duration duration;
	
	public CometResource(final Callable<Task> task, final Duration duration) {
		this.task = task;
		this.duration = duration;
	}
	
	@Override
	public void respond(Attributes attributes) {

		ServletWebRequest request = (ServletWebRequest) attributes.getRequest();
		
		// Grap a Meteor
        Meteor meteor = Meteor.build(request.getHttpServletRequest());

        // Start scheduling update.
        if (!scheduleStarted.getAndSet(true)) {
            meteor.schedule(task, new Double(duration.seconds()).longValue());
        }

        // Add us to the listener list.
        meteor.addListener(this);

        // Depending on the connection
        String transport = request.getHeader("X-Atmosphere-Transport");

        // Suspend the connection. Could be long-polling, streaming or websocket.
        meteor.suspend(-1, !(transport != null && transport.equalsIgnoreCase("long-polling")));
	}

	public void onBroadcast(
			AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
		LoggerUtils.getLogger().log(Level.INFO,
				"onBroadcast: " + event.getMessage());

		// If we are using long-polling, resume the connection as soon as we get
		// an event.
		String transport = event.getResource().getRequest()
				.getHeader("X-Atmosphere-Transport");
		if (transport != null && transport.equalsIgnoreCase("long-polling")) {
			Meteor meteor = Meteor.lookup(event.getResource().getRequest());

			meteor.removeListener(this);
			meteor.resume();
		}
	}

	public void onSuspend(
			AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
		String transport = event.getResource().getRequest()
				.getHeader("X-Atmosphere-Transport");
		HttpServletRequest req = event.getResource().getRequest();
		LoggerUtils.getLogger().log(
				Level.INFO,
				String.format("Suspending the %s response from ip %s:%s",
						transport == null ? "websocket" : transport,
						req.getRemoteAddr(), req.getRemotePort()));
	}

	public void onResume(
			AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
		String transport = event.getResource().getRequest()
				.getHeader("X-Atmosphere-Transport");
		HttpServletRequest req = event.getResource().getRequest();
		LoggerUtils.getLogger().log(
				Level.INFO,
				String.format("Resuming the %s response from ip %s:%s",
						transport == null ? "websocket" : transport,
						req.getRemoteAddr(), req.getRemotePort()));
	}

	public void onDisconnect(
			AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
		String transport = event.getResource().getRequest()
				.getHeader("X-Atmosphere-Transport");
		HttpServletRequest req = event.getResource().getRequest();
		LoggerUtils.getLogger().log(
				Level.INFO,
				String.format("%s connection dropped from ip %s:%s",
						transport == null ? "websocket" : transport,
						req.getRemoteAddr(), req.getRemotePort()));
	}

	@Override
	public void onThrowable(AtmosphereResourceEvent<HttpServletRequest, HttpServletResponse> event) {
		event.throwable().printStackTrace();
	}

}
