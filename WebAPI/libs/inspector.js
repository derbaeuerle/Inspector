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

    listen : function(gadget, opts, callback, error) {
        if(!(gadget in inspector.listeners)) {
            inspector.listeners[gadget] = [];
        }
        streamId = "inspector" + parseInt((new Date()).getTime() / Math.random() * 1000, 10);
        opts.id = streamId;

        inspector.call(gadget, opts, callback, error);

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

    /*call : function(gadget, opts, callback, error, timeouts) {
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
                }, 1500);
            }
        }
        inspector.logger("calling gadget:" + gadget + "<br />" + url);
        inspector[cbName] = function(response) {
            try {
                if(response['error']){
                    // Gadget needs permission
                    if(response.error.errorCode == 2) {
                        inspector.requestPermission(gadget, opts, callback, error, timeouts);
                    } else {
                        error(response);
                    }
                } else {
                    window.setTimeout(function() {
                        inspector.logger("callback");
                        callback(response);
                    }, 10);
                    if(opts.id && inspector.listeners[gadget][opts.id]) {
                        inspector.onEvent(gadget, opts, callback, error, response);
                    }
                }
            }
            finally {
                delete inspector[cbName];
                script.parentNode.removeChild(script);
            }
        };
        script.src = url;
        document.body.appendChild(script);
    },*/

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
                }, 1500);
            }
        }

        inspector[cbName] = function(response) {
            try {
                inspector.defaultCallback(gadget, opts, callback, error, timeouts, response);
            } finally {
                delete inspector[cbName];
                script.parentNode.removeChild(script);
            }
        };
        script.src = url;
        document.body.appendChild(script);
    },

    defaultCallback : function(gadget, opts, callback, error, timeouts, response) {
        // Check if an error occured in native interface.
        if(response['error']) {
            // Checks for specific error codes.
            if(response.error['errorCode']) {
                eCode = response.error.errorCode;

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
            // Call callback of call request.
            window.setTimeout(function() {
                callback(response);
            }, 100);

            // Check if call was an stream.
            if(opts['id'] && inspector.listeners[gadget][opts.id]) {
                window.setTimeout(function() {
                    // inspector.onEvent(gadget, opts, callback, error);
                    inspector.call(gadget, opts, callback, error);
                }, 200);
            }
        }
    },

    /*onEvent : function(gadget, opts, callback, error) {
        inspector.logger("onEvent");
        inspector.call(gadget, opts, callback, error);
    },*/

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