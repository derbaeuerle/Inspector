inspector.audio = {

    id: 0,
    template: '<button id="play" onclick="inspector.audio.onClick(this);">Play</button>' +
              '<button id="pause" onclick="inspector.audio.onClick(this);">Pause</button>' +
              '<button id="stop" onclick="inspector.audio.onClick(this);">Stop</button>',

    onClick: function(el) {
        if (el.id === "play") {
            inspector.call(inspector.gadgets.AUDIO, {
                "audiofile" : el.parentNode.getAttribute('src'),
                "do" : "play",
                "playerid" : el.getAttribute("playerid")
            }, function(data) {
                el.setAttribute("playerid", data.playerid);
                console.log(data);
            });
        } else if (el.id === "pause") {
            inspector.call(inspector.gadgets.AUDIO, {
                "audiofile" : el.parentNode.getAttribute('src'),
                "do" : "pause",
                "playerid" : el.getAttribute("playerid")
            }, function(data) {
                el.setAttribute("playerid", data.playerid);
                console.log(data);
            });
        } else if (el.id === "stop") {
            inspector.call(inspector.gadgets.AUDIO, {
                "audiofile" : el.parentNode.getAttribute('src'),
                "do" : "stop",
                "playerid" : el.getAttribute("playerid")
            }, function(data) {
                el.setAttribute("playerid", data.playerid);
                console.log(data);
            });
        }
    },

    init: function() {
        var audios = document.getElementsByTagName('audio');
        for(i=0; i<audios.length; i++) {
            id = 'iaudio' + inspector.audio.id++;
            var iaudio = document.createElement('inspector-audio');
            iaudio.id = id;
            iaudio.setAttribute("src", audios[0].getElementsByTagName('source')[0].getAttribute('src'));
            iaudio.innerHTML = inspector.audio.template;
            audios[i].parentNode.replaceChild(iaudio, audios[i]);
        }
    }

};
window.onload = inspector.audio.init;