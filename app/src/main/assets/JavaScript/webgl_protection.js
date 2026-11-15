(function () {
    // WebGL fingerprint protection.
    // Masks the UNMASKED_VENDOR_WEBGL / UNMASKED_RENDERER_WEBGL strings exposed by the
    // WEBGL_debug_renderer_info extension so GPU-specific fingerprints cannot be derived.
    // The extension object is still returned (so sites don't error), but getParameter for
    // the unmasked constants returns generic SwiftShader values instead of the real GPU.
    // Modeled on Brave's WebGL spoofing shield.
    try {
        var FAKE_VENDOR = "Google Inc. (Google)";
        var FAKE_RENDERER = "ANGLE (Google, Vulkan 1.3.0 (SwiftShader Device (Subzero)))";
        var UNMASKED_VENDOR_WEBGL = 37445;
        var UNMASKED_RENDERER_WEBGL = 37446;

        function patch(proto) {
            if (!proto) return;
            var origGetParameter = proto.getParameter;
            if (origGetParameter) {
                proto.getParameter = function (p) {
                    try {
                        if (p === UNMASKED_VENDOR_WEBGL) return FAKE_VENDOR;
                        if (p === UNMASKED_RENDERER_WEBGL) return FAKE_RENDERER;
                    } catch (e) {}
                    return origGetParameter.apply(this, arguments);
                };
            }
        }

        if (typeof WebGLRenderingContext !== "undefined") {
            patch(WebGLRenderingContext.prototype);
        }
        if (typeof WebGL2RenderingContext !== "undefined") {
            patch(WebGL2RenderingContext.prototype);
        }
    } catch (e) {}
})();
