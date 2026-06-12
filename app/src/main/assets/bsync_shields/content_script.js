// B-Sync Native Anti-Popup Shield

// 1. Defuse native dialogs often abused by scareware
const noop = () => console.log("[B-Sync Shields] Blocked abusive dialog");
window.alert = noop;
window.confirm = () => { console.log("[B-Sync Shields] Blocked confirm dialog"); return false; };

// 2. Continually hunt and destroy scareware overlay elements
const badKeywords = [
    "clean your device",
    "virus detected",
    "your device is infected",
    "system warning",
    "clean now"
];

function scanAndDestroyScareware() {
    // Target high z-index elements or fixed overlays
    const elements = document.querySelectorAll('div, iframe, section');
    for (let el of elements) {
        try {
            const style = window.getComputedStyle(el);
            if (style.zIndex > 1000 || style.position === 'fixed' || style.position === 'absolute') {
                const text = el.textContent.toLowerCase();
                if (badKeywords.some(keyword => text.includes(keyword))) {
                    console.log("[B-Sync Shields] Destroyed scareware element!");
                    el.remove();
                }
            }
        } catch (e) {
            // Ignore cross-origin iframe styling errors
        }
    }
}

// Run the scan periodically to catch aggressively re-injected elements
setInterval(scanAndDestroyScareware, 1000);

// Also run on DOM mutations for instant destruction
const observer = new MutationObserver((mutations) => {
    scanAndDestroyScareware();
});
observer.observe(document.documentElement, { childList: true, subtree: true });
