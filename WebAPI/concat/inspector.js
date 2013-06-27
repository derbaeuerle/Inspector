/*! PROJECT_NAME - v0.1.0 - 2013-06-27
* http://PROJECT_WEBSITE/
* Copyright (c) 2013 YOUR_NAME; Licensed MIT */
function addLoadEvent(func) {
    var oldonload = window.onload;
    if (typeof window.onload != 'function') {
        window.onload = func;
    } else {
        window.onload = function() {
            if (oldonload) {
                oldonload();
            }
            func();
        }
    }
}
function getElementByAttribute(attr, value, root) {
    root = root || document.body;
    if(root.hasAttribute(attr) && root.getAttribute(attr) == value) {
        return root;
    }
    var children = root.children, 
        element;
    for(var i = children.length; i--; ) {
        element = getElementByAttribute(attr, value, children[i]);
        if(element) {
            return element;
        }
    }
    return null;
}

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
        inspector.logger('init inspector');
        //window.onbeforeunload = inspector.destroyInspector;
        //window.onblur = inspector.destroyInspector;
        inspector.initInspector();
        var event = document.createEvent("Event");
        event.initEvent("inspector-ready", true, true);
        window.dispatchEvent(event);
    },

    initInspector : function() {
        //if(!inspector.browser) {
            inspector.sendIntent(inspector.initCommand);
        /*} else {
            inspector.sendIntent('intent://init/#Intent;scheme=inspector;package=de.hsrm.inspector;end');
        }*/
    },

    destroyInspector : function() {
        inspector.destroyListeners();
        //if(!inspector.browser) {
            inspector.sendIntent(inspector.destroyCommand);
        /*} else {
            inspector.sendIntent('intent://init/#Intent;scheme=inspector;package=de.hsrm.inspector;end');
        }*/
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

    //TODO: Overwrite onError in call function to do call again.
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
    },

    logger : function(msg) {
        console.log(msg);
    }

};
addLoadEvent(inspector.init);
inspector.audio = {

    id: 1,
    template: '<inspector-audio-state>unknown</inspector-audio-state>' +
              '<button inspector-action="play" onclick="inspector.audio.onClick(this);">Play</button>' +
              '<button inspector-action="pause" onclick="inspector.audio.onClick(this);">Pause</button>' +
              '<button inspector-action="stop" onclick="inspector.audio.onClick(this);">Stop</button>',
    instances: {},
    elements: [],

    onClick: function(el) {
        action = el.getAttribute("inspector-action");
        file = el.parentNode.getAttribute("src");
        playerid = el.parentNode.getAttribute("inspector-playerid");
        if(file.indexOf('http') === -1) {
            loc = window.location.href;
            file = (loc.indexOf('/', loc.length - 1)) ? loc + file : loc + '/' + file;
        }
        file = encodeURIComponent(file);
        inspector.call(inspector.gadgets.AUDIO, {
            "audiofile" : file,
            "do" : action,
            "playerid" : playerid
        }, function(data) {
            var event = document.createEvent("Event");
            event.initEvent('state', true, true);
            event.detail = data;
            event.request = {
                message: action,
                audiofile: file,
                playerid: playerid,
                response: data
            };
            el.parentNode.dispatchEvent(event);
            inspector.audio.updateState(el.parentNode, data);
        });
        if(action === "play") {
            // If starts playing, safe id of stream and send states for current player state request.
            inspector.audio.instances[playerid] = inspector.listen(inspector.gadgets.AUDIO, {
                "do": "state",
                "playerid": playerid
            }, function(data) {
                var event = document.createEvent("Event");
                event.initEvent('state', true, true);
                event.detail = data;
                el.parentNode.dispatchEvent(event);
                inspector.audio.updateState(el.parentNode, data);
            });
        } else {
            // Else remove player state request from streams.
            window.setTimeout(function() {
                inspector.unlisten(inspector.gadgets.AUDIO, inspector.audio.instances[playerid]);
            }, 500);
        }

    },

    updateState: function(el, response) {
        sEl = el.getElementsByTagName('inspector-audio-state');
        if(sEl.length > 0) {
            sEl = sEl[0];
            sEl.innerHTML = response.state || 'unknown';
        }
    },

    needReplacement: function(el) {
        srcs = el.getElementsByTagName('source');
        for(x=0; x<srcs.length; x++) {
            if(el.canPlayType(srcs[x].getAttribute('type'))) {
                return false;
            }
        }
        return true;
    },

    check: function() {
        var audios = document.getElementsByTagName('audio');
        for(i=0; i<audios.length; i++) {
            if(inspector.audio.needReplacement(audios[i])) {
                id = (new Date()).getTime() / inspector.audio.id++;
                id = id / (Math.floor(Math.random()*1001));
                id = 'iaudio' + id;
                var iaudio = document.createElement('inspector-audio');
                iaudio.setAttribute("inspector-playerid", id);
                iaudio.setAttribute("src", audios[i].getElementsByTagName('source')[0].getAttribute('src'));
                iaudio.innerHTML = inspector.audio.template;

                iaudio.play = function() {
                    el = getElementByAttribute("inspector-action", "play", iaudio);
                    if(el) {
                        el.click();
                    }
                }
                iaudio.stop = function() {
                    el = getElementByAttribute("inspector-action", "stop", iaudio);
                    if(el) {
                        el.click();
                    }
                }
                iaudio.stop = function() {
                    el = getElementByAttribute("inspector-action", "pause", iaudio);
                    if(el) {
                        el.click();
                    }
                }

                audios[i].parentNode.replaceChild(iaudio, audios[i]);
                inspector.audio.elements.push(iaudio);
                i--;
            }
        }
    },

    init: function() {
        inspector.logger("init() audio");
        inspector.audio.check();
        var event = document.createEvent("Event");
        event.initEvent('inspector-audio-ready', true, true);
        window.dispatchEvent(event);
    }

};
addLoadEvent(inspector.audio.init);