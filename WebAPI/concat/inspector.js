/*! PROJECT_NAME - v0.1.0 - 2013-08-01
* http://PROJECT_WEBSITE/
* Copyright (c) 2013 YOUR_NAME; Licensed MIT */
Array.prototype.findAttribute = function(key, value) {
    for(var i=0; i<this.length; i++) {
        if(typeof(this[i]) === typeof({})) {
            if(key in this[i]) {
                if(this[i][key] == value) {
                    return this[i];
                }
            }
        }
    }
    return false;
};

var inspector = {
    intents: {
        init: 'inspector://init/'
    },
    events: {
        data: 'DATA',
        feedback: 'FEEDBACK',
        state: 'STATE',
        error: 'ERROR'
    },
    connections: {},
    commandAddress: 'http://localhost:9090/inspector/',
    stateAddress: 'http://localhost:9191/state/',
    max_timeouts: 10,
    state_stream: null,

    __id: null,

    init: function() {
        inspector.__id = inspector.__generateUID();
        inspector.intents.init += inspector.__id + "/";
        inspector.sendIntent(inspector.intents.init);
        var event = document.createEvent("Event");
        event.initEvent("inspector-ready", true, true);
        window.dispatchEvent(event);
    },

    sendIntent: function(intent) {
        intent += '?' + inspector.__generateCallbackName();
        var iframe = document.createElement("iframe");
        iframe.id = "ajaxFrame";
        iframe.style.display = "none";
        iframe.src = intent;
        document.body.appendChild(iframe);
        window.setTimeout(function() {
            iframe.parentNode.removeChild(iframe);
        }, 10);
    },

    use: function(gadget, keep_alive) {
        if(!Object.keys(inspector.connections).length && !inspector.state_stream) {
            inspector.state_stream = new inspector.stateStream();
            inspector.state_stream.getData();
        } else if(!Object.keys(inspector.connections).length && inspector.state_stream) {
            if(!inspector.state_stream.isRunning()) {
                inspector.state_stream.running = true;
                inspector.state_stream.timeouts = 0;
                inspector.state_stream.getData();
            }
        }

        gadget = gadget.toUpperCase();
        if(!(gadget in inspector.connections)) {
            g = new inspector.gadget(gadget, keep_alive);
            g.__constructor();
            inspector.connections[gadget] = g;
        }
        return inspector.connections[gadget];
    },

    __generateCallbackName: function() {
        return "cb" + inspector.__generateUID();
    },

    __generateUID: function() {
        return parseInt((new Date()).getTime() / Math.random() * 1000, 10);
    },

    on : function(eventType, callback, conditions) {
        if(!("SYSTEM" in inspector.connections)) {
            inspector.connections["SYSTEM"] = {};
            inspector.connections.SYSTEM.eventListener = {};
        }
        if(!(eventType in inspector.connections.SYSTEM.eventListener)) {
            inspector.connections.SYSTEM.eventListener[eventType] = [];
        }
        inspector.connections.SYSTEM.eventListener[eventType].push({
            "test": "test",
            "id" : inspector.connections.SYSTEM.eventListener[eventType].length,
            'event': eventType,
            'callback': callback,
            'conditions': conditions
        });
        return inspector.connections.SYSTEM.eventListener[eventType].length;
    },

    off : function(eventType, id) {
        var element = inspector.connections.SYSTEM.eventListener[eventType].findAttribute("id", id);
        var ret = !!(inspector.connections.SYSTEM.eventListener[eventType].splice(element, 1));

        if(inspector.connections.SYSTEM.eventListener[eventType].length === 0) {
            delete inspector.connections.SYSTEM.eventListener[eventType];
            if(Object.keys(inspector.connections.SYSTEM.eventListener).length === 0) {
                delete inspector.connections.SYSTEM;
                if(Object.keys(inspector.connections).length === 0) {
                    inspector.state_stream.stop();
                }
            }
        }
        return ret;
    },

    stateStream : function() {
        var me = this;
        me.timeouts = 0;
        me.callbackName = "inspector_state";
        me.running = true;
        me.script = null;

        me.isRunning = function() {
            return me.running;
        };

        me.getData = function() {
            me.callbackName = "inspector_state_" + inspector.__generateCallbackName();
            window[me.callbackName] = __callback;
            if(!inspector.__id) {
                inspector.__id = inspector.__generateUID();
            }
            var url = inspector.stateAddress + "?browserid=" + inspector.__id + "&callback=" + me.callbackName;
            me.script = document.createElement("script");
            me.script.src = url;
            me.script.onerror = function(e) {
                me.timeouts += 1;
                me.script.parentNode.removeChild(me.script);
                if(me.timeouts < inspector.max_timeouts && me.running) {
                    inspector.sendIntent(inspector.intents.init);
                    window.setTimeout(function() {
                        me.getData();
                    }, me.timeouts * 500);
                }
            };
            document.body.appendChild(me.script);
        };

        me.stop = function() {
            me.running = false;
        };

        var __callback = function(data) {
            try {
                // Iterate all received event data.
                for(var id in data) {
                    var item = data[id];
                    // Get connection for gadget.
                    var connection = inspector.connections[item.gadget];
                    // Test if connection is available and if there are listeners to this event.
                    if(connection && connection.eventListener[item.event]) {
                        // Get listener array.
                        var listener = connection.eventListener[item.event];
                        for(var i in listener) {
                            var l = listener[i];
                            var alarm = true;

                            var itemData = item.data;
                            // Test if all listener conditions are confirmed.
                            for(var con in l.conditions) {
                                if(!(con in itemData) || itemData[con] !== l.conditions[con]) {
                                    alarm = false;
                                    break;
                                }
                            }

                            if(alarm && l.callback) {
                                l.callback(item);
                            }
                        }
                    }
                }
            } catch(e) {
                console.log(e.message);
            }
            try {
                delete window[me.callbackName];
            } catch(e) {}
            try {
                me.script.parentNode.removeChild(me.script);
                me.script = null;
            } catch(e) {}

            if(me.running) {
                me.getData();
            }
        };
    },

    gadget : function(gadget, keep_alive) {
        var me = this;
        me.gadget = gadget;
        me.callbackName = inspector.__generateCallbackName();
        me.stateCallbackName = inspector.__generateCallbackName();
        me.eventListener = {};
        me.stateScript = null;
        me.basicUrl = inspector.commandAddress + gadget + '/';
        me.timeouts = 0;
        me.keep_alive = keep_alive || 200;
        me.__destroyed = false;
        me.__initial = false;
        me.__initialQueue = [];

        me.__constructor = function() {
            me.__initial = false;
            __sendCommand({'inspector-cmd': 'initial'}, __callback);
        };

        me.__destroy = function() {
            // Send destroy command.
            try {
                delete window[me.stateCallbackName];
                me.stateCallbackName = null;
            } catch(e) {}
            try {
                me.stateScript.parentNode.removeChild(me.stateScript);
                me.stateScript = null;
            } catch(e) {}

            delete inspector.connections[me.gadget];
            if(Object.keys(inspector.connections).length === 0) {
                inspector.state_stream.stop();
            }
            me.eventListener = {};
            me.__initial = false;
            me.__destroyed = true;
        };

        me.on = function(event, callback, conditions) {
            if(!(event in me.eventListener)) {
                me.eventListener[event] = [];
            }
            me.eventListener[event].push({
                "id" : me.eventListener[event].length,
                "callback" : callback,
                "conditions" : conditions
            });
            return me.eventListener[event].length - 1;
        };

        me.off = function(event, id) {
            var element = me.eventListener[event].findAttribute("id", id);
            return !!(me.eventListener[event].splice(element, 1));
        };

        me.submit = function(params, callback) {
            if(!me.__destroyed) {
                if(!me.__initial) {
                    me.__initialQueue.push({"params": params, "callback": callback});
                    return;
                }
                __sendCommand(params, callback);
            }
        };

        me.__errorHandler = function(data) {
            try {
                var code = parseInt(data.data.errorCode, 10);
                if(code === 2) {
                    var accept = confirm("Permission request: " + me.gadget);
                    if(accept) {
                        // If permission granted, send status update.
                        var params = {
                            permission: accept
                        };
                        if(data.request) {
                            delete data.request.callback;
                            params = data.request;
                            params.permission = accept;
                        }
                        // TODO: Send original params!!!!
                        __sendCommand(params, __callback);
                    }
                }
            } catch (e) {

            }
        };

        var __sendCommand = function(params, callback) {
            var cbName = inspector.__generateCallbackName();
            var url = me.basicUrl + "?callback=" + cbName;

            if(!inspector.__id) {
                inspector.__id = inspector.__generateUID();
            }
            params.browserid = inspector.__id;
            for(var key in params) {
                if(params.hasOwnProperty(key)) {
                    url += ("&" + key + "=" + params[key]);
                }
            }

            script = document.createElement("script");
            script.src = url;
            script.onerror = function(e) {
                me.timeouts += 1;
                script.parentNode.removeChild(script);
                if(me.timeouts < inspector.max_timeouts) {
                    window.setTimeout(function() {
                        __sendCommand(params, callback);
                    }, me.timeouts * 50);
                }
            };
            if(!me.__destroyed) {
                window[cbName] = function(data) {
                    try {
                        if(data.event === 'error') {
                            me.__errorHandler(data);
                        }
                        if(callback) {
                            callback(data);
                        }
                    } finally {
                        script.parentNode.removeChild(script);
                        delete window[cbName];
                    }
                };

                document.body.appendChild(script);
            }
        };

        var __requestState = function() {
            if(!inspector.__id) {
                inspector.__id = inspector.__generateUID();
            }
            var url = me.basicUrl + "keep-alive/?callback=" + me.stateCallbackName + "&browserid=" + inspector.__id;
            me.stateScript = document.createElement("script");
            me.stateScript.src = url;
            me.stateScript.onerror = function(e) {
                me.timeouts += 1;
                me.stateScript.parentNode.removeChild(me.stateScript);
                if(me.timeouts < inspector.max_timeouts) {
                    window.setTimeout(function() {
                        __requestState();
                    }, me.timeouts * 50);
                }
            };
            if(me.stateCallbackName && !me.__destroyed) {
                window[me.stateCallbackName] = __stateCallback;
                document.body.appendChild(me.stateScript);
            }
        };

        var __stateCallback = function(data) {
            try {
                me.stateCallbackName = inspector.__generateCallbackName();
                window.setTimeout(function() {
                    __requestState();
                }, me.keep_alive);
            } catch(e) {
            } finally {
                me.stateScript.parentNode.removeChild(me.stateScript);
            }
        }

        var __callback = function(response) {
            try {
                if (response.data === 'initial') {
                    me.__initial = true;
                }
                if(me.__initialQueue.length > 0) {
                    var next = me.__initialQueue.splice(0, 1)[0];
                    __sendCommand(next.params, next.callback);
                }
                if (response.data === 'initial') {
                    window.setTimeout(function() {
                        __requestState();
                    }, me.keep_alive);
                }
            } catch(e) {
            }
        };

    }

};

