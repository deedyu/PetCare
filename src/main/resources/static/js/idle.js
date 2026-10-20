(function () {
  var seconds = Number(document.querySelector('meta[name="idle-seconds"]')?.content || "0");
  var token = document.querySelector('meta[name="csrf-token"]')?.content;
  var header = document.querySelector('meta[name="csrf-header"]')?.content;
  if (!seconds || !token || !header) return;

  var timer = 0;
  function arm() {
    window.clearTimeout(timer);
    timer = window.setTimeout(expire, seconds * 1000);
  }

  function expire() {
    var headers = {};
    headers[header] = token;
    headers["X-PetCare-Background"] = "1";
    fetch("/session/idle", { method: "POST", headers: headers, credentials: "same-origin" })
      .finally(function () {
        window.location.replace("/login?idle");
      });
  }

  ["pointerdown", "keydown", "input", "submit"].forEach(function (name) {
    document.addEventListener(name, arm, true);
  });
  arm();
})();
