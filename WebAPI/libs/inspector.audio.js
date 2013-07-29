inspector.audio = {

    template: '<small class="state">unknown</small>&nbsp;&nbsp;' +
              '<span class="position">00:00</span>/<span class="duration">00:00</span>&nbsp;&nbsp;&nbsp;' +
              '<span class="play" inspector-action="play" onclick="inspector.audio.onClick(this);" href="#">Play</span>&nbsp;' +
              '<span class="pause" inspector-action="pause" onclick="inspector.audio.onClick(this);" href="#">Pause</span>&nbsp;' +
              '<span class="stop" inspector-action="stop" onclick="inspector.audio.onClick(this);" href="#">Stop</span>',
    instances: {},
    elements: {},
    force: true,
    gadget: null,

    onClick: function(el) {
        var action = el.getAttribute("inspector-action");
        var file = el.parentNode.getAttribute("src");
        var playerid = el.parentNode.id;

        var actives = el.parentNode.getElementsByClassName("active");
        for(var i=0; i<actives.length; i++) {
            var act = actives[i];
            act.className = act.className.replace(new RegExp('(\\s|^)active(\\s|$)'),' ').replace(/^\s+|\s+$/g, '');
        }
        if(!new RegExp('active').test(el.className)) {
            el.className += " active";
        }
        el = el.parentNode;

        // Change relative path of source to absolute path.
        if(file.indexOf('http') === -1) {
            var loc = window.location.href;
            loc = loc.substr(0, loc.lastIndexOf('/') + 1).replace(window.location.hash,"").replace('#', '');
            file = (loc.indexOf('/', loc.length - 1)) ? loc + file : loc + '/' + file;
        }
        file = encodeURIComponent(file);
        
        if(!inspector.audio.gadget) {
            inspector.audio.startStream();
        }

        var params = {
            'do': action,
            'audiofile': file,
            'playerid': playerid
        };
        inspector.audio.gadget.submit(params);

        if(action !== 'stop') {
            inspector.audio.instances[playerid] = playerid;
        } else {
            delete inspector.audio.instances[playerid];
            inspector.audio.updateState({
                playerid: playerid,
                state: 'STOPPED'
            });
        }
        if(!Object.keys(inspector.audio.instances).length) {
            inspector.audio.gadget.__destroy();
            inspector.audio.gadget = null;
        }
    },

    startStream: function() {
        if(!inspector.audio.gadget) {
            inspector.audio.gadget = inspector.use("AUDIO");
            inspector.audio.gadget.on(inspector.events.data, function(response) {
                for(var playerid in response.data) {
                    var item = response.data[playerid];

                    // Check if playerid is in this instances.
                    if(inspector.audio.instances[playerid]) {
                        // Check if response was messed up.
                        if(typeof(item) === typeof({})) {
                            inspector.audio.updateState(item);
                        }
                    }
                }
            });
        }
    },

    updateState: function(data) {
        var id = data.playerid;
        var el = inspector.audio.elements[id];
        var state = el.getElementsByClassName("state")[0];
        state.className = "state " + data.state.toLowerCase();
        state.innerHTML = data.state;
        el.getElementsByClassName("position")[0].innerHTML = inspector.audio.milliToTime(data.position);
        el.getElementsByClassName("duration")[0].innerHTML = inspector.audio.milliToTime(data.duration);
    },

    milliToTime: function(ms) {
        if(!ms) {
            return '00:00';
        }
        x = ms / 1000
        seconds = parseInt(x % 60, 10);
        x /= 60
        minutes = parseInt(x % 60, 10);
        x /= 60
        hours = parseInt(x % 24, 10);

        return ((hours) ? hours + ':' : '') + ((minutes) ? minutes + ':' : '00:') + ((seconds) ? ((seconds < 10) ? '0' + seconds : seconds) : '00');
    },

    needReplacement: function(el) {
        var srcs = el.getElementsByTagName('source');
        for(var x=0; x<srcs.length; x++) {
            if(el.canPlayType(srcs[x].getAttribute('type'))) {
                return false;
            }
        }
        return true;
    },

    play: function() {
        var el = this.getElementsByClassName("play")[0];
        if(el) {
            el.click();
        }
    },

    stop: function() {
        var el = this.getElementsByClassName("stop")[0];
        if(el) {
            el.click();
        }
    },

    pause: function() {
        var el = this.getElementsByClassName("pause")[0];
        if(el) {
            el.click();
        }
    },

    update: function(data) {
        var el = this.getElementsByClassName("state")[0];
        if(el) {
            el.innerHTML = data.state || 'unknown';
        }
    },

    check: function() {
        var audios = document.getElementsByTagName('audio');
        for(var i=0; i<audios.length; i++) {
            var audio = audios[i];
            if(inspector.audio.force || inspector.audio.needReplacement(audio)) {
                var id = parseInt((new Date()).getTime() / Math.random(), 10);
                id = 'iaudio' + id;
                var iaudio = document.createElement('inspector-audio');
                iaudio.id = id;
                iaudio.setAttribute("inspector-playerid", id);
                iaudio.setAttribute("src", audio.getElementsByTagName('source')[0].getAttribute('src'));
                iaudio.innerHTML = inspector.audio.template;

                for(var o=0;o<audio.attributes.length;o++) {
                    iaudio.setAttribute(audio.attributes[o].name, audio.attributes[o].value);
                }

                iaudio.play = inspector.audio.play;
                iaudio.pause = inspector.audio.pause;
                iaudio.stop = inspector.audio.stop;
                iaudio.update = inspector.audio.update;

                /*iaudio.seek = function(to) {
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
                };*/

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
window.addEventListener("inspector-ready", inspector.audio.init);
