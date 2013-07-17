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

        inspector.call(gadget, opts, callback, error);

        return streamId;
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