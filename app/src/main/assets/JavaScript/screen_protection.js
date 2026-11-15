(function () {
    // Screen / display fingerprint protection.
    // Reduces the granularity of screen geometry and pixel-depth values that sites use for
    // fingerprinting, without breaking layout. Dimensions are bucketed to common multiples
    // and devicePixelRatio is rounded to the nearest 0.5 (capped at 3). innerWidth/innerHeight
    // are intentionally left untouched because they change on every resize and overriding
    // them breaks responsive layouts and the app's own scroll/nav detection.
    try {
        function roundUp(v, step) { return Math.max(step, Math.ceil(v / step) * step); }

        var realW = screen.width || 0;
        var realH = screen.height || 0;
        var realAW = screen.availWidth || realW;
        var realAH = screen.availHeight || realH;

        var sw = realW > 0 ? roundUp(realW, 50) : realW;
        var sh = realH > 0 ? roundUp(realH, 50) : realH;
        var saw = realAW > 0 ? roundUp(realAW, 50) : realAW;
        var sah = realAH > 0 ? roundUp(realAH, 50) : realAH;

        function defineProp(obj, prop, val) {
            try {
                Object.defineProperty(obj, prop, { get: function () { return val; }, configurable: true });
            } catch (e) {}
        }

        defineProp(screen, "width", sw);
        defineProp(screen, "height", sh);
        defineProp(screen, "availWidth", saw);
        defineProp(screen, "availHeight", sah);
        try { defineProp(screen, "availLeft", 0); } catch (e) {}
        try { defineProp(screen, "availTop", 0); } catch (e) {}
        defineProp(screen, "colorDepth", 24);
        defineProp(screen, "pixelDepth", 24);

        var dpr = window.devicePixelRatio || 1;
        var fakeDpr = Math.round(dpr * 2) / 2;
        if (fakeDpr < 1) fakeDpr = 1;
        if (fakeDpr > 3) fakeDpr = 3;
        defineProp(window, "devicePixelRatio", fakeDpr);
    } catch (e) {}
})();
