document.querySelectorAll("[data-toggle-secret]").forEach((button) => {
  button.addEventListener("click", () => {
    const input = document.getElementById(button.getAttribute("data-toggle-secret"));
    if (!input) return;
    const show = input.type === "password";
    input.type = show ? "text" : "password";
    button.textContent = show ? "숨기기" : "표시";
  });
});

const username = document.getElementById("username");
const checkButton = document.getElementById("checkUsername");
const usernameStatus = document.getElementById("usernameStatus");
let checkedValue = "";

if (username && checkButton && usernameStatus) {
  username.addEventListener("input", () => {
    checkedValue = "";
    usernameStatus.textContent = "아이디를 바꾸면 중복 확인을 다시 해야 합니다.";
    usernameStatus.className = "hint";
  });
  checkButton.addEventListener("click", async () => {
    const value = username.value.trim();
    usernameStatus.textContent = "확인 중입니다.";
    usernameStatus.className = "hint";
    try {
      const response = await fetch("/register/username-check?username=" + encodeURIComponent(value));
      const body = await response.json();
      usernameStatus.textContent = body.message || "아이디를 확인할 수 없습니다.";
      usernameStatus.className = body.available ? "field-ok" : "field-error";
      checkedValue = body.available ? value : "";
    } catch (error) {
      usernameStatus.textContent = "아이디를 확인하지 못했습니다. 다시 시도해 주세요.";
      usernameStatus.className = "field-error";
      checkedValue = "";
    }
  });
}

const registerForm = document.getElementById("registerForm");
if (registerForm) {
  registerForm.addEventListener("submit", (event) => {
    if (username && checkedValue !== username.value.trim()) {
      event.preventDefault();
      if (usernameStatus) {
        usernameStatus.textContent = "아이디 중복 확인을 먼저 해주세요.";
        usernameStatus.className = "field-error";
      }
      return;
    }
    const button = registerForm.querySelector("[type=submit]");
    if (button && button.dataset.locked !== "1") {
      button.dataset.locked = "1";
      window.setTimeout(() => {
        button.disabled = true;
        button.textContent = "가입 중";
      }, 0);
    }
  });
}

const password = document.getElementById("password");
const confirm = document.getElementById("passwordConfirm");
const matchStatus = document.getElementById("matchStatus");

function updateMatch() {
  if (!password || !confirm || !matchStatus) return;
  if (confirm.value.length === 0) {
    matchStatus.textContent = "";
    return;
  }
  const same = password.value === confirm.value;
  matchStatus.textContent = same ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다.";
  matchStatus.className = same ? "field-ok" : "field-error";
}

if (password) password.addEventListener("input", updateMatch);
if (confirm) confirm.addEventListener("input", updateMatch);

document.querySelectorAll("form[data-lock-submit]").forEach((form) => {
  form.addEventListener("submit", () => {
    const button = form.querySelector("[type=submit]");
    if (button && button.dataset.locked !== "1") {
      button.dataset.locked = "1";
      window.setTimeout(() => {
        button.disabled = true;
      }, 0);
    }
  });
});
