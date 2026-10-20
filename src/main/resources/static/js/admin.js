(function () {
  var sidebar = document.querySelector(".sidebar");
  var menuBtn = document.getElementById("menuBtn");
  var backdrop = document.getElementById("backdrop");
  var pageTitle = document.getElementById("pageTitle");
  var pageDesc = document.getElementById("pageDesc");
  var titles = {
    "#dashboard": ["대시보드", "운영 지표와 최근 가입을 확인합니다."],
    "#members": ["회원 관리", "일반 회원만 표시합니다. 관리자 계정은 이 목록에 없습니다."],
    "#pets": ["반려동물 관리", "등록된 반려동물과 주인을 확인합니다."],
    "#schedules": ["일정 관리", "일정 제목은 표시하지 않고 건수와 완료 현황만 보여 줍니다."]
  };

  function closeMenu() {
    if (!sidebar) return;
    sidebar.classList.remove("open");
    if (backdrop) backdrop.hidden = true;
  }

  if (menuBtn) {
    menuBtn.addEventListener("click", function () {
      sidebar.classList.add("open");
      if (backdrop) backdrop.hidden = false;
    });
  }
  if (backdrop) backdrop.addEventListener("click", closeMenu);

  function syncSection() {
    if (!document.body.dataset.page || document.body.dataset.page !== "dashboard") return;
    var hash = window.location.hash || "#dashboard";
    if (!titles[hash]) hash = "#dashboard";
    document.querySelectorAll("[data-nav]").forEach(function (link) {
      link.classList.toggle("is-active", link.getAttribute("data-nav") === hash.slice(1));
    });
    if (pageTitle && titles[hash]) {
      pageTitle.textContent = titles[hash][0];
      pageDesc.textContent = titles[hash][1];
    }
  }
  window.addEventListener("hashchange", syncSection);
  syncSection();

  var modal = document.getElementById("confirmModal");
  var modalText = document.getElementById("confirmText");
  var modalOk = document.getElementById("confirmOk");
  var pending = null;
  function closeModal() {
    if (modal) modal.hidden = true;
    pending = null;
  }
  document.addEventListener("submit", function (event) {
    var form = event.target;
    if (!form || !form.dataset || !form.dataset.confirm) return;
    if (form.dataset.confirmed === "1") return;
    event.preventDefault();
    pending = form;
    if (modalText) modalText.textContent = form.dataset.confirm;
    if (modal) modal.hidden = false;
  });
  if (modalOk) {
    modalOk.addEventListener("click", function () {
      if (!pending) return;
      pending.dataset.confirmed = "1";
      var form = pending;
      closeModal();
      form.submit();
    });
  }
  document.querySelectorAll("[data-close-modal]").forEach(function (btn) {
    btn.addEventListener("click", closeModal);
  });

  document.querySelectorAll("[data-inquiry-filter]").forEach(function (select) {
    select.addEventListener("change", function () {
      var value = select.value;
      var shown = 0;
      document.querySelectorAll("[data-inquiry]").forEach(function (row) {
        var visible = value === "all" || row.dataset.inquiry === value;
        row.hidden = !visible;
        if (visible) shown += 1;
      });
      var empty = document.getElementById("inquiryEmpty");
      if (empty) empty.hidden = shown !== 0;
    });
  });

  var table = document.getElementById("memberTable");
  if (!table) return;
  var rows = Array.prototype.slice.call(table.querySelectorAll("tbody tr[data-member]"));
  var search = document.getElementById("memberSearch");
  var status = document.getElementById("statusFilter");
  var role = document.getElementById("roleFilter");
  var info = document.getElementById("memberCount");
  var pager = document.getElementById("memberPager");
  var prev = document.getElementById("memberPrev");
  var next = document.getElementById("memberNext");
  var pageLabel = document.getElementById("memberPage");
  var empty = document.getElementById("memberEmpty");
  var page = 1;
  var size = 10;

  function filtered() {
    var q = (search && search.value || "").trim().toLowerCase();
    var st = status ? status.value : "all";
    var rl = role ? role.value : "all";
    return rows.filter(function (row) {
      var blob = (row.dataset.search || "").toLowerCase();
      var okQ = !q || blob.indexOf(q) >= 0;
      var okS = st === "all" || row.dataset.status === st;
      var okR = rl === "all" || row.dataset.role === rl;
      return okQ && okS && okR;
    });
  }

  function render() {
    var list = filtered();
    var pages = Math.max(1, Math.ceil(list.length / size));
    if (page > pages) page = pages;
    rows.forEach(function (row) { row.hidden = true; });
    list.forEach(function (row, index) {
      row.hidden = index < (page - 1) * size || index >= page * size;
    });
    if (info) info.textContent = list.length + "명";
    if (pageLabel) pageLabel.textContent = page + " / " + pages;
    if (prev) prev.disabled = page <= 1;
    if (next) next.disabled = page >= pages;
    if (pager) pager.hidden = list.length <= size;
    if (empty) empty.hidden = list.length !== 0;
  }

  ["input", "change"].forEach(function (type) {
    [search, status, role].forEach(function (el) {
      if (el) el.addEventListener(type, function () { page = 1; render(); });
    });
  });
  if (prev) prev.addEventListener("click", function () { page -= 1; render(); });
  if (next) next.addEventListener("click", function () { page += 1; render(); });
  render();

})();
