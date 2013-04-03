window.onload = function() {
	$.init({'appServer': 'http://gd.geobytes.com/'});
	/*document.getElementById("url").addEventListener('click', sendString, false);
	document.getElementById("obj").addEventListener('click', sendRequest, false);
	document.getElementById("input").addEventListener('click', sendInput, false);

	document.getElementById("time").addEventListener('click', getTime, false);
	document.getElementById("release").addEventListener('click', getRelease, false);
	document.getElementById("integer").addEventListener('click', getInteger, false);*/

	document.getElementById("play").addEventListener('click', play, false);
	document.getElementById("pause").addEventListener('click', pause, false);
	document.getElementById("stop").addEventListener('click', stop, false);
}

function play() {
	$.loadJSON("http://localhost:9018/audio/play", {}, function(json) {
		log(json);
	});
}

function pause() {
	$.loadJSON("http://localhost:9018/audio/pause", {}, function(json) {
		log(json);
	});
}

function stop() {
	$.loadJSON("http://localhost:9018/audio/stop", {}, function(json) {
		log(json);
	});
}

function getTime() {
	$.loadJSON("http://localhost:9018/time/", {}, function(json) {
		log(json);
	});
}

function getRelease() {
	$.loadJSON("http://localhost:9018/release/", {}, testCallback);
}

function getInteger() {
	//$.loadJSON("http://localhost:9018/get/integer/666", {}, testCallback);
	log("getInteger()");
	$.loadJSON("http://localhost:9018/get/integer/666", {}, testCallback);
}

function sendRequest() {
	$.sendRequest({
		'method': 'AutoCompleteCity',
		'callback': testCallback,
		'params': {
			'q': 'Wiesb'
		}
	});
}

var i = 0;
function sendString() {
	log(++i);
	$.loadJSON("http://gd.geobytes.com/AutoCompleteCity", {"q": "Wiesb" }, function(json) {
		log(json);
	});
}

function sendInput() {
	$.loadJSON(document.getElementById("input_url").value, {}, testCallback);
}

function testCallback(json) {
	log(json);
}

function log(message) {
	cons = document.getElementById("console");
	if($.DEBUG) {
		cons.innerHTML += message + "<br />";
	} else {
		console.log(message);
	}
}