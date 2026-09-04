/**
 * 今日予定があるとき:
 * - 画面上部バナー
 * - 画面内トースト（許可不要・必ず見える）
 * - ブラウザ通知（許可時）
 * - 開始1時間前に個別通知（ページを開いている間は setTimeout で予約）
 */
(function () {
  const script = document.currentScript;
  const contextPath = (script && script.dataset.context) || "";
  const apiUrl = contextPath + "/api/today-events";
  const calendarUrl = contextPath + "/calendar";

  const DAY_KEY = "event-notify-day:";
  const EVENT_KEY = "event-notify-id:";
  const timers = {};

  function todayKey(date) {
    return DAY_KEY + date;
  }

  function eventKey(id, date) {
    return EVENT_KEY + date + ":" + id;
  }

  function escapeHtml(text) {
    return String(text)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function ensureBannerHost() {
    let host = document.getElementById("today-notify-banner");
    if (!host) {
      host = document.createElement("div");
      host.id = "today-notify-banner";
      host.className = "today-notify";
      host.hidden = true;
      document.body.prepend(host);
    }
    return host;
  }

  function ensureToastHost() {
    let host = document.getElementById("notify-toast-host");
    if (!host) {
      host = document.createElement("div");
      host.id = "notify-toast-host";
      host.className = "notify-toast-host";
      document.body.appendChild(host);
    }
    return host;
  }

  /** 画面内トースト（ブラウザ許可なしでも表示） */
  function showToast(title, body) {
    const host = ensureToastHost();
    const el = document.createElement("div");
    el.className = "notify-toast";
    el.innerHTML =
      "<strong>" +
      escapeHtml(title) +
      "</strong>" +
      (body ? "<p>" + escapeHtml(body) + "</p>" : "");
    host.appendChild(el);
    setTimeout(function () {
      el.classList.add("is-out");
      setTimeout(function () {
        el.remove();
      }, 400);
    }, 8000);
  }

  function canNotify() {
    return typeof Notification !== "undefined";
  }

  function showBrowserNotification(title, body, tag) {
    if (!canNotify() || Notification.permission !== "granted") {
      return false;
    }
    try {
      const n = new Notification(title, {
        body: body || "",
        tag: tag || undefined,
        requireInteraction: false
      });
      n.onclick = function () {
        window.focus();
        n.close();
      };
      return true;
    } catch (e) {
      console.warn("Notification failed", e);
      return false;
    }
  }

  /** OS通知 + 画面トーストの両方 */
  function notifyUser(title, body, tag) {
    const ok = showBrowserNotification(title, body, tag);
    showToast(title, body);
    if (!ok && canNotify() && Notification.permission !== "granted") {
      // 許可前でもトーストは出ている
    }
    return ok;
  }

  function renderBanner(date, events) {
    const host = ensureBannerHost();
    if (!events.length) {
      host.hidden = true;
      host.innerHTML = "";
      return;
    }
    const lines = events
      .map(function (ev) {
        const time = ev.eventTime ? ev.eventTime + " " : "";
        return "<li><strong>" + escapeHtml(time + ev.title) + "</strong></li>";
      })
      .join("");

    const perm = canNotify() ? Notification.permission : "unsupported";
    const permLabel =
      perm === "granted"
        ? "ブラウザ通知: ON"
        : perm === "denied"
          ? "ブラウザ通知: 拒否（設定から許可してください）"
          : "ブラウザ通知: OFF";

    host.innerHTML =
      "<div class=\"today-notify-inner\">" +
      "<p>今日（" +
      escapeHtml(date) +
      "）の予定が " +
      events.length +
      " 件あります <span class=\"perm-label\">" +
      escapeHtml(permLabel) +
      "</span></p>" +
      "<ul>" +
      lines +
      "</ul>" +
      "<p><a href=\"" +
      calendarUrl +
      "\">カレンダーを開く</a>" +
      (perm === "granted"
        ? ' <button type="button" class="notify-test">今すぐテスト通知</button>'
        : ' <button type="button" class="notify-enable">ブラウザ通知を許可</button>') +
      "</p>" +
      "</div>";
    host.hidden = false;

    const enableBtn = host.querySelector(".notify-enable");
    if (enableBtn) {
      enableBtn.addEventListener("click", function () {
        requestPermissionAndNotify(date, events, true);
      });
    }
    const testBtn = host.querySelector(".notify-test");
    if (testBtn) {
      testBtn.addEventListener("click", function () {
        localStorage.removeItem(todayKey(date));
        events.forEach(function (ev) {
          localStorage.removeItem(eventKey(ev.id, date));
        });
        notifyUser("テスト通知", "通知はこのように表示されます", "test-" + Date.now());
        scheduleReminders(date, events);
      });
    }
  }

  function dailySummary(date, events) {
    if (!events.length) {
      return;
    }
    if (localStorage.getItem(todayKey(date))) {
      return;
    }
    const body = events
      .map(function (ev) {
        return (ev.eventTime ? ev.eventTime + " " : "") + ev.title;
      })
      .join(" / ");
    notifyUser("今日の予定（" + events.length + "件）", body, "today-" + date);
    localStorage.setItem(todayKey(date), "1");
  }

  function requestPermissionAndNotify(date, events, forceSummary) {
    if (!canNotify()) {
      showToast("通知非対応", "このブラウザは OS 通知に対応していません。画面内通知のみ使えます。");
      dailySummary(date, events);
      return;
    }
    Notification.requestPermission().then(function (perm) {
      renderBanner(date, events);
      if (perm === "granted") {
        if (forceSummary) {
          localStorage.removeItem(todayKey(date));
        }
        dailySummary(date, events);
        scheduleReminders(date, events);
        showToast("通知を許可しました", "予定の1時間前に知らせます");
      } else if (perm === "denied") {
        showToast("通知が拒否されています", "ブラウザのサイト設定から通知を許可してください");
      }
    });
  }

  function parseTimeToMinutes(timeText) {
    if (!timeText) {
      return null;
    }
    const parts = timeText.split(":");
    if (parts.length < 2) {
      return null;
    }
    return Number(parts[0]) * 60 + Number(parts[1]);
  }

  function eventStartDate(dateText, timeText) {
    const parts = timeText.split(":");
    const d = new Date(dateText + "T00:00:00");
    d.setHours(Number(parts[0]), Number(parts[1]), Number(parts[2] || 0), 0);
    return d;
  }

  function fireReminder(date, ev) {
    const key = eventKey(ev.id, date);
    if (localStorage.getItem(key)) {
      return;
    }
    const body =
      "1時間後に開始" + (ev.description ? " — " + ev.description : "");
    notifyUser(ev.title, body, "ev-" + ev.id);
    localStorage.setItem(key, "1");
  }

  /**
   * 各予定の「開始1時間前」にタイマーを張る。
   * すでに1時間前を過ぎていて開始前なら、すぐに1回通知する。
   */
  function scheduleReminders(date, events) {
    Object.keys(timers).forEach(function (id) {
      clearTimeout(timers[id]);
      delete timers[id];
    });

    const now = Date.now();
    events.forEach(function (ev) {
      if (!ev.eventTime) {
        return;
      }
      const key = eventKey(ev.id, date);
      if (localStorage.getItem(key)) {
        return;
      }

      const start = eventStartDate(date, ev.eventTime);
      const remindAt = start.getTime() - 60 * 60 * 1000;
      const startMs = start.getTime();

      if (now >= startMs) {
        // 開始済みはスキップ
        return;
      }

      if (now >= remindAt) {
        // すでに1時間前を過ぎている → すぐ通知
        fireReminder(date, ev);
        return;
      }

      const delay = remindAt - now;
      timers[ev.id] = setTimeout(function () {
        fireReminder(date, ev);
        delete timers[ev.id];
      }, delay);
      console.info(
        "[notify] scheduled",
        ev.title,
        "in",
        Math.round(delay / 60000),
        "min"
      );
    });
  }

  function run(data) {
    const date = data.date;
    const events = data.events || [];
    renderBanner(date, events);

    // 許可済みならまとめ通知 + 1時間前タイマー
    if (canNotify() && Notification.permission === "granted") {
      dailySummary(date, events);
      scheduleReminders(date, events);
    } else if (events.length) {
      // 未許可でも、画面内の今日バナーは出る。初回だけ画面内で今日の予定を知らせる
      if (!localStorage.getItem(todayKey(date) + ":toast")) {
        const body = events
          .map(function (ev) {
            return (ev.eventTime ? ev.eventTime + " " : "") + ev.title;
          })
          .join(" / ");
        showToast("今日の予定（" + events.length + "件）", body);
        localStorage.setItem(todayKey(date) + ":toast", "1");
      }
      // タイマー自体は許可前でも画面トースト用に予約
      scheduleReminders(date, events);
    }
  }

  fetch(apiUrl, { headers: { Accept: "application/json" } })
    .then(function (res) {
      if (!res.ok) {
        throw new Error("api " + res.status);
      }
      return res.json();
    })
    .then(function (data) {
      run(data);
      // 予定の増減に追従（タイマー再設定）
      setInterval(function () {
        fetch(apiUrl, { headers: { Accept: "application/json" } })
          .then(function (res) {
            return res.json();
          })
          .then(run)
          .catch(function (e) {
            console.warn("[notify] poll failed", e);
          });
      }, 60 * 1000);
    })
    .catch(function (e) {
      console.error("[notify] init failed", e);
      showToast("通知の初期化に失敗", "ページを再読み込みしてください");
    });
})();
