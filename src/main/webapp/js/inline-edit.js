/**
 * - 一覧: 日付は date、時間は time 入力でセルごと編集
 * - カレンダー上の予定 / 新規: ダイアログ（日付・時間は別入力）
 */
(function () {
  const script = document.currentScript;
  const contextPath = (script && script.dataset.context) || "";
  const postUrl = contextPath + "/";
  const main = document.querySelector(".calendar-wrap");
  const dialog = document.getElementById("event-dialog");
  const form = document.getElementById("event-form");
  if (!main || !dialog || !form) {
    return;
  }

  const titleEl = document.getElementById("event-dialog-title");
  const errorEl = document.getElementById("event-error");
  const idEl = document.getElementById("event-id");
  const titleInput = document.getElementById("event-title");
  const dateInput = document.getElementById("event-date");
  const timeInput = document.getElementById("event-time");
  const descInput = document.getElementById("event-description");
  const dateEndInput = document.getElementById("event-date-end");
  const endWrap = document.getElementById("event-end-wrap");
  const deleteBtn = document.getElementById("event-delete");
  const cancelBtn = document.getElementById("event-cancel");
  const newBtn = document.getElementById("btn-new-event");

  const year = main.dataset.year;
  const month = main.dataset.month;
  let activeEditor = null;
  let dragStartDate = null;
  let dragging = false;

  // 月移動リロード時のスクロール復元
  (function restoreScroll() {
    const y = sessionStorage.getItem("cal-scroll");
    if (y == null) {
      return;
    }
    sessionStorage.removeItem("cal-scroll");
    requestAnimationFrame(function () {
      window.scrollTo(0, Number(y) || 0);
    });
  })();

  function showError(message) {
    if (!message) {
      errorEl.hidden = true;
      errorEl.textContent = "";
      return;
    }
    errorEl.hidden = false;
    errorEl.textContent = message;
  }

  function formatLocalDate(d) {
    return (
      d.getFullYear() +
      "-" +
      String(d.getMonth() + 1).padStart(2, "0") +
      "-" +
      String(d.getDate()).padStart(2, "0")
    );
  }

  function parseLocalDate(text) {
    const parts = text.split("-").map(Number);
    return new Date(parts[0], parts[1] - 1, parts[2]);
  }

  function todayText() {
    return formatLocalDate(new Date());
  }

  function isPastDate(text) {
    return !!text && text < todayText();
  }

  /** 過去日を除いた追加可能な期間。全部過去なら null */
  function clampRangeToFuture(start, end) {
    const today = todayText();
    const range = normalizeRange(start, end || start);
    if (range.end < today) {
      return null;
    }
    if (range.start < today) {
      return { start: today, end: range.end };
    }
    return range;
  }

  function normalizeRange(a, b) {
    const da = parseLocalDate(a);
    const db = parseLocalDate(b);
    if (da <= db) {
      return { start: a, end: b };
    }
    return { start: b, end: a };
  }

  function listDatesInRange(startText, endText) {
    const range = normalizeRange(startText, endText || startText);
    const from = parseLocalDate(range.start);
    const to = parseLocalDate(range.end);
    const dates = [];
    for (let d = new Date(from.getTime()); d <= to; d.setDate(d.getDate() + 1)) {
      dates.push(formatLocalDate(d));
      if (dates.length > 93) {
        break;
      }
    }
    return dates;
  }

  function clearDaySelection() {
    document.querySelectorAll(".calendar td.is-selected").forEach(function (td) {
      td.classList.remove("is-selected");
    });
  }

  function highlightRange(start, end) {
    clearDaySelection();
    if (!start) {
      return;
    }
    const range = normalizeRange(start, end || start);
    const set = {};
    listDatesInRange(range.start, range.end).forEach(function (d) {
      set[d] = true;
    });
    document.querySelectorAll(".calendar td.js-day-cell").forEach(function (td) {
      if (set[td.dataset.date]) {
        td.classList.add("is-selected");
      }
    });
  }

  function reloadMonth(dateText) {
    if (dateText && /^\d{4}-\d{2}-\d{2}$/.test(dateText)) {
      const parts = dateText.split("-");
      location.href = postUrl + "?year=" + parts[0] + "&month=" + Number(parts[1]);
      return;
    }
    location.href = postUrl + "?year=" + year + "&month=" + month;
  }

  function postForm(params) {
    const body = new URLSearchParams(params);
    body.set("ajax", "1");
    return fetch(postUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
        "X-Requested-With": "XMLHttpRequest"
      },
      body: body.toString()
    }).then(function (res) {
      return res.json().then(function (data) {
        if (!res.ok || !data.ok) {
          throw new Error((data && data.error) || "保存に失敗しました");
        }
        return data;
      });
    });
  }

  function defaultSingleDate() {
    const today = todayText();
    const y = Number(year);
    const m = Number(month);
    const first = y + "-" + String(m).padStart(2, "0") + "-01";
    const now = new Date();
    if (now.getFullYear() === y && now.getMonth() + 1 === m) {
      return today;
    }
    if (first >= today) {
      return first;
    }
    return today;
  }

  function openCreate(presetStart, presetEnd) {
    titleEl.textContent = "予定の追加";
    idEl.value = "";
    titleInput.value = "";
    const rawStart =
      presetStart && /^\d{4}-\d{2}-\d{2}$/.test(presetStart)
        ? presetStart
        : defaultSingleDate();
    const rawEnd =
      presetEnd && /^\d{4}-\d{2}-\d{2}$/.test(presetEnd) ? presetEnd : rawStart;
    const range = clampRangeToFuture(rawStart, rawEnd);
    if (!range) {
      clearDaySelection();
      alert("過去の日付には予定を追加できません");
      return;
    }
    dateInput.value = range.start;
    dateEndInput.value = range.end !== range.start ? range.end : "";
    dateInput.min = todayText();
    dateEndInput.min = todayText();
    timeInput.value = "";
    descInput.value = "";
    deleteBtn.hidden = true;
    endWrap.hidden = false;
    showError("");
    dialog.showModal();
    titleInput.focus();
  }

  function openEdit(el, focusField) {
    titleEl.textContent = "予定の編集";
    idEl.value = el.dataset.id || "";
    titleInput.value = el.dataset.title || "";
    dateInput.value = el.dataset.date || "";
    dateEndInput.value = "";
    dateInput.removeAttribute("min");
    dateEndInput.removeAttribute("min");
    timeInput.value = el.dataset.time || "";
    descInput.value = el.dataset.description || "";
    deleteBtn.hidden = !idEl.value;
    endWrap.hidden = true;
    clearDaySelection();
    showError("");
    dialog.showModal();
    if (focusField === "time") {
      timeInput.focus();
    } else if (focusField === "date") {
      dateInput.focus();
    } else {
      titleInput.focus();
    }
  }

  function displayFor(field, value) {
    if (field === "time") {
      return value ? value : "—";
    }
    return value || "";
  }

  function escapeHtml(text) {
    return String(text == null ? "" : text)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function syncRow(row, data) {
    row.dataset.title = data.title || "";
    row.dataset.date = data.eventDate || "";
    row.dataset.time = data.eventTime || "";
    row.dataset.description = data.description || "";
    row.querySelectorAll(".js-inline-edit").forEach(function (cell) {
      const field = cell.dataset.field;
      if (field === "date") {
        cell.textContent = data.eventDate || "";
      } else if (field === "time") {
        cell.textContent = displayFor("time", data.eventTime);
      } else if (field === "title") {
        cell.textContent = data.title || "";
      } else if (field === "description") {
        cell.textContent = data.description || "";
      }
    });
  }

  /** カレンダー上の同じ予定も、リロードせずに更新する */
  function syncCalendarEvent(data) {
    const id = String(data.id);
    const btn = document.querySelector('.calendar .js-open-event[data-id="' + id + '"]');
    if (!btn) {
      return;
    }
    const li = btn.closest("li");
    btn.dataset.title = data.title || "";
    btn.dataset.date = data.eventDate || "";
    btn.dataset.time = data.eventTime || "";
    btn.dataset.description = data.description || "";
    btn.innerHTML =
      (data.eventTime
        ? '<span class="ev-time">' + escapeHtml(data.eventTime) + "</span>"
        : "") +
      '<span class="ev-title">' +
      escapeHtml(data.title || "") +
      "</span>";

    const targetTd = document.querySelector(
      '.calendar td[data-date="' + data.eventDate + '"]'
    );
    if (targetTd && li) {
      const ul = targetTd.querySelector(".day-events");
      if (ul && li.parentElement !== ul) {
        ul.appendChild(li);
      }
    }
  }

  function applyEventUpdate(row, data) {
    syncRow(row, data);
    syncCalendarEvent(data);
    removeDomDuplicatesOf(data);
    sortEventList();
  }

  /** 同じ日・時間・タイトル・説明で、残す id 以外を画面から消す */
  function sameContent(el, data) {
    return (
      (el.dataset.date || "") === (data.eventDate || "") &&
      (el.dataset.time || "") === (data.eventTime || "") &&
      (el.dataset.title || "") === (data.title || "") &&
      (el.dataset.description || "") === (data.description || "")
    );
  }

  function removeDomDuplicatesOf(data) {
    if (!data || data.id == null) {
      return;
    }
    const keepId = String(data.id);
    document.querySelectorAll(".event-list tr.event-row").forEach(function (row) {
      if (String(row.dataset.id) !== keepId && sameContent(row, data)) {
        row.remove();
      }
    });
    document.querySelectorAll(".calendar .js-open-event").forEach(function (btn) {
      if (String(btn.dataset.id) !== keepId && sameContent(btn, data)) {
        const li = btn.closest("li");
        if (li) {
          li.remove();
        }
      }
    });
    // 同じ id が二重に出ている場合も1つにする
    const listRows = document.querySelectorAll(
      '.event-list tr.event-row[data-id="' + keepId + '"]'
    );
    for (let i = 1; i < listRows.length; i++) {
      listRows[i].remove();
    }
    const calBtns = document.querySelectorAll(
      '.calendar .js-open-event[data-id="' + keepId + '"]'
    );
    for (let i = 1; i < calBtns.length; i++) {
      const li = calBtns[i].closest("li");
      if (li) {
        li.remove();
      }
    }
    showEmptyListIfNeeded();
  }

  function isSameDisplayedMonth(dateText) {
    if (!dateText || !/^\d{4}-\d{2}-\d{2}$/.test(dateText)) {
      return false;
    }
    const parts = dateText.split("-");
    return Number(parts[0]) === Number(year) && Number(parts[1]) === Number(month);
  }

  function findListRow(id) {
    return document.querySelector('.event-list tr.event-row[data-id="' + id + '"]');
  }

  function bindInlineEditCells(row) {
    row.querySelectorAll(".js-inline-edit").forEach(function (cell) {
      cell.addEventListener("click", function () {
        startInlineEdit(cell);
      });
    });
  }

  function bindCalendarButton(btn) {
    btn.addEventListener("click", function (e) {
      e.preventDefault();
      e.stopPropagation();
      openEdit(btn);
    });
  }

  function createListRow(data) {
    const tr = document.createElement("tr");
    tr.className = "event-row";
    tr.dataset.id = String(data.id);
    tr.dataset.title = data.title || "";
    tr.dataset.date = data.eventDate || "";
    tr.dataset.time = data.eventTime || "";
    tr.dataset.description = data.description || "";

    const cells = [
      { field: "date", input: "date", text: data.eventDate || "" },
      { field: "time", input: "time", text: displayFor("time", data.eventTime) },
      { field: "title", input: "text", text: data.title || "" },
      { field: "description", input: "text", text: data.description || "" }
    ];
    cells.forEach(function (c) {
      const td = document.createElement("td");
      td.className = "js-inline-edit";
      td.dataset.field = c.field;
      td.dataset.input = c.input;
      td.textContent = c.text;
      tr.appendChild(td);
    });
    bindInlineEditCells(tr);
    return tr;
  }

  function createCalendarItem(data) {
    const li = document.createElement("li");
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "ev-link js-open-event";
    btn.dataset.id = String(data.id);
    btn.dataset.title = data.title || "";
    btn.dataset.date = data.eventDate || "";
    btn.dataset.time = data.eventTime || "";
    btn.dataset.description = data.description || "";
    btn.innerHTML =
      (data.eventTime
        ? '<span class="ev-time">' + escapeHtml(data.eventTime) + "</span>"
        : "") +
      '<span class="ev-title">' +
      escapeHtml(data.title || "") +
      "</span>";
    bindCalendarButton(btn);
    li.appendChild(btn);
    return li;
  }

  function ensureListTbody() {
    const tbody = document.querySelector(".event-list tbody");
    if (!tbody) {
      return null;
    }
    const emptyRow = tbody.querySelector("td[colspan]");
    if (emptyRow) {
      emptyRow.closest("tr").remove();
    }
    return tbody;
  }

  function showEmptyListIfNeeded() {
    const tbody = document.querySelector(".event-list tbody");
    if (!tbody) {
      return;
    }
    if (tbody.querySelector("tr.event-row")) {
      return;
    }
    if (tbody.querySelector("td[colspan]")) {
      return;
    }
    const tr = document.createElement("tr");
    tr.innerHTML = '<td colspan="4">この月の予定はありません</td>';
    tbody.appendChild(tr);
  }

  /** 同じ月なら DOM 更新のみ。別月なら要素を外す／追加不可なら false */
  function upsertEventInDom(data) {
    if (!isSameDisplayedMonth(data.eventDate)) {
      const existing = findListRow(data.id);
      if (existing) {
        removeEventFromDom(data.id);
      }
      return false;
    }
    let row = findListRow(data.id);
    if (row) {
      applyEventUpdate(row, data);
      return true;
    }
    const tbody = ensureListTbody();
    if (tbody) {
      tbody.appendChild(createListRow(data));
    }
    const existingBtn = document.querySelector(
      '.calendar .js-open-event[data-id="' + data.id + '"]'
    );
    if (!existingBtn) {
      const td = document.querySelector(
        '.calendar td[data-date="' + data.eventDate + '"]'
      );
      if (td) {
        const ul = td.querySelector(".day-events");
        if (ul) {
          ul.appendChild(createCalendarItem(data));
        }
      }
    }
    removeDomDuplicatesOf(data);
    sortEventList();
    return true;
  }

  function removeEventFromDom(id) {
    const row = findListRow(id);
    if (row) {
      row.remove();
    }
    const btn = document.querySelector(
      '.calendar .js-open-event[data-id="' + id + '"]'
    );
    if (btn) {
      const li = btn.closest("li");
      if (li) {
        li.remove();
      }
    }
    showEmptyListIfNeeded();
  }

  /** 一覧を日付→時間の順に並べ替える（リロードなし） */
  function sortEventList() {
    const tbody = document.querySelector(".event-list tbody");
    if (!tbody) {
      return;
    }
    const rows = Array.prototype.slice.call(tbody.querySelectorAll("tr.event-row"));
    if (rows.length < 2) {
      return;
    }
    rows.sort(function (a, b) {
      const dateCmp = (a.dataset.date || "").localeCompare(b.dataset.date || "");
      if (dateCmp !== 0) {
        return dateCmp;
      }
      const timeA = a.dataset.time || "99:99";
      const timeB = b.dataset.time || "99:99";
      const timeCmp = timeA.localeCompare(timeB);
      if (timeCmp !== 0) {
        return timeCmp;
      }
      return Number(a.dataset.id || 0) - Number(b.dataset.id || 0);
    });
    rows.forEach(function (r) {
      tbody.appendChild(r);
    });
  }

  function finishEditor(restoreText) {
    if (!activeEditor) {
      return;
    }
    const cell = activeEditor.cell;
    cell.textContent = restoreText;
    cell.classList.remove("is-editing");
    activeEditor = null;
  }

  function startInlineEdit(cell) {
    if (cell.classList.contains("is-editing")) {
      return;
    }
    if (activeEditor) {
      finishEditor(activeEditor.originalDisplay);
    }

    const row = cell.closest("tr");
    const field = cell.dataset.field;
    const inputType = cell.dataset.input || "text";
    const originalDisplay = cell.textContent.trim();
    let currentValue = "";
    if (field === "date") {
      currentValue = row.dataset.date || "";
    } else if (field === "time") {
      currentValue = row.dataset.time || "";
    } else if (field === "title") {
      currentValue = row.dataset.title || "";
    } else {
      currentValue = row.dataset.description || "";
    }

    const input =
      field === "description"
        ? document.createElement("textarea")
        : document.createElement("input");
    if (field !== "description") {
      input.type = inputType; // date / time / text
    } else {
      input.rows = 2;
    }
    input.className = "inline-input";
    input.value = currentValue;
    if (field === "title") {
      input.maxLength = 100;
      input.required = true;
    }
    if (field === "date") {
      input.required = true;
    }

    cell.textContent = "";
    cell.classList.add("is-editing");
    cell.appendChild(input);
    input.focus();
    if (inputType === "text" && field !== "description" && input.select) {
      input.select();
    }

    let saving = false;

    function save() {
      if (saving) {
        return;
      }
      saving = true;
      const next = (input.value || "").trim();
      const payload = {
        action: "update",
        id: row.dataset.id,
        title: row.dataset.title || "",
        eventDate: row.dataset.date || "",
        eventTime: row.dataset.time || "",
        description: row.dataset.description || ""
      };

      if (field === "date") {
        payload.eventDate = next;
      } else if (field === "time") {
        payload.eventTime = next;
      } else if (field === "title") {
        payload.title = next;
      } else {
        payload.description = next;
      }

      if (!payload.title || !payload.eventDate) {
        alert("タイトルと日付は必須です");
        saving = false;
        input.focus();
        return;
      }

      const monthChanged =
        field === "date" &&
        next &&
        (next.slice(0, 7) !== row.dataset.date.slice(0, 7));

      postForm(payload)
        .then(function (data) {
          activeEditor = null;
          cell.classList.remove("is-editing");
          if (monthChanged) {
            reloadMonth(data.eventDate);
            return;
          }
          // 同じ月ならリロードせず更新（スクロール位置を維持）
          applyEventUpdate(row, data);
        })
        .catch(function (err) {
          saving = false;
          alert(err.message || "保存に失敗しました");
          input.focus();
        });
    }

    function cancel() {
      finishEditor(originalDisplay);
    }

    input.addEventListener("keydown", function (e) {
      if (e.key === "Enter" && field !== "description") {
        e.preventDefault();
        save();
      } else if (e.key === "Escape") {
        e.preventDefault();
        cancel();
      }
    });

    input.addEventListener("blur", function () {
      // date/time のピッカー操作中に blur が先に飛ぶことがあるので少し待つ
      setTimeout(function () {
        if (activeEditor && activeEditor.input === input) {
          save();
        }
      }, 150);
    });

    activeEditor = { cell: cell, input: input, originalDisplay: originalDisplay };
  }

  document.querySelectorAll(".event-list .js-inline-edit").forEach(function (cell) {
    cell.addEventListener("click", function () {
      startInlineEdit(cell);
    });
  });

  document.querySelectorAll(".js-open-event").forEach(function (el) {
    el.addEventListener("click", function (e) {
      e.preventDefault();
      e.stopPropagation();
      openEdit(el);
    });
  });

  document.querySelectorAll(".js-day-cell").forEach(function (td) {
    if (isPastDate(td.dataset.date)) {
      td.classList.add("is-past");
    }
    td.addEventListener("mousedown", function (e) {
      if (e.target.closest(".js-open-event")) {
        return;
      }
      if (isPastDate(td.dataset.date)) {
        e.preventDefault();
        return;
      }
      e.preventDefault();
      dragging = true;
      dragStartDate = td.dataset.date;
      highlightRange(dragStartDate, dragStartDate);
    });

    td.addEventListener("mouseenter", function () {
      if (!dragging || !dragStartDate) {
        return;
      }
      if (isPastDate(td.dataset.date)) {
        return;
      }
      highlightRange(dragStartDate, td.dataset.date);
    });
  });

  document.addEventListener("mouseup", function (e) {
    if (!dragging) {
      return;
    }
    dragging = false;
    const cell = e.target.closest(".js-day-cell");
    let endDate = cell && cell.dataset.date ? cell.dataset.date : dragStartDate;
    if (isPastDate(endDate)) {
      endDate = dragStartDate;
    }
    const range = clampRangeToFuture(dragStartDate, endDate);
    if (!range) {
      clearDaySelection();
      alert("過去の日付には予定を追加できません");
      return;
    }
    highlightRange(range.start, range.end);
    openCreate(range.start, range.end);
  });

  if (newBtn) {
    newBtn.addEventListener("click", function () {
      clearDaySelection();
      openCreate();
    });
  }

  cancelBtn.addEventListener("click", function () {
    clearDaySelection();
    dialog.close();
  });

  dialog.addEventListener("close", function () {
    clearDaySelection();
  });

  form.addEventListener("submit", function (e) {
    e.preventDefault();
    const id = idEl.value;
    if (id) {
      const payload = {
        action: "update",
        id: id,
        title: titleInput.value,
        eventDate: dateInput.value,
        eventTime: timeInput.value,
        description: descInput.value
      };
      postForm(payload)
        .then(function (data) {
          dialog.close();
          const row = findListRow(data.id);
          if (isSameDisplayedMonth(data.eventDate)) {
            if (row) {
              applyEventUpdate(row, data);
            } else {
              upsertEventInDom(data);
            }
            return;
          }
          // 表示中の月から外れた → 消すだけでリロードしない
          removeEventFromDom(data.id);
        })
        .catch(function (err) {
          showError(err.message || "保存に失敗しました");
        });
      return;
    }

    const start = dateInput.value;
    const end = dateEndInput.value || start;
    if (!start) {
      showError("開始日は必須です");
      return;
    }
    const range = clampRangeToFuture(start, end);
    if (!range) {
      showError("過去の日付には予定を追加できません");
      return;
    }
    const dates = listDatesInRange(range.start, range.end).filter(function (d) {
      return !isPastDate(d);
    });
    if (!dates.length) {
      showError("過去の日付には予定を追加できません");
      return;
    }
    if (dates.length > 93) {
      showError("一度に作成できるのは最大93日分です");
      return;
    }

    const title = titleInput.value;
    const time = timeInput.value;
    const description = descInput.value;
    Promise.all(
      dates.map(function (d) {
        return postForm({
          action: "create",
          title: title,
          eventDate: d,
          eventTime: time,
          description: description
        });
      })
    )
      .then(function (results) {
        dialog.close();
        // 同じ内容は1件にまとめる（後勝ちの id を残す）
        const unique = [];
        const seen = {};
        results.forEach(function (data) {
          const key = [
            data.eventDate || "",
            data.eventTime || "",
            data.title || "",
            data.description || ""
          ].join("\t");
          seen[key] = data;
        });
        Object.keys(seen).forEach(function (k) {
          unique.push(seen[k]);
        });
        unique.forEach(function (data) {
          upsertEventInDom(data);
        });
        if (
          unique.length &&
          unique.every(function (r) {
            return !isSameDisplayedMonth(r.eventDate);
          })
        ) {
          sessionStorage.setItem("cal-scroll", String(window.scrollY));
          reloadMonth(unique[0].eventDate);
        }
      })
      .catch(function (err) {
        showError(err.message || "保存に失敗しました");
      });
  });

  deleteBtn.addEventListener("click", function () {
    if (!idEl.value) {
      return;
    }
    if (!confirm("この予定を削除しますか？")) {
      return;
    }
    const deletedId = idEl.value;
    postForm({ action: "delete", id: deletedId })
      .then(function () {
        dialog.close();
        removeEventFromDom(deletedId);
      })
      .catch(function (err) {
        showError(err.message || "削除に失敗しました");
      });
  });
})();
