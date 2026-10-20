(function () {
  const root = document.querySelector("[data-pet-profile]");
  if (!root) return;

  const nameInput = root.querySelector("[name=name]");
  const emojiInput = root.querySelector("[name=profileEmoji]");
  const fileInput = root.querySelector("[name=photo]");
  const preview = root.querySelector("[data-preview]");
  const previewImage = root.querySelector("[data-preview-image]");
  const previewText = root.querySelector("[data-preview-text]");
  const photoError = root.querySelector("[data-photo-error]");
  const radios = root.querySelectorAll("[name=displayType]");
  const maxBytes = 5 * 1024 * 1024;
  const allowed = ["image/jpeg", "image/png", "image/webp"];

  function mode() {
    const selected = Array.from(radios).find((radio) => radio.checked);
    return selected ? selected.value : "INITIAL";
  }

  function firstGrapheme(value) {
    const text = (value || "").trim();
    if (!text) return "?";
    if (window.Intl && Intl.Segmenter) {
      const segment = new Intl.Segmenter(undefined, { granularity: "grapheme" }).segment(text)[Symbol.iterator]().next();
      return segment.done ? "?" : segment.value.segment;
    }
    return Array.from(text)[0] || "?";
  }

  function showPhoto(url) {
    previewImage.hidden = false;
    previewImage.src = url;
    preview.classList.add("has-photo");
    previewText.textContent = "";
  }

  function showText(text) {
    previewImage.hidden = true;
    previewImage.removeAttribute("src");
    preview.classList.remove("has-photo");
    previewText.textContent = text;
  }

  function render() {
    const selected = mode();
    root.dataset.mode = selected;
    root.querySelectorAll("[data-panel]").forEach((panel) => {
      panel.hidden = panel.dataset.panel !== selected;
    });
    if (selected === "EMOJI") {
      showText((emojiInput && emojiInput.value.trim()) || "🐶");
      return;
    }
    if (selected === "INITIAL") {
      showText(firstGrapheme(nameInput.value));
      return;
    }
    const file = fileInput && fileInput.files && fileInput.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = () => showPhoto(String(reader.result));
      reader.readAsDataURL(file);
      return;
    }
    if (root.dataset.existingPhoto) {
      showPhoto(root.dataset.existingPhoto);
      return;
    }
    showText("사진");
  }

  radios.forEach((radio) => radio.addEventListener("change", render));
  nameInput.addEventListener("input", render);
  if (emojiInput) emojiInput.addEventListener("input", render);
  if (fileInput) {
    fileInput.addEventListener("change", () => {
      photoError.textContent = "";
      const file = fileInput.files && fileInput.files[0];
      if (!file) {
        render();
        return;
      }
      if (file.size > maxBytes) {
        fileInput.value = "";
        photoError.textContent = "사진 파일은 5MB 이하만 올릴 수 있습니다.";
      } else if (file.type && allowed.indexOf(file.type) === -1) {
        fileInput.value = "";
        photoError.textContent = "JPG, PNG, WebP 파일만 올릴 수 있습니다.";
      }
      render();
    });
  }
  root.addEventListener("submit", (event) => {
    if (mode() !== "PHOTO" || !fileInput || !fileInput.files || !fileInput.files[0]) return;
    const file = fileInput.files[0];
    if (file.size > maxBytes || (file.type && allowed.indexOf(file.type) === -1)) {
      event.preventDefault();
    }
  });
  render();
})();
