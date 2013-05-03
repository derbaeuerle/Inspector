console = document.getElementById("console");

function getColor() {
    try {
        inspector.loadJSON("http://localhost:9090/color", null, function(json) {
            console.style.backgroundColor = "rgb(" + json + ")";
            out = "";
            if(typeof(json) === typeof([])) {
                for(i=0; i<json.length; i++) {
                    if(i + 1 === json.length) {
                        out += parseInt(json[i] * 255);
                    } else {
                        out += (parseInt(json[i] * 255)) + ", ";
                    }
                }
                document.body.style.background = "rgb(" + out + ")";
            }
            else {
                out = typeof(json);
            }
            console.innerHTML = JSON.stringify("rgb(" + out + ")");
        }, null);
    } catch(e) {
        console.innerHTML = e;
    }
}

window.onload = function() {
    inspector.init();
    var color = window.setInterval(getColor, 400);
}
window.onBlur = function() {
    window.clearInterval(color);
}