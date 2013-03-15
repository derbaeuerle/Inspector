window.onload = function() {
	$.init({'appServer': 'http://gd.geobytes.com/'});
	document.getElementById("url").addEventListener('click', sendString, false);
	document.getElementById("obj").addEventListener('click', sendRequest, false);
	document.getElementById("input").addEventListener('click', sendInput, false);
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