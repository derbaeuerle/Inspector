inspector.audio = {

    id: 1,
    template: '<inspector-audio-state>unknown</inspector-audio-state>' +
              '<button inspector-action="play" onclick="inspector.audio.onClick(this);">Play</button>' +
              '<button inspector-action="pause" onclick="inspector.audio.onClick(this);">Pause</button>' +
              '<button inspector-action="stop" onclick="inspector.audio.onClick(this);">Stop</button>',
    instances: {},
    elements: [],
    force: true,

    onClick: function(el) {
        action = el.getAttribute("inspector-action");
        file = el.parentNode.getAttribute("src");
        playerid = el.parentNode.getAttribute("inspector-playerid");
        if(file.indexOf('http') === -1) {
            loc = window.location.href;
            file = (loc.indexOf('/', loc.length - 1)) ? loc + file : loc + '/' + file;
        }
        file = encodeURIComponent(file);
        opts = {
            "audiofile" : file,
            "do" : action //,
            //"playerid" : playerid
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
            var event = document.createEvent("Event");
            event.initEvent('state', true, true);
            event.detail = data;
            event.request = {
                message: action,
                audiofile: file,
                playerid: playerid,
                response: data
            };
            el.parentNode.dispatchEvent(event);
            inspector.audio.updateState(el.parentNode, data);
        }, null, 20);
        if(action === "play") {
            // If starts playing, safe id of stream and send states for current player state request.
            window.setTimeout(function() {
                inspector.audio.instances[playerid] = inspector.listen(inspector.gadgets.AUDIO, {
                    "do": "state",
                    "playerid": playerid
                }, function(data) {
                    var event = document.createEvent("Event");
                    event.initEvent('state', true, true);
                    event.detail = data;
                    el.parentNode.dispatchEvent(event);
                    inspector.audio.updateState(el.parentNode, data);
                });
            }, 1000);
        } else {
            // Else remove player state request from streams.
            window.setTimeout(function() {
                inspector.unlisten(inspector.gadgets.AUDIO, inspector.audio.instances[playerid]);
            }, 500);
        }

    },

    updateState: function(el, response) {
        sEl = el.getElementsByTagName('inspector-audio-state');
        if(sEl.length > 0) {
            sEl = sEl[0];
            sEl.innerHTML = response.state || 'unknown';
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
                id = (new Date()).getTime() / inspector.audio.id++;
                id = id / (Math.floor(Math.random()*1001));
                id = 'iaudio' + id;
                var iaudio = document.createElement('inspector-audio');
                iaudio.setAttribute("inspector-playerid", id);
                iaudio.setAttribute("src", audio.getElementsByTagName('source')[0].getAttribute('src'));
                iaudio.innerHTML = inspector.audio.template;

                for(o=0;o<audio.attributes.length;o++) {
                    iaudio.setAttribute(audio.attributes[o].name, audio.attributes[o].value);
                }

                iaudio.play = function() {
                    el = getElementByAttribute("inspector-action", "play", iaudio);
                    if(el) {
                        el.click();
                    }
                }
                iaudio.stop = function() {
                    el = getElementByAttribute("inspector-action", "stop", iaudio);
                    if(el) {
                        el.click();
                    }
                }
                iaudio.stop = function() {
                    el = getElementByAttribute("inspector-action", "pause", iaudio);
                    if(el) {
                        el.click();
                    }
                }
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
                }
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
                }

                audio.parentNode.replaceChild(iaudio, audio);
                inspector.audio.elements.push(iaudio);
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