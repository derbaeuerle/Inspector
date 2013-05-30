inspector = {

    initCommand: 'inspector://init/',
    destroyCommand: 'inspector://destroy/',
    serverAddress: 'http://localhost:9090/',
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
        // TODO: Send initCommand
    },

    destroy : function() {
        // TODO: Send destroyCommand
    },

    listen : function(gadget, opts, callback) {
        if(!(gadget in inspector.listeners)) {
            inspector.listeners[gadget] = [];
        }
        id = setIntervall(inspector.call(gadget, opts, callback), 250);
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

    call : function(gadget, opts, callback) {
        // TODO: Send request to native app.
    }

};