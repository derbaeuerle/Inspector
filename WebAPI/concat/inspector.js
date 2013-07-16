/*! PROJECT_NAME - v0.1.0 - 2013-07-16
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
        inspector.initInspector();
        var event = document.createEvent("Event");
        event.initEvent("inspector-ready", true, true);
        window.dispatchEvent(event);

        window.addEventListener('blur', function(e) {
            html = document.documentElement;
            classes = html.className;
            html.className = classes.replace('focused', 'blured');
            console.log(html.className);
        });
        window.addEventListener('focus', function(e) {
            html = document.documentElement;
            classes = html.className;
            html.className = classes.replace('blured', 'focused');
            console.log(html.className);
        });
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
            inspector.unlisten(key);
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
        iframe.onload = function() {
            document.body.removeChild(iframe);
        }
        if(!inspector.browser) {
            iframe.src = command;
        } else {
            window.onbeforeunload = function() { window.location = '#'; window.stop(); };
            window.location = command;
        }
        document.body.appendChild(iframe);
    },

    listen : function(gadget, opts, callback, error, delay) {
        do {
            streamId = "inspector" + parseInt((new Date()).getTime() / Math.random() * 1000, 10);
            opts.streamid = streamId;
        } while (opts.streamid in inspector.listeners);

        inspector.listeners[opts.streamid] = {
            "streamid": opts.streamid,
            "gadget": gadget,
            "opts": opts,
            "callback": callback,
            "error": error,
            "delay": delay || 500
        };

        //inspector.call(gadget, opts, callback, error);

        clazz = function() {
            this.id = opts.streamid;
            this.gadget = gadget;
            this.opts = opts;
            this.delay = delay || 500;
        };

        con = new inspector.connection(opts.streamid, gadget, opts, delay);

        // deep clone error and callback functions.
        if(error) {
            eval("con.error = " + error.toString());
        } else {
            con.error = function(e) {}
        }
        eval("con.callback = " + callback.toString());

        return con;
    },

    connection : function(streamid, gadget, opts, delay) {
        this.id = streamid;
        this.gadget = gadget;
        this.opts = opts;
        this.delay = delay;

        this.call = function() {
            var that = this;
            console.log("delay: " + this.delay);
            url = inspector.serverAddress + this.gadget + "/";

            do {
                var cbName = "ins" + parseInt((new Date()).getTime() / Math.random() * 1000, 10);
            } while (inspector[cbName]);

            that.cbName = cbName;

            url += "?callback=inspector." + cbName;
            for(key in this.opts) {
                if(this.opts.hasOwnProperty(key)) {
                    url += "&" + key + "=" + this.opts[key];
                }
            }

            that.sc = document.createElement("script");
            if(this.error) {
                that.sc.onerror = this.error;
            }
            var cb = this.callback || function(data) {};
            inspector[cbName] = function(data) {
                try {
                    if(data['error']) {
                        // Checks for specific error codes.
                        if(data.error['errorCode']) {
                            var eCode = data.error.errorCode;
                            // Check error code for permission request.
                            if(eCode == 2) {
                                window.setTimeout(function() {
                                    inspector.requestPermission(that.gadget, that.opts, cb, that.error);
                                }, 100);
                            }
                        } else {
                            // Check if error function has been submitted.
                            if(that.error) {
                                that.error(response);
                            }
                        }
                    } else {
                        // Check if call was an stream.
                        if('stream' in response && response.stream.streamid == that.id) {
                            cb(response);
                            
                            window.setTimeout(function() {
                                that.call();
                            }, that.delay);
                        } else {
                            // Call callback of call request.
                            window.setTimeout(function() {
                                cb(response);
                            }, 100);
                        }
                        // inspector.logger(JSON.stringify(response));
                    }
                } finally {
                    delete inspector[that.cbName];
                    script.parentNode.removeChild(that.script);
                }
            };

            that.sc.src = url;
            document.body.appendChild(that.sc);
        };

        this.destroy = function() {

        };
    },

    unlisten : function(id) {
        if(id in inspector.listeners) {
            delete inspector.listeners[id];
        }
    },

    call : function(gadget, opts, callback, error, timeouts) {
        url = inspector.serverAddress + gadget + "/";

        do {
            var cbName = "inspector" + parseInt((new Date()).getTime() / Math.random() * 1000, 10);
        } while (inspector[cbName]);

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
                }, 1500);
            }
        }

        inspector[cbName] = function(response) {
            try {
                if(response['error']) {
                    // Checks for specific error codes.
                    if(response.error['errorCode']) {
                        var eCode = response.error.errorCode;
                        // Check error code for permission request.
                        if(eCode == 2) {
                            window.setTimeout(function() {
                                inspector.requestPermission(gadget, opts, callback, error, timeouts);
                            }, 100);
                        }
                    } else {
                        // Check if error function has been submitted.
                        if(error) {
                            error(response);
                        }
                    }
                } else {
                    // Check if call was an stream.
                    if('stream' in response && response.stream.streamid in inspector.listeners) {
                        stream = inspector.listeners[response.stream.streamid];
                        
                        stream.callback(response);
                        
                        window.setTimeout(function() {
                            inspector.call(stream['gadget'], stream['opts'], stream['callback'], stream['error']);
                        }, stream['delay']);
                    } else {
                        // Call callback of call request.
                        window.setTimeout(function() {
                            callback(response);
                        }, 100);
                    }
                    // inspector.logger(JSON.stringify(response));
                }
            } finally {
                delete inspector[cbName];
                script.parentNode.removeChild(script);
            }
        };
        script.src = url;
        document.body.appendChild(script);
    },

    requestPermission : function(gadget, opts, callback, error, timeouts) {
        var accept = confirm("Permission request: " + gadget);
        if(accept) {
            // If permission granted, send status update.
            opts['permission'] = accept;
            window.setTimeout(function() {
                inspector.call(gadget, opts, callback, error, timeouts);
            }, 100);
        } else {
            // Else publish error to request sender.
            if(error) {
                error({
                    'error': {
                        'message': 'Permission denied by user!',
                        'code': -1
                    }
                });
            }
        }
    },

    logger : function(msg) {
        console.log(msg);
    }

};
addLoadEvent(inspector.init);
inspector.audio = {

    template: '<span class="state">unknown</span>' +
              '<span class="play" inspector-action="play" onclick="inspector.audio.onClick(this);" href="#">Play</span>&nbsp;' +
              '<span class="pause" inspector-action="pause" onclick="inspector.audio.onClick(this);" href="#">Pause</span>&nbsp;' +
              '<span class="stop" inspector-action="stop" onclick="inspector.audio.onClick(this);" href="#">Stop</span>&nbsp;',
    instances: {},
    elements: {},
    force: true,

    onClick: function(el) {
        action = el.getAttribute("inspector-action");
        file = el.parentNode.getAttribute("src");
        playerid = el.parentNode.id;

        // Change relative path of source to absolute path.
        if(file.indexOf('http') === -1) {
            loc = window.location.href.replace(location.hash,"").replace('#', '');
            file = (loc.indexOf('/', loc.length - 1)) ? loc + file : loc + '/' + file;
        }
        file = encodeURIComponent(file);
        opts = {
            "audiofile" : file,
            "do" : action,
            "playerid" : playerid
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
            if(data['playerid']) {
                playerid = data.playerid;
                el = inspector.audio.elements[playerid];
            }

            var event = document.createEvent("Event");
            event.initEvent('state', true, true);
            event.detail = data;
            event.request = {
                message: action,
                audiofile: file,
                playerid: playerid,
                response: data
            };
            el.dispatchEvent(event);

            el.update(data);

            if(action === "play") {
                // If starts playing, safe id of stream and send states for current player state request.
                inspector.audio.instances[playerid] = inspector.listen(inspector.gadgets.AUDIO, {
                    "do": "state",
                    "playerid": playerid
                }, function(data) {
                    if(data['playerid']) {
                        playerid = data.playerid;
                        el = inspector.audio.elements[playerid];
                    }
                    var event = document.createEvent("Event");
                    event.initEvent('state', true, true);
                    event.detail = data;
                    el.dispatchEvent(event);

                    el.update(data);

                    inspector.audio.checkState(data);
                }, null, 200);
            }

            inspector.audio.checkState(data);
        }, null, 20);

    },

    checkState: function(data) {
        // Stopping stream if media player has stoppedd or paused!
        if(!data || data['stopped'] == true || data['state'] == 'PAUSED' || data['state'] == 'STOPPED') {
            stream = data.stream;

            if("playerid" in data && data.playerid) {
                if(inspector.audio.instances[data.playerid] == stream.streamid) {
                    inspector.unlisten(stream.streamid);
                    delete inspector.audio.instances[key];
                }
            } else {
                for(key in inspector.audio.instances) {
                    if(inspector.audio.instances[key] == stream.streamid) {
                        inspector.unlisten(stream.streamid);
                        delete inspector.audio.instances[key];
                    }
                }
            }
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
                id = parseInt((new Date()).getTime() / Math.random(), 10);
                id = 'iaudio' + id;
                iaudio = document.createElement('inspector-audio');
                iaudio.id = id;
                iaudio.setAttribute("inspector-playerid", id);
                iaudio.setAttribute("src", audio.getElementsByTagName('source')[0].getAttribute('src'));
                iaudio.innerHTML = inspector.audio.template;

                for(o=0;o<audio.attributes.length;o++) {
                    iaudio.setAttribute(audio.attributes[o].name, audio.attributes[o].value);
                }

                iaudio.play = function() {
                    el = this.getElementsByClassName("play")[0];
                    if(el) {
                        el.click();
                    }
                }
                iaudio.stop = function() {
                    el = this.getElementsByClassName("stop")[0];
                    if(el) {
                        el.click();
                    }
                }
                iaudio.stop = function() {
                    el = this.getElementsByClassName("pause")[0];
                    if(el) {
                        el.click();
                    }
                }
                iaudio.update = function(data) {
                    el = this.getElementsByClassName("state")[0];
                    if(el) {
                        el.innerHTML = data['state'] || 'unknown';
                    }
                };

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
                };

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
addLoadEvent(inspector.audio.init);
var tagsToReplace = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;'
};

function replaceTag(tag) {
    return tagsToReplace[tag] || tag;
}

function safe_tags_replace(str) {
    return str.replace(/[&<>]/g, replaceTag);
}