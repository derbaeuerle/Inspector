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

        var cbName = "inspected" + Math.floor((new Date()).getTime() / Math.random());
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
        inspector.initInspector();
        console.error(e);
    }

};
window.onload = inspector.init;