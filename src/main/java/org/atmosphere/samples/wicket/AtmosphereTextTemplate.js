$(document).ready(function() {
	function callback_${componentId}(response) {
		// Websocket events.
		$.atmosphere.log('info', ["response.state: " + response.state]);
		$.atmosphere.log('info', ["response.transport: " + response.transport]);
        
		if (response.transport != 'polling' && response.state != 'connected' && response.state != 'closed') {
			$.atmosphere.log('info', ["response.responseBody: " + response.responseBody]);
			if (response.status == 200) {
				var data = response.responseBody;
console.log(1, data);
				document.getElementById('clock').innerHTML = data;
			} else {
				$.atmosphere.log('error', ["status: " + response.status]);
			}
		}
	}

	// You can set websocket, streaming or long-polling here.
	$.atmosphere.subscribe('${callbackUrl}',
			callback_${componentId},
			$.atmosphere.request = {transport: 'websocket'});
});
