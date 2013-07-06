/*! PROJECT_NAME - v0.1.0 - 2013-07-06
* http://PROJECT_WEBSITE/
* Copyright (c) 2013 YOUR_NAME; Licensed MIT */
function addLoadEvent(func) {
    var oldonload = window.onload;
    if (typeof window.onload !== 'function') {
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
    timeout: -1,

    init : function() {
        //window.onbeforeunload = inspector.destroyInspector;
        //window.onblur = inspector.destroyInspector;
        inspector.initInspector();
        var event = document.createEvent("Event");
        event.initEvent("inspector-ready", true, true);
        window.dispatchEvent(event);
    },

    initInspector : function() {
        inspector.sendIntent(inspector.initCommand);
    },

    destroyInspector : function() {
        inspector.destroyListeners();
        inspector.sendIntent(inspector.destroyCommand);
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

    listen : function(gadget, opts, callback, error) {
        if(!(gadget in inspector.listeners)) {
            inspector.listeners[gadget] = [];
        }
        streamId = "inspector" + parseInt((new Date()).getTime() / Math.random() * 1000, 10);
        opts.id = streamId;

        inspector.call(gadget, opts, callback, error);

        // id = window.setInterval(function() { inspector.call(gadget, opts, callback, error); }, 500 || opts.interval);
        inspector.listeners[gadget][streamId] = streamId;
        return streamId;
    },

    unlisten : function(gadget, id) {
        if(gadget in inspector.listeners) {
            if(id in inspector.listeners[gadget]) {
                delete inspector.listeners[gadget][id];
            }
        }
    },

    onEvent : function(gadget, opts, callback, error) {
        window.setTimeout(function() {
            inspector.call(gadget, opts, callback, error);
        }, 100);
    },

    call : function(gadget, opts, callback, error, timeouts) {
        url = inspector.serverAddress + gadget + "/";

        var cbName = "inspector" + parseInt((new Date()).getTime() / Math.random() * 1000, 10);
        url += '?callback=inspector.' + cbName;
        for(var key in opts) {
            if(opts.hasOwnProperty(key)) {
                url += "&" + key + "=" + opts[key];
            }
        }

        script = document.createElement("script");
        script.onerror = function(e) {
            if(error) {
                error(e, gadget, opts, callback, error);
            } if(timeouts && timeouts > 0) {
                inspector.initInspector();
                window.setTimeout(function() {
                    timeouts -= 1;
                    inspector.call(gadget, opts, callback, error, timeouts);
                }, 200);
            }
        }

        inspector[cbName] = function(response) {
            try {
                if(response['error']){
                    error(response);
                } else {
                    callback(response);
                }
                if(opts.id && inspector.listeners[gadget][opts.id]) {
                    inspector.onEvent(gadget, opts, callback, error, response);
                }
            }
            finally {
                delete inspector[cbName];
                script.parentNode.removeChild(script);
            }
        };
        script.src = url;
        document.body.appendChild(script);
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
    force: true,

    onClick: function(el) {
        action = el.getAttribute("inspector-action");
        file = el.parentNode.getAttribute("src");
        playerid = el.parentNode.getAttribute("inspector-playerid");
        if(file.indexOf('http') === -1) {
            loc = window.location.href;
            file = (loc.indexOf('/', loc.length - 1)) ? loc + file : loc + '/' + file;
        }
        file = encodeURIComponent(file);
        opts = {
            "audiofile" : file,
            "do" : action //,
            //"playerid" : playerid
        };

        for(a=0;a<el.parentNode.attributes.length;a++) {
            attr = el.parentNode.attributes[a];
            if(attr.value === true || attr.value === "") {
                attr.value = true;
            } else if (!attr.value) {
                attr.value = false;
            }
            opts[attr.name.replace('inspector-', '')] = attr.value;
        }
        inspector.call(inspector.gadgets.AUDIO, opts, function(data) {
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
        }, null, 20);
        if(action === "play") {
            // If starts playing, safe id of stream and send states for current player state request.
            window.setTimeout(function() {
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
            }, 1000);
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
            audio = audios[i];
            if(inspector.audio.force || inspector.audio.needReplacement(audio)) {
                id = (new Date()).getTime() / inspector.audio.id++;
                id = id / (Math.floor(Math.random()*1001));
                id = 'iaudio' + id;
                var iaudio = document.createElement('inspector-audio');
                iaudio.setAttribute("inspector-playerid", id);
                iaudio.setAttribute("src", audio.getElementsByTagName('source')[0].getAttribute('src'));
                iaudio.innerHTML = inspector.audio.template;

                for(o=0;o<audio.attributes.length;o++) {
                    iaudio.setAttribute(audio.attributes[o].name, audio.attributes[o].value);
                }

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
                iaudio.seek = function(to) {
                    inspector.call(inspector.gadgets.AUDIO, {
                        "audiofile" : iaudio.getAttribute('src'),
                        "do" : "seek",
                        "playerid" : iaudio.getAttribute('inspector-playerid'),
                        "seekto" : to
                    }, function(data) {
                        var event = document.createEvent("Event");
                        event.initEvent('state', true, true);
                        event.detail = data;
                        iaudio.dispatchEvent(event);
                        inspector.audio.updateState(iaudio, data);
                    });
                }
                iaudio.volume = function(volume) {
                    inspector.call(inspector.gadgets.AUDIO, {
                        "audiofile" : iaudio.getAttribute('src'),
                        "do" : "volume",
                        "playerid" : iaudio.getAttribute('inspector-playerid'),
                        "volume" : volume
                    }, function(data) {
                        var event = document.createEvent("Event");
                        event.initEvent('state', true, true);
                        event.detail = data;
                        iaudio.dispatchEvent(event);
                        inspector.audio.updateState(iaudio, data);
                    });
                }

                audio.parentNode.replaceChild(iaudio, audio);
                inspector.audio.elements.push(iaudio);
                i--;
                inspector.audio.autoplay(iaudio);
            }
        }
    },

    autoplay: function(iaudio) {
        if(iaudio.getAttribute('autoplay') || iaudio.getAttribute('autoplay') === "") {
            iaudio.play();
        }
    },

    init: function() {
        inspector.audio.check();
        var event = document.createEvent("Event");
        event.initEvent('inspector-audio-ready', true, true);
        window.dispatchEvent(event);
    }

};
addLoadEvent(inspector.audio.init);