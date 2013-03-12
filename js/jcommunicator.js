cons = document.getElementById("console");
cons.innerHTML += "script started<br />";
var head = document.getElementsByTagName('head')[0];

function sendRequest(server, methode, callback, params) {
	log("sendRequest("+server+", "+methode+", "+callback+", "+params+")");
	script = document.createElement("script");
	script.setAttribute("type", "text/javascript");
    var id = Math.floor((new Date()).getTime() / Math.random());
    log("generated id: " + id);
	var apiSrc = "http://" + server + ":9018/" + methode + "?callback=" + callback;
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