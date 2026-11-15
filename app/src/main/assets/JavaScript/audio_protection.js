(function () {
    // Audio fingerprint protection.
    // Adds an inaudible amount of deterministic, per-origin noise to the data returned by
    // AnalyserNode frequency/time-domain readers and AudioBuffer channel data. The classic
    // AudioContext fingerprint (oscillator -> analyser -> getFloatFrequencyData -> hash)
    // therefore changes per origin/session without affecting audible output.
    try {
        var seed = 0;
        try {
            var origin = location.origin || "";
            for (var i = 0; i < origin.length; i++) {
                seed = (seed * 31 + origin.charCodeAt(i)) | 0;
            }
        } catch (e) {}
        if (seed === 0) seed = 0x85ebca6b;

        function rand() {
            seed = (seed + 0x6D2B79F5) | 0;
            var t = seed;
            t = Math.imul(t ^ (t >>> 15), t | 1);
            t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
        }

        // Amplitude is far below the threshold of human hearing / typical analyser noise floor.
        var NOISE_AMPLITUDE = 1e-7;

        function addNoise(arr) {
            try {
                for (var i = 0; i < arr.length; i++) {
                    arr[i] = arr[i] + (rand() - 0.5) * NOISE_AMPLITUDE;
                }
            } catch (e) {}
        }

        if (typeof AnalyserNode !== "undefined" && AnalyserNode.prototype) {
            var origGetFloat = AnalyserNode.prototype.getFloatFrequencyData;
            if (origGetFloat) {
                AnalyserNode.prototype.getFloatFrequencyData = function (arr) {
                    origGetFloat.apply(this, arguments);
                    addNoise(arr);
                };
            }
            var origGetFloatTime = AnalyserNode.prototype.getFloatTimeDomainData;
            if (origGetFloatTime) {
                AnalyserNode.prototype.getFloatTimeDomainData = function (arr) {
                    origGetFloatTime.apply(this, arguments);
                    addNoise(arr);
                };
            }
        }

        if (typeof AudioBuffer !== "undefined" && AudioBuffer.prototype) {
            var origGetChannel = AudioBuffer.prototype.getChannelData;
            if (origGetChannel) {
                AudioBuffer.prototype.getChannelData = function () {
                    var arr = origGetChannel.apply(this, arguments);
                    addNoise(arr);
                    return arr;
                };
            }
        }
    } catch (e) {}
})();
