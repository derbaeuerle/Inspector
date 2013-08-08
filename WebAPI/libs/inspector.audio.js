inspector.audio = {

    template: '<small class="state">unknown</small>&nbsp;&nbsp;' +
              '<span class="position">00:00</span>/<span class="duration">00:00</span>&nbsp;&nbsp;&nbsp;' +
              '<span class="btn play" inspector-action="play" onclick="inspector.audio.onClick(this);" href="#">Play</span>&nbsp;' +
              '<span class="btn pause" inspector-action="pause" onclick="inspector.audio.onClick(this);" href="#">Pause</span>&nbsp;' +
              '<span class="btn stop" inspector-action="stop" onclick="inspector.audio.onClick(this);" href="#">Stop</span>',
    instances: {},
    elements: {},
    force: true,
    gadget: null,

    onClick: function(el) {
        var action = el.getAttribute("inspector-action");
        var file = el.parentNode.getAttribute("src");
        var playerid = el.parentNode.id;
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
            'playerid': playerid,
            'loop': !!el.getAttribute('loop') || el.getAttribute('loop') === ""
        };
        inspector.audio.gadget.submit(params);

        if(action !== 'stop') {
            inspector.audio.instances[playerid] = {
                id: playerid,
                timeout: 2
            };
        } else {
            delete inspector.audio.instances[playerid];
            inspector.audio.updateState({
                playerid: playerid,
                state: 'STOPPED'
            });
        }

        // Dispatch event, what was done!
        var event = document.createEvent("Event");
        event.initEvent('action', true, true);
        event.detail = params;
        el.dispatchEvent(event);

        inspector.audio.checkInstances();
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
                        inspector.audio.instances[playerid].timeout = 2;
                        if(typeof(item) === typeof({})) {
                            if(item.state === 'ERROR') {
                                inspector.audio.onError(item);
                            }
                            inspector.audio.updateState(item);
                        }
                    }
                }
                inspector.audio.checkInstances();
            });
        }
    },

    onError: function(data) {
        var el = inspector.audio.elements[data.playerid];
        if(!el.getAttribute('dispatched')) {
            var event = document.createEvent("Event");
            event.initEvent('error', true, true);
            el.dispatchEvent(event);

            el.setAttribute('dispatched', 'true');
        }
    },

    checkInstances: function() {
        if(!Object.keys(inspector.audio.instances).length) {
            inspector.audio.gadget.__destroy();
            inspector.audio.gadget = null;
        }
    },

    updateState: function(data) {
        if(data) {
            var id = data.playerid;
            var el = inspector.audio.elements[id];
            if(el.className.indexOf('error') === -1 && (data.state.toLowerCase() !== 'buffering' || data.state.toLowerCase() !== 'playing')) {
                el.className = data.state.toLowerCase();
            }
            el.getElementsByClassName("position")[0].innerHTML = inspector.audio.milliToTime(data.position);
            el.getElementsByClassName("duration")[0].innerHTML = inspector.audio.milliToTime(data.duration);
            if(data.state === "COMPLETED" || data.state === "STOPPED" || data.state === "ERROR") {
                delete inspector.audio.instances[id];
            }
        }
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

        return ((hours) ? hours + ':' : '') + ((minutes) ? ((minutes < 10) ? '0' + minutes : minutes) + ':' : '00:') + ((seconds) ? ((seconds < 10) ? '0' + seconds : seconds) : '00');
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
            if(el.click) {
                el.click();
            } else if(el.onclick) {
                el.onclick();
            }
        }
    },

    stop: function() {
        var el = this.getElementsByClassName("stop")[0];
        if(el) {
            if(el.click) {
                el.click();
            } else if(el.onclick) {
                el.onclick();
            }
        }
    },

    pause: function() {
        var el = this.getElementsByClassName("pause")[0];
        if(el) {
            if(el.click) {
                el.click();
            } else if(el.onclick) {
                el.onclick();
            }
        }
    },

    update: function(data) {
        var el = this.getElementsByClassName("state")[0];
        if(el) {
            el.innerHTML = data.state || 'unknown';
        }
    },

    seek: function(to) {
        if(inspector.audio.gadget) {
            inspector.audio.gadget.submit({
                "audiofile" : this.getAttribute('src'),
                "do" : "seek",
                "playerid" : this.getAttribute('inspector-playerid'),
                "seekto" : to
            }, function(response) {});
        }
    },

    volume: function(volume) {
        if(inspector.audio.gadget) {
            inspector.audio.gadget.submit({
                "audiofile" : this.getAttribute('src'),
                "do" : "volume",
                "playerid" : this.getAttribute('inspector-playerid'),
                "volume" : volume
            }, function(response) {});
        }
    },

    leftVolume: function(volume) {
        if(inspector.audio.gadget) {
            inspector.audio.gadget.submit({
                "audiofile" : this.getAttribute('src'),
                "do" : "volume",
                "playerid" : this.getAttribute('inspector-playerid'),
                "volume-left" : volume
            }, function(response) {});
        }
    },

    rightVolume: function(volume) {
        if(inspector.audio.gadget) {
            inspector.audio.gadget.submit({
                "audiofile" : this.getAttribute('src'),
                "do" : "volume",
                "playerid" : this.getAttribute('inspector-playerid'),
                "volume-right" : volume
            }, function(response) {});
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
                    if(audio.attributes[o].value === "") {
                        iaudio.setAttribute(audio.attributes[o].name, "true");
                    } else {
                        iaudio.setAttribute(audio.attributes[o].name, audio.attributes[o].value);
                    }
                }

                iaudio.play = inspector.audio.play;
                iaudio.pause = inspector.audio.pause;
                iaudio.stop = inspector.audio.stop;
                iaudio.update = inspector.audio.update;

                iaudio.seek = inspector.audio.seek;
                iaudio.volume = inspector.audio.volume;
                iaudio.leftVolume = inspector.audio.leftVolume;
                iaudio.rightVolume = inspector.audio.rightVolume;

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
