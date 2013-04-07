var $ = {

	DEBUG: true,
	SERVER_PORT: '80',

	appServer: null,
	head: null,
	cb: null,
	timeout: null,
	timeoutIn: 4000,

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
			} else {
				if($.DEBUG)
					alert("Your have to define 'method' and 'callback' in opt!");
				else {
					throw "Your have to define 'method' and 'callback' in opt!";
				}
			}
		}
	},

	/**
	 * Load JSON data from url with params.
	 * 
	 * @param url
	 * @param params
	 * @param callback
	 */
	loadJSON: function(url, params, callback, onerror) {
		script = document.createElement("script");
		script.setAttribute("type", "text/javascript");

		var id = Math.floor((new Date()).getTime() / Math.random());

		var errorHandler = $.onError;

		if(typeof(callback) === typeof($.loadJSON)) {
			if(callback.name === "") {
				callback.name = "tmp" + id;
			}
			tmp = callback;
			callback = defaultCallback.name;
		}
		if(typeof(onerror) === typeof(errorHandler)) {
			if(onerror.name === "") {
				onerror.name = "onError" + id;
			}
			errorHandler = onerror;
		}

		if(url.indexOf("callback=") == -1) {
			url += ((url.indexOf("?") == -1) ? "?" : "&") + "callback=" + callback;
		}
		for(var key in params) {
	        if(params.hasOwnProperty(key)) {
	        	url += "&" + key + "=" + params[key];
	        }
	    }
		script.setAttribute("src", url);
		script.setAttribute("id", "$" + id);
		script.onerror = errorHandler;
		$.timeout = setTimeout($.onError, $.timeoutIn);
		$.head.appendChild(script);
	},

	onError: function(e) {
		clearTimeout($.timeout);
		if(e != undefined && e.srcElement != undefined) {
			throw "Couldn't load " + e.srcElement.src;
		}
	},

	init: function(e) {
		window.onerror = $.onError;
		$.head = document.getElementsByTagName("head")[0];
		if(e != undefined) {
			if('appServer' in e) {
				$.appServer = e.appServer;
			}
		}
		return this;
	}

};


var tmp = null;
function defaultCallback(json) {
	if(tmp != null && typeof(tmp) === typeof(defaultCallback)) {
		clearTimeout($.timeout);
		tmp(json);
	}
	tmp = null;
}