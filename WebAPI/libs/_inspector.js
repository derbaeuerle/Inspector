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

    /*
     * {
     *     callback: function(data) { ... },
     *     error: function(e) { ... },
     *     params: { ... },
     *     delay: 100,
     *     gadget: 'AUDIO'
     * }
     */
    connect : function(opts) {
        if(!opts) {
            throw new Error("No options set!");
        }
        do {
            streamId = "inspector" + parseInt((new Date()).getTime() / Math.random() * 1000, 10);
            opts.streamid = streamId;
        } while (opts.streamid in inspector.listeners);
        inspector.listeners[opts.streamid] = true;

        if(!('callback' in opts)) {
            throw new Error("No callback set!");
        }
        if(!('gadget' in opts)) {
            throw new Error("No gadget set!");
        }
        if(!('delay' in opts)) {
            opts.delay = 10;
        }

        con = new inspector.connection(opts);
        console.log(con);
        return con;
    },

    connection : function(opts) {
        this.id = opts.streamid;
        this.gadget = opts.gadget;
        this.opts = opts.params;
        this.delay = opts.delay;
        this.callback = opts.callback;
        this.error = opts.error || function(e) {};
        this.stopped = false;

        this.call = function(aOpts) {
            if(!this.stopped) {
                if(!this.cbName || !window[this.cbName]) {
                    this.__setCallback();
                }
                if(!this.url) {
                    this.__generateURI(aOpts);
                }

                this.sc = document.createElement("script");
                if(this.error) {
                    this.sc.onerror = this.error;
                }

                this.sc.src = this.url;
                document.body.appendChild(this.sc);
            }
        };

        this.__setCallback = function() {
            var that = this;
            do {
                var cbName = "ins" + parseInt((new Date()).getTime() / Math.random() * 1000, 10);
            } while (window[cbName]);
            that.cbName = cbName;

            window[cbName] = function(data) {
                try {
                    if(data['error']) {
                        // Checks for specific error codes.
                        if(data.error['errorCode']) {
                            var eCode = data.error.errorCode;
                            // Check error code for permission request.
                            if(eCode == 2) {
                                window.setTimeout(function() {
                                    inspector.requestPermission(that);
                                }, 100);
                            }
                        } else {
                            // Check if error function has been submitted.
                            if(that.error) {
                                that.error(data);
                            }
                        }
                    } else {
                        // Check if call was an stream.
                        if(that.id) {
                            inspector.logger(that.id);
                            that.callback(data);

                            window.setTimeout(function() {
                                that.call();
                            }, that.delay);
                        }
                        if('stream' in data && data.stream.streamid == that.id) {
                            that.callback(data);
                            
                            window.setTimeout(function() {
                                that.call();
                            }, that.delay);
                        } else {
                            // Call callback of call request.
                            window.setTimeout(function() {
                                that.callback(data);
                            }, 100);
                        }
                        // inspector.logger(JSON.stringify(response));
                    }
                } finally {
                    that.sc.parentNode.removeChild(that.sc);
                }
            }
        };

        this.__generateURI = function(aOpts) {
            url = inspector.serverAddress + this.gadget + "/";

            url += "?callback=inspector." + this.cbName;
            for(key in this.opts) {
                if(this.opts.hasOwnProperty(key)) {
                    url += "&" + key + "=" + this.opts[key];
                }
            }
            for(key in aOpts) {
                if(aOpts.hasOwnProperty(key)) {
                    url += "&" + key + "=" + aOpts[key];
                }
            }

            this.url = url;
            this.url = "http://api.flickr.com/services/feeds/photos_public.gne?jsoncallback=" + this.cbName + "&format=json";
        }

        this.__destroy = function() {
            this.stopped = true;
            try {
                if(this.cbName) {
                    delete window[this.cbName];
                    this.cbName = null;
                    this.url = null;
                }
            } catch(e) {}
            try {
                if(this.sc) {
                    this.sc.parentNode.removeChild(this.sc);
                    this.sc = null;
                }
            } catch(e) {}
        };
    },

    requestPermission : function(con) {
        var accept = confirm("Permission request: " + con.gadget);
        if(accept) {
            // If permission granted, send status update.
            opts['permission'] = accept;
            window.setTimeout(function() {
                con.call({ 'permission' : accept });
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

// addLoadEvent(inspector.init);