package org.atmosphere.samples;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.Meteor;
import org.atmosphere.util.LoggerUtils;

/**
 * The servlet that will serve endless response
 * 
 * Suspend the response using the {@link Meteor} API.
 * 
 * @author Andrey Belyaev
 * @author Jeanfrancois Arcand
 */
@SuppressWarnings("serial")
public class CometServlet extends HttpServlet implements AtmosphereResourceEventListener {

	private final AtomicBoolean scheduleStarted = new AtomicBoolean(false);
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

        // Grap a Meteor
        Meteor meteor = Meteor.build(req);

        // Start scheduling update.
        if (!scheduleStarted.getAndSet(true)) {
            meteor.schedule(new TaskCallable(), 5); // One second
        }

        // Add us to the listener list.
        meteor.addListener(this);

        // Depending on the connection
        String transport = req.getHeader("X-Atmosphere-Transport");

        // Suspend the connection. Could be long-polling, streaming or websocket.
        meteor.suspend(-1, !(transport != null && transport.equalsIgnoreCase("long-polling")));

	}

	private static class Task {
		@Override
		public String toString() {
			String s = new Date().toString();
			return s;
		}
	}

	private static class TaskCallable implements Callable<String> {
		@Override
		public String call() {
			String s = new Date().toString();
			return s;
		}
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
	public void onThrowable(Throwable t) {
		t.printStackTrace();
	}

}
