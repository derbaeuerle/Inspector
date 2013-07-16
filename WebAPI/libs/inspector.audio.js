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