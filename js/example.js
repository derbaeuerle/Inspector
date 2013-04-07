var settings = {
	'audiofile' : 'http://www.mi.hs-rm.de/~dbaeu001/jc/grux.mp3',
	'autoprepare' : true,
	'playerid' : 1,
};

window.onload = function() {
	$.init();
	/*document.getElementById("url").addEventListener('click', sendString, false);
	document.getElementById("obj").addEventListener('click', sendRequest, false);
	document.getElementById("input").addEventListener('click', sendInput, false);

	document.getElementById("time").addEventListener('click', getTime, false);
	document.getElementById("release").addEventListener('click', getRelease, false);
	document.getElementById("integer").addEventListener('click', getInteger, false);*/

	document.getElementById("play").addEventListener('click', play, false);
	document.getElementById("pause").addEventListener('click', pause, false);
	document.getElementById("stop").addEventListener('click', stop, false);
	document.getElementById("twitter").addEventListener('click', twitter, false);
}

function twitter() {
	/*$.loadJSON("http://gibt-es-nicht.example.com/", {}, function(json) {
		log(json);
	}, function(error) {
		console.log("own error handler");
		console.log(error);
	});*/
	$.loadJSON("https://api.twitter.com/1/users/show.json?screen_name=TwitterAPI&include_entities=true", {}, function(json) {
		log(json);
	}, null);
}

function play() {
	$.loadJSON("http://localhost:9018/audio/play", settings, function(json) {
		log(json);
	}, null);
}

function pause() {
	$.loadJSON("http://localhost:9018/audio/pause", settings, function(json) {
		log(json);
	}, null);
}

function stop() {
	$.loadJSON("http://localhost:9018/audio/stop", settings, function(json) {
		log(json);
	}, null);
}

function log(message) {
	cons = document.getElementById("console");
	if($.DEBUG) {
		cons.innerHTML += JSON.stringify(message, undefined, 4); + "<br />";
	} else {
		console.log(message);
	}
}