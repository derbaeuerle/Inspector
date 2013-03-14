var jc = {

	DEBUG: true,
	SERVER_PORT: '9018',

	appServer: null,
	head: null,

	/**
	 * Get JSON data from configured server and method. JSON object will be
	 * reached to the defined callback method.
	 * Possible properties in 'opt':
	 *		- server: IP or domain of Android application server.
	 *		- method (required): Method name to be called on server.
	 *		- callback (required): Javascript callback function with JSON object as parameter.
	 *		- params: JSON object (e.g. '{"name": "will", "age": 34}')
	 */
	sendRequest: function(opt) {
		if('method' in opt && 'callback' in opt) {
			var server = opt.server || jc.appServer;
			var method = opt.method;
			var callback = opt.callback;
			var params = opt.params || {};

			if(server == 'undefined' || server == null || server == "") {
				if(jc.DEBUG)
					alert("No valid server has been defined!");
				else {
					throw "No valid server has been defined!";
				}
			}
			script = document.createElement("script");
			script.setAttribute("type", "text/javascript");

		    var id = Math.floor((new Date()).getTime() / Math.random());
			var apiSrc = "http://" + server + ":" + jc.SERVER_PORT + "/" + method + "?callback=callback";

			if('params' in opt) {
				for(var key in params) {
			        if(params.hasOwnProperty(key)) {
			        	apiSrc += "&" + key + "=" + params[key];
			        }
			    }
			}

			log("url: " + apiSrc);
			
			script.setAttribute("src", apiSrc);
			script.setAttribute("id", "jc" + id);
			jc.head.appendChild(script);

			if (callback && typeof(callback) === "function") {  
		        callback(JSON.parse(JSON.stringify(json, undefined, 2)));  
		    }  
		} else {
			if(jc.DEBUG)
				alert("Your have to define 'method' and 'callback' in opt!");
			else {
				throw "Your have to define 'method' and 'callback' in opt!";
			}
		}
	},

	init: function(e) {
		head = document.getElementsByTagName("head")[0];
		if('appServer' in e) {
			jc.appServer = e.appServer;
		}
	}

};

com = jc.init({'appServer': 'http://localhost/'});

/*
cons = document.getElementById("console");
cons.innerHTML += "script started<br />";

function sendRequest(server, method, callback, params) {
	log("sendRequest("+server+", "+method+", "+callback+", "+params+")");
	script = document.createElement("script");
	script.setAttribute("type", "text/javascript");
    var id = Math.floor((new Date()).getTime() / Math.random());
    log("generated id: " + id);
	var apiSrc = "http://" + server + ":9018/" + method + "?callback=" + callback;
	for(var key in params) {
        if(params.hasOwnProperty(key)) {
        	apiSrc += "&" + key + "=" + params[key];
        }
    }
	log(params);
	log("url: " + apiSrc);
	script.setAttribute("src", apiSrc);
	script.setAttribute("id", "jc" + id);
	head.appendChild(script);
}

function testRequest() {
	log("testing ...");
	sendRequest("localhost", "time", "callback", {"test": "testvalue"});
	log("test ended");
}

function getSystemTime() {
	sendRequest("localhost", "time", "callback", {});
	log("loaded");
}

function getJsonData() {
	sendRequest("localhost", "data", "callback", {});
	log("loaded");
}

function callback(json) {
	log("callback");
	log(JSON.stringify(json, undefined, 2));
}

function log(message) {
	cons.innerHTML += message + "<br />";
}
*/