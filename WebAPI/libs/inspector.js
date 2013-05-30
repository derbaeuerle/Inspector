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

    init : function() {
        window.onbeforeunload = inspector.destroyInspector;
        inspector.initInspector();
    },

    initInspector : function() {
        inspector.sendIntent(inspector.initCommand);
    },

    destroyInspector : function() {
        //inspector.sendIntent(inspector.destroyCommand);
    },

    sendIntent : function(command) {
        iframe = document.createElement("iframe");
        iframe.style.display = "none";
        iframe.onerror = function(e) {
            document.body.removeChild(iframe);
            console.log(e);
        }
        iframe.src = command;
        document.body.appendChild(iframe);
    },

    listen : function(gadget, opts, callback, onerror) {
        if(!(gadget in inspector.listeners)) {
            inspector.listeners[gadget] = [];
        }
        id = setIntervall(inspector.call(gadget, opts, callback, onerror), 250);
        inspector.listeners[gadget].push(id);
        return 0;
    },

    unlisten : function(gadget, id) {
        if(gadget in inspector.listeners) {
            if(id in inspector.listeners[gadget]) {
                gadgetListeners = inspector.listeners[gadget];
                clearIntervall(gadgetListeners[id]);
                delete(gadgetListeners[gadgetListeners.indexOf(id)]);
            }
        }
    },

    call : function(gadget, opts, callback, onerror) {
        url = inspector.serverAddress + gadget + "/";

        onError = onError;

        var cbName = "inspected" + Math.floor((new Date()).getTime() / Math.random());
        url += '?callback=' + cbName;

        for(var key in opts) {
            if(opts.hasOwnProperty(key)) {
                url += "&" + key + "=" + opts[key];
            }
        }

        script = document.createElement("script");

        script.onError = onerror || inspector.onError;
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
        console.error(e);
    }

};
window.onload = inspector.init;