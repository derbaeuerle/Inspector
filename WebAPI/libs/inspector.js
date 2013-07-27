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
            inspector.connections[gadget] = new inspector.gadget(gadget, keep_alive);
        }
        return inspector.connections[gadget];
    },

    __generateCallbackName: function() {
        return "cb" + inspector.__generateUID();
    },

    __generateUID: function() {
        return parseInt((new Date()).getTime() / Math.random() * 1000, 10);
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
                            if(alarm) {
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
        me.eventListener = {};
        me.script = null;
        me.basicUrl = inspector.commandAddress + gadget + '/';
        me.timeouts = 0;
        me.keep_alive = keep_alive || 200;
        me.__destroyed = false;

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
                __sendCommand(params, callback);
            }
        };

        me.__destroy = function() {
            // Send destroy command.
            try {
                delete window[me.callbackName];
                me.callbackName = null;
            } catch(e) {}
            try {
                me.script.parentNode.removeChild(me.script);
                me.script = null;
            } catch(e) {}

            delete inspector.connections[me.gadget];
            if(Object.keys(inspector.connections).length === 0) {
                inspector.state_stream.stop();
            }
            me.__destroyed = true;
        };

        me.__errorHandler = function(data) {
            var code = parseInt(data.data.errorCode, 10);
            if(code === 2) {
                var accept = confirm("Permission request: " + me.gadget);
                if(accept) {
                    // If permission granted, send status update.
                    var params = {
                        permission: accept
                    };
                    if(data.request) {
                        params = data.request;
                        params.permission = accept;
                    }
                    // TODO: Send original params!!!!
                    __sendCommand(params, function(data) {
                        me.script.parentNode.removeChild(me.script);
                        delete window[me.callbackName];
                        me.callbackName = inspector.__generateCallbackName();
                        __requestState();
                    });
                } /* else {
                    // Else publish error to request sender.
                    if(error) {
                        error({
                            'error': {
                                'message': 'Permission denied by user!',
                                'code': -1
                            }
                        });
                    }
                } */
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

            var script = document.createElement("script");
            script.src = url;
            script.onerror = function(e) {
                me.timeouts += 1;
                script.parentNode.removeChild(me.script);
                if(me.timeouts < inspector.max_timeouts) {
                    window.setTimeout(function() {
                        __sendCommand(params, callback);
                    }, me.timeouts * 50);
                }
            };
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
                    __requestState();
                }
            };

            document.body.appendChild(script);
        };

        var __requestState = function() {
            var url = me.basicUrl + "keep-alive/?callback=" + me.callbackName + "&browserid=" + inspector.__id;
            me.script = document.createElement("script");
            me.script.src = url;
            me.script.onerror = function(e) {
                me.timeouts += 1;
                me.script.parentNode.removeChild(me.script);
                if(me.timeouts < inspector.max_timeouts) {
                    window.setTimeout(function() {
                        __requestState();
                    }, me.timeouts * 50);
                }
            };
            if(me.callbackName) {
                window[me.callbackName] = __callback;
                document.body.appendChild(me.script);
            }
        };

         var __callback = function(data) {
            try {
                if(data.event === 'error') {
                    me.__errorHandler(data);
                } else {
                    me.callbackName = inspector.__generateCallbackName();
                    // Send new state to avoid gadget timeout.
                    window.setTimeout(function() {
                        __requestState();
                    }, me.keep_alive);
                }
            } catch(e) {
            } finally {
                me.script.parentNode.removeChild(me.script);
            }
        };

        __requestState();

    }

};

window.addEventListener("load", inspector.init);
