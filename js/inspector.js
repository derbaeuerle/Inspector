inspector = new function() {
    this.id = 0;

    function deleteInterval(cbName) {
        clearInterval(cbName);
    }

    function loop(url, params, onSuccess, onError, interval) {
        url = url;
        onSuccess = onSuccess;
        params = params;
        onError = onError;

        cbName = "inspect" + this.id++;

        setInterval(function() {
            this.getJSON(url, params, onSuccess, onError, cbName);
        }, interval);

        return cbName;
    }

    function getJSON(url, params, onSuccess, onError, cbName) {
        url = url;
        onSuccess = onSuccess;
        params = params;
        onError = onError;
        if(cbName === null || cbName === undefined) {
            cbName = "inspect" + this.id++;
        }

        if(url.indexOf('?') === -1) {
            url += '?callback=' + cbName;
        } else {
            url += '&callback=' + cbName;
        }

        for(var key in params) {
            if(params.hasOwnProperty(key)) {
                url += "&" + key + "=" + params[key];
            }
        }

        script = document.createElement("script");
        script.onError = onError;
        this[cbName] = function(response) {
            try {
                onSuccess(response);
            }
            finally {
                delete this[cbName];
                script.parentNode.removeChild(script);
            }
        };
        script.src = url;
        document.body.appendChild(script);
    }
};