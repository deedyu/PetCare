(function () {
  var sidebar = document.getElementById("sidebar");
  var menuBtn = document.getElementById("menuBtn");
  var backdrop = document.getElementById("backdrop");

  function closeMenu() {
    if (!sidebar) return;
    sidebar.classList.remove("open");
    if (backdrop) backdrop.hidden = true;
  }

  if (menuBtn && sidebar) {
    menuBtn.addEventListener("click", function () {
      var open = sidebar.classList.toggle("open");
      if (backdrop) backdrop.hidden = !open;
    });
  }
  if (backdrop) backdrop.addEventListener("click", closeMenu);

  document.querySelectorAll("form[data-task-form]").forEach(function (form) {
    var dateToggle = form.querySelector("#dateToggle");
    var timeToggle = form.querySelector("#timeToggle");
    var datePicker = form.querySelector("#dueDate");
    var timePicker = form.querySelector("#dueTime");
    var repeatSelect = form.querySelector("#repeatType");
    var weekdays = form.querySelector("[data-weekdays]");

    function syncDate() {
      if (!dateToggle || !datePicker) return;
      datePicker.disabled = !dateToggle.checked;
    }
    function syncTime() {
      if (!timeToggle || !timePicker) return;
      timePicker.disabled = !timeToggle.checked;
    }
    function syncWeekdays() {
      if (!repeatSelect || !weekdays) return;
      weekdays.hidden = repeatSelect.value !== "WEEKDAYS";
    }
    if (dateToggle) dateToggle.addEventListener("change", syncDate);
    if (timeToggle) timeToggle.addEventListener("change", syncTime);
    if (repeatSelect) repeatSelect.addEventListener("change", syncWeekdays);
    syncDate();
    syncTime();
    syncWeekdays();
    form.addEventListener("submit", function () {
      if (datePicker && datePicker.disabled) datePicker.removeAttribute("name");
      if (timePicker && timePicker.disabled) timePicker.removeAttribute("name");
    });
  });
})();
