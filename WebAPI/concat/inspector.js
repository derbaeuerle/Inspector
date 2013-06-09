/*! PROJECT_NAME - v0.1.0 - 2013-06-09
* http://PROJECT_WEBSITE/
* Copyright (c) 2013 YOUR_NAME; Licensed MIT */
inspector = {

    initCommand: 'inspector://init/',
    destroyCommand: 'inspector://destroy/',
    serverAddress: 'http://localhost:9090/inspector/',
    gadgets: {
        AUDIO: 'AUDIO',
        ACCELERATION: 'ACCELERATION',
        GYROSCOPE: 'GYROSCOPE',
        TEMPERATURE: 'TEMPERATURE',
        LIGHT: 'LIGHT',
        MAGNETIC: 'MAGNETIC',
        ORIENTATION: 'ORIENTATION',
        PRESSURE: 'PRESSURE',
        PROXIMITY: 'PROXIMITY'
    },
    listeners: {},
    browser: null,
    timeout: -1,

    init : function() {
        uMatch = navigator.userAgent.match(/Chrome\/[0-9]{1,2}/);
        if(uMatch && uMatch.length >= 1) {
            inspector.browser = parseInt(uMatch[0].replace("Chrome/", ""), 10);
            inspector.browser = (inspector.browser >= 25) ? inspector.browser : null;
        }
        //window.onbeforeunload = inspector.destroyInspector;
        window.onblur = inspector.destroyInspector;
        inspector.initInspector();
    },

    initInspector : function() {
        if(!inspector.browser) {
            inspector.sendIntent(inspector.initCommand);
        } else {
            inspector.sendIntent('intent://init/#Intent;scheme=inspector;package=de.hsrm.inspector;end');
        }
    },

    destroyInspector : function() {
        inspector.destroyListeners();
        if(!inspector.browser) {
            inspector.sendIntent(inspector.destroyCommand);
        } else {
            inspector.sendIntent('intent://init/#Intent;scheme=inspector;package=de.hsrm.inspector;end');
        }
    },

    destroyListeners : function() {
        for (var key in inspector.listeners) {
            var obj = inspector.listeners[key];
            for (var prop in obj) {
                inspector.unlisten(key, prop);
            }
        }
    },

    sendIntent : function(command) {
        iframe = document.createElement("iframe");
        iframe.id = "ajaxFrame";
        iframe.style.display = "none";
        iframe.onerror = function(e) {
            document.body.removeChild(iframe);
            console.log(e);
        }
        if(!inspector.browser) {
            iframe.src = command;
        } else {
            window.onbeforeunload = function() { window.location = '#'; window.stop(); };
            window.location = command;
        }
        document.body.appendChild(iframe);
    },

    listen : function(gadget, opts, callback) {
        if(!(gadget in inspector.listeners)) {
            inspector.listeners[gadget] = [];
        }
        id = window.setInterval(function() { inspector.call(gadget, opts, callback); }, 500 || opts.interval);
        inspector.listeners[gadget][id] = id;
        return id;
    },

    unlisten : function(gadget, id) {
        if(gadget in inspector.listeners) {
            if(id in inspector.listeners[gadget]) {
                gadgetListeners = inspector.listeners[gadget];
                window.clearInterval(gadgetListeners[id]);
                delete(gadgetListeners[gadgetListeners.indexOf(id)]);
            }
        }
    },

    call : function(gadget, opts, callback) {
        url = inspector.serverAddress + gadget + "/";

        var cbName = "inspector" + Math.floor((new Date()).getTime() / Math.random());
        url += '?callback=inspector.' + cbName;

        for(var key in opts) {
            if(opts.hasOwnProperty(key)) {
                url += "&" + key + "=" + opts[key];
            }
        }

        script = document.createElement("script");

        script.onerror = inspector.onError;
        this[cbName] = function(response) {
            try {
                callback(response);
            }
            finally {
                delete this[cbName];
                script.parentNode.removeChild(script);
            }
        };
        script.src = url;
        document.body.appendChild(script);
    },

    onError : function(e) {
        if(inspector.timeout === -1) {
            inspector.timeout = 20;
            inspector.timeout--;
            inspector.initInspector();
        } else if(inspector.timeout > 0) {
            inspector.initInspector();
            inspector.timeout--;
        } else if(inspector.timeout === 0) {
            throw new Exception("Failed to establish connection!");
        }
        console.error(e);
    }

};
window.onload = inspector.init;
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