(function () {
    // Canvas fingerprint protection.
    // Adds a tiny, per-origin deterministic perturbation to canvas pixel readbacks
    // (toDataURL / toBlob / getImageData / OffscreenCanvas.convertToBlob) so that the
    // fingerprint hash is stable for a given site but does not reveal the real device.
    // Inspired by Brave's "farbling" and DuckDuckGo's canvas fingerprint protection.
    // The noise is sub-visual (a handful of pixels shifted by +/-1 in one channel).
    try {
        var seed = 0;
        try {
            var origin = location.origin || "";
            for (var i = 0; i < origin.length; i++) {
                seed = (seed * 31 + origin.charCodeAt(i)) | 0;
            }
        } catch (e) {}
        if (seed === 0) seed = 0x9e3779b9;

        function rand() {
            // Mulberry32 PRNG — deterministic per origin/session.
            seed = (seed + 0x6D2B79F5) | 0;
            var t = seed;
            t = Math.imul(t ^ (t >>> 15), t | 1);
            t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
            return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
        }

        function perturb(data, length) {
            // Flip a small fraction of pixels by +/-1 in the red channel only.
            var pixelCount = (length / 4) | 0;
            var count = Math.max(1, Math.floor(pixelCount * 0.03));
            for (var i = 0; i < count; i++) {
                var idx = (Math.floor(rand() * pixelCount)) * 4;
                if (idx + 3 < length) {
                    data[idx] = (data[idx] + (rand() < 0.5 ? 1 : -1) + 256) & 255;
                }
            }
        }

        function copyWithNoise(sourceCanvas) {
            var w = sourceCanvas.width;
            var h = sourceCanvas.height;
            if (w <= 0 || h <= 0) return null;
            var tmp = document.createElement("canvas");
            tmp.width = w;
            tmp.height = h;
            var tctx = tmp.getContext("2d");
            tctx.drawImage(sourceCanvas, 0, 0);
            try {
                var img = tctx.getImageData(0, 0, w, h);
                perturb(img.data, img.data.length);
                tctx.putImageData(img, 0, 0);
            } catch (e) {}
            return tmp;
        }

        if (typeof HTMLCanvasElement !== "undefined") {
            var origToDataURL = HTMLCanvasElement.prototype.toDataURL;
            if (origToDataURL) {
                HTMLCanvasElement.prototype.toDataURL = function () {
                    try {
                        var tmp = copyWithNoise(this);
                        if (tmp) return origToDataURL.apply(tmp, arguments);
                    } catch (e) {}
                    return origToDataURL.apply(this, arguments);
                };
            }
            var origToBlob = HTMLCanvasElement.prototype.toBlob;
            if (origToBlob) {
                HTMLCanvasElement.prototype.toBlob = function () {
                    try {
                        var tmp = copyWithNoise(this);
                        if (tmp) return origToBlob.apply(tmp, arguments);
                    } catch (e) {}
                    return origToBlob.apply(this, arguments);
                };
            }
        }

        if (typeof CanvasRenderingContext2D !== "undefined") {
            var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;
            if (origGetImageData) {
                CanvasRenderingContext2D.prototype.getImageData = function () {
                    var img = origGetImageData.apply(this, arguments);
                    try { perturb(img.data, img.data.length); } catch (e) {}
                    return img;
                };
            }
        }

        if (typeof OffscreenCanvas !== "undefined" && OffscreenCanvas.prototype) {
            var origConvertToBlob = OffscreenCanvas.prototype.convertToBlob;
            if (origConvertToBlob) {
                OffscreenCanvas.prototype.convertToBlob = function () {
                    try {
                        var ctx = this.getContext("2d");
                        if (ctx && this.width > 0 && this.height > 0) {
                            var img = ctx.getImageData(0, 0, this.width, this.height);
                            perturb(img.data, img.data.length);
                            ctx.putImageData(img, 0, 0);
                        }
                    } catch (e) {}
                    return origConvertToBlob.apply(this, arguments);
                };
            }
        }
    } catch (e) {}
})();
