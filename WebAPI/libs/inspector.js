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
        init: 'inspector://init/',
        settings: 'inspector://settings/'
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
        inspector.__id = (inspector.__id) ? inspector.__id : inspector.__generateUID();
        inspector.intents.init += inspector.__id + "/";
        inspector.sendIntent(inspector.intents.init);
        var event = document.createEvent("Event");
        event.initEvent("inspector-ready", true, true);
        window.dispatchEvent(event);
    },

    openSettings: function() {
        inspector.sendIntent(inspector.intents.settings);
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
        inspector.__id = (inspector.__id) ? inspector.__id : inspector.__generateUID();
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
                } else {
                    me.timeouts = 0;
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
                            if(l.conditions) {
                                for(var con in l.conditions) {
                                    if(!__checkConditions(itemData, con, l.conditions[con])) {
                                        alarm = false;
                                        break;
                                    }
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

        var __checkConditions = function(obj, key, value) {
            if(typeof obj === typeof {}) {
                for(var i in obj) {
                    if(typeof obj[i] === typeof {}) {
                        return __checkConditions(obj[i], key, value);
                    } else {
                        if(i === key) {
                            if(obj[i] === value) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        };
    },

    gadget : function(gadget, keep_alive) {
        var me = this;
        me.gadget = gadget;
        me.eventListener = {};
        me.basicUrl = inspector.commandAddress + gadget + '/';
        me.timeouts = 0;
        me.keep_alive = keep_alive || 200;
        me.__destroyed = false;
        me.__initial = false;
        me.__error = null;
        me.__initialQueue = [];

        me.__constructor = function() {
            me.__initial = false;
            __sendCommand({'inspector-cmd': 'initial'}, __callback);
        };

        me.__destroy = function() {
            // Send destroy command.
            delete inspector.connections[me.gadget];
            if(Object.keys(inspector.connections).length === 0) {
                inspector.state_stream.stop();
            }
            me.__initialQueue = [];
            me.eventListener = {};
            me.__initial = false;
            me.__destroyed = true;
        };

        me.on = function(event, callback, conditions) {
            if(!me.__destroyed) {
                if(!(event in me.eventListener)) {
                    me.eventListener[event] = [];
                }
                me.eventListener[event].push({
                    "id" : me.eventListener[event].length,
                    "callback" : callback,
                    "conditions" : conditions
                });
                return me.eventListener[event].length - 1;
            } else {
                if(me.__error) {
                    throw me.__error;
                }
                return false;
            }
        };

        me.off = function(event, id) {
            if(!me.__destroyed) {
                var element = me.eventListener[event].findAttribute("id", id);
                return !!(me.eventListener[event].splice(element, 1));
            } else {
                if(me.__error) {
                    throw me.__error;
                }
                return false;
            }
        };

        me.submit = function(params, callback) {
            if(!me.__destroyed) {
                if(!me.__initial) {
                    me.__initialQueue.push({"params": params, "callback": callback});
                    return;
                }
                __sendCommand(params, callback);
            } else {
                if(me.__error) {
                    throw me.__error;
                }
                return false;
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
                        __sendCommand(params, __callback);
                    }
                }
            } catch (e) {

            }
        };

        var __sendCommand = function(params, callback) {
            var cbName = inspector.__generateCallbackName();
            var url = me.basicUrl + "?callback=" + cbName;

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
                } else {
                    me.timeouts = 0;
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
            var cbName = inspector.__generateCallbackName();
            var url = me.basicUrl + "keep-alive/?callback=" + cbName + "&browserid=" + inspector.__id;

            stateScript = document.createElement("script");
            stateScript.src = url;
            stateScript.onerror = function(e) {
                me.timeouts += 1;
                stateScript.parentNode.removeChild(stateScript);
                if(me.timeouts < inspector.max_timeouts) {
                    window.setTimeout(function() {
                        __requestState();
                    }, me.timeouts * 50);
                } else {
                    me.timeouts = 0;
                }
            };
            if(!me.__destroyed) {
                window[cbName] = function(data) {
                    try {
                        if(!me.__destroyed) {
                            window.setTimeout(function() {
                                __requestState();
                            }, me.keep_alive);
                            if(me.__initial && me.__initialQueue.length > 0) {
                                for(var i=0; i<me.__initialQueue.length; i++) {
                                    var next = me.__initialQueue.splice(0, 1)[0];
                                    __sendCommand(next.params, next.callback);
                                }
                            }
                        }
                    } catch(e) {
                    } finally {
                        stateScript.parentNode.removeChild(stateScript);
                        delete window[cbName];
                    }
                };
                document.body.appendChild(stateScript);
            }
        };

        var __callback = function(response) {
            try {
                if(response.event === 'error') {
                    if(response.request && response.request['inspector-cmd']) {
                        me.__destroy();
                        me.__error = new Error(response.data.message);
                    }
                }
                if (response.data === 'initial') {
                    me.__initial = true;
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