window.addEventListener("load", inspector.init);

inspector.audio = {

    template: '<small class="state">unknown</small>&nbsp;&nbsp;' +
              '<span class="position">00:00</span>/<span class="duration">00:00</span>&nbsp;&nbsp;&nbsp;' +
              '<span class="play" inspector-action="play" onclick="inspector.audio.onClick(this);" href="#">Play</span>&nbsp;' +
              '<span class="pause" inspector-action="pause" onclick="inspector.audio.onClick(this);" href="#">Pause</span>&nbsp;' +
              '<span class="stop" inspector-action="stop" onclick="inspector.audio.onClick(this);" href="#">Stop</span>',
    instances: {},
    elements: {},
    force: true,
    gadget: null,

    onClick: function(el) {
        var action = el.getAttribute("inspector-action");
        var file = el.parentNode.getAttribute("src");
        var playerid = el.parentNode.id;

        var actives = el.parentNode.getElementsByClassName("active");
        for(var i=0; i<actives.length; i++) {
            var act = actives[i];
            act.className = act.className.replace(new RegExp('(\\s|^)active(\\s|$)'),' ').replace(/^\s+|\s+$/g, '');
        }
        if(!new RegExp('active').test(el.className)) {
            el.className += " active";
        }
        el = el.parentNode;

        // Change relative path of source to absolute path.
        if(file.indexOf('http') === -1) {
            var loc = window.location.href;
            loc = loc.substr(0, loc.lastIndexOf('/') + 1).replace(window.location.hash,"").replace('#', '');
            file = (loc.indexOf('/', loc.length - 1)) ? loc + file : loc + '/' + file;
        }
        file = encodeURIComponent(file);
        
        if(!inspector.audio.gadget) {
            inspector.audio.startStream();
        }

        var params = {
            'do': action,
            'audiofile': file,
            'playerid': playerid
        };
        inspector.audio.gadget.submit(params);

        if(action !== 'stop') {
            inspector.audio.instances[playerid] = playerid;
        } else {
            delete inspector.audio.instances[playerid];
            inspector.audio.updateState({
                playerid: playerid,
                state: 'STOPPED'
            });
        }
        if(!Object.keys(inspector.audio.instances).length) {
            inspector.audio.gadget.__destroy();
            inspector.audio.gadget = null;
        }
    },

    startStream: function() {
        if(!inspector.audio.gadget) {
            inspector.audio.gadget = inspector.use("AUDIO");
            inspector.audio.gadget.on(inspector.events.data, function(response) {
                for(var playerid in response.data) {
                    var item = response.data[playerid];

                    // Check if playerid is in this instances.
                    if(inspector.audio.instances[playerid]) {
                        // Check if response was messed up.
                        if(typeof(item) === typeof({})) {
                            inspector.audio.updateState(item);
                        }
                    }
                }
            });
        }
    },

    updateState: function(data) {
        var id = data.playerid;
        var el = inspector.audio.elements[id];
        var state = el.getElementsByClassName("state")[0];
        var classes = state.className.split(' ');
        if (classes.indexOf(data.state.toLowerCase()) === -1) {
            console.log("switch state to: " + data.state.toLowerCase());
            state.className = "state " + data.state.toLowerCase();
            state.innerHTML = data.state;
        }
        el.getElementsByClassName("position")[0].innerHTML = inspector.audio.milliToTime(data.position);
        el.getElementsByClassName("duration")[0].innerHTML = inspector.audio.milliToTime(data.duration);
    },

    milliToTime: function(ms) {
        if(!ms) {
            return '00:00';
        }
        x = ms / 1000
        seconds = parseInt(x % 60, 10);
        x /= 60
        minutes = parseInt(x % 60, 10);
        x /= 60
        hours = parseInt(x % 24, 10);

        return ((hours) ? hours + ':' : '') + ((minutes) ? minutes + ':' : '00:') + ((seconds) ? ((seconds < 10) ? '0' + seconds : seconds) : '00');
    },

    needReplacement: function(el) {
        var srcs = el.getElementsByTagName('source');
        for(var x=0; x<srcs.length; x++) {
            if(el.canPlayType(srcs[x].getAttribute('type'))) {
                return false;
            }
        }
        return true;
    },

    play: function() {
        var el = this.getElementsByClassName("play")[0];
        if(el) {
            el.click();
        }
    },

    stop: function() {
        var el = this.getElementsByClassName("stop")[0];
        if(el) {
            el.click();
        }
    },

    pause: function() {
        var el = this.getElementsByClassName("pause")[0];
        if(el) {
            el.click();
        }
    },

    update: function(data) {
        var el = this.getElementsByClassName("state")[0];
        if(el) {
            el.innerHTML = data.state || 'unknown';
        }
    },

    check: function() {
        var audios = document.getElementsByTagName('audio');
        for(var i=0; i<audios.length; i++) {
            var audio = audios[i];
            if(inspector.audio.force || inspector.audio.needReplacement(audio)) {
                var id = parseInt((new Date()).getTime() / Math.random(), 10);
                id = 'iaudio' + id;
                var iaudio = document.createElement('inspector-audio');
                iaudio.id = id;
                iaudio.setAttribute("inspector-playerid", id);
                iaudio.setAttribute("src", audio.getElementsByTagName('source')[0].getAttribute('src'));
                iaudio.innerHTML = inspector.audio.template;

                for(var o=0;o<audio.attributes.length;o++) {
                    iaudio.setAttribute(audio.attributes[o].name, audio.attributes[o].value);
                }

                iaudio.play = inspector.audio.play;
                iaudio.pause = inspector.audio.pause;
                iaudio.stop = inspector.audio.stop;
                iaudio.update = inspector.audio.update;

                /*iaudio.seek = function(to) {
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
                };
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
                };
                iaudio.leftVolume = function(volume) {
                    inspector.call(inspector.gadgets.AUDIO, {
                        "audiofile" : iaudio.getAttribute('src'),
                        "do" : "volume",
                        "playerid" : iaudio.getAttribute('inspector-playerid'),
                        "volume-left" : volume
                    }, function(data) {
                        var event = document.createEvent("Event");
                        event.initEvent('state', true, true);
                        event.detail = data;
                        iaudio.dispatchEvent(event);
                        inspector.audio.updateState(iaudio, data);
                    });
                };
                iaudio.rightVolume = function(volume) {
                    inspector.call(inspector.gadgets.AUDIO, {
                        "audiofile" : iaudio.getAttribute('src'),
                        "do" : "volume",
                        "playerid" : iaudio.getAttribute('inspector-playerid'),
                        "volume-right" : volume
                    }, function(data) {
                        var event = document.createEvent("Event");
                        event.initEvent('state', true, true);
                        event.detail = data;
                        iaudio.dispatchEvent(event);
                        inspector.audio.updateState(iaudio, data);
                    });
                };*/

                audio.parentNode.replaceChild(iaudio, audio);
                inspector.audio.elements[id] = iaudio;
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
window.addEventListener("inspector-ready", inspector.audio.init);
