var settings = {
	'audiofile' : 'http://www.mi.hs-rm.de/~dbaeu001/jc/grux.mp3',
	'autoprepare' : true,
	'playerid' : 1,
};

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
	$.loadJSON("http://localhost:9018/audio/play", settings, function(json) {
		log(json);
	});
}

function pause() {
	$.loadJSON("http://localhost:9018/audio/pause", settings, function(json) {
		log(json);
	});
}

function stop() {
	$.loadJSON("http://localhost:9018/audio/stop", settings, function(json) {
		log(json);
	});
}

function log(message) {
	cons = document.getElementById("console");
	if($.DEBUG) {
		cons.innerHTML += message + "<br />";
	} else {
		console.log(message);
	}
}