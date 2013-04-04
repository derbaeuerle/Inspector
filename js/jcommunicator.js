var $ = {

	DEBUG: true,
	SERVER_PORT: '80',

	appServer: null,
	head: null,
	cb: null,

	/**
	 * Get JSON data from configured server and method. JSON object will be
	 * reached to the defined callback method.
	 * 'opt' can be a string or object. String defines complete url to request
	 * and 'opt' as object can contain followin porperties:
	 *		- server: IP or domain of Android application server with leading protocoll.
	 *		- port: Server port.
	 *		- method (required): Method name to be called on server.
	 *		- callback (required): Javascript callback function with JSON object as parameter.
	 *		- params: JSON object (e.g. '{"name": "will", "age": 34}')
	 * @param opt
	 */
	sendRequest: function(opt) {
		script = document.createElement("script");
		script.setAttribute("type", "text/javascript");

		var id = Math.floor((new Date()).getTime() / Math.random());

		if(typeof(opt) === typeof({})) {
			if('method' in opt && 'callback' in opt) {
				var server = opt.server || $.appServer;
				var method = opt.method;
				var port = opt.port || null;
				var params = opt.params || {};
				$.cb = opt.callback;

				if(server == 'undefined' || server == null || server == "") {
					if($.DEBUG)
						alert("No valid server has been defined!");
					else {
						throw "No valid server has been defined!";
					}
				}
				var apiSrc = server;
				apiSrc = apiSrc + ((port != null) ? ":" + port : "") + method;

				$.loadJSON(apiSrc, params, opt.callback);

				/*
				if('params' in opt) {
					for(var key in params) {
				        if(params.hasOwnProperty(key)) {
				        	apiSrc += "&" + key + "=" + params[key];
				        }
				    }
				}*/
			} else {
				if($.DEBUG)
					alert("Your have to define 'method' and 'callback' in opt!");
				else {
					throw "Your have to define 'method' and 'callback' in opt!";
				}
			}
		} /*else if (typeof(opt) === typeof("")) {
			apiSrc = opt;
		}
		console.log("url: " + apiSrc);

		script.setAttribute("src", apiSrc);
		script.setAttribute("id", "$" + id);
		$.head.appendChild(script);*/
	},

	/**
	 * Load JSON data from url with params.
	 * 
	 * @param url
	 * @param params
	 * @param callback
	 */
	loadJSON: function(url, params, callback) {
		script = document.createElement("script");
		script.setAttribute("type", "text/javascript");
		console.log(url);
		console.log(params);
		console.log(callback);

		var id = Math.floor((new Date()).getTime() / Math.random());

		if(typeof(callback) === typeof($.loadJSON)) {
			if(callback.name === "") {
				callback.name = "tmp" + id;
				tmp = callback;
				callback = tempCallback.name;
			} else {
				callback = callback.name;
			}
		}

		if(url.indexOf("callback=") == -1) {
			url += ((url.indexOf("?") == -1) ? "?" : "&") + "callback=" + callback;
		}
		for(var key in params) {
	        if(params.hasOwnProperty(key)) {
	        	url += "&" + key + "=" + params[key];
	        }
	    }

		console.log("url: " + url);
		script.setAttribute("src", url);
		script.setAttribute("id", "$" + id);
		$.head.appendChild(script);
	},

	init: function(e) {
		$.head = document.getElementsByTagName("head")[0];
		if('appServer' in e) {
			$.appServer = e.appServer;
		}
		return this;
	}

};


var tmp = null;
function tempCallback(json) {
	if(tmp != null && typeof(tmp) === typeof(tempCallback)) {
		tmp(json);
	}
	tmp = null;
}

function defaultCallback(json) {
	$.cb(json);
}