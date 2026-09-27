# Pilot kit

For roadmap phase 5 (weeks 17–20). Pilot group: GDS of Benagoria, Birsinghpur, Barwa, Pandra,
Poddardih and Kaliasol BOs, staff at Nirsa Chatti SO, plus 2–3 staff from other Sub Offices.

Before the pilot starts: the intimation/permission letter to the Division has gone (roadmap §7.1),
a separate "Agent Setu Users" WhatsApp group exists, and version 1.0.0 is published following
[RELEASE.md](RELEASE.md).

## 1. Rollout stages

| Stage | Who | Move on when |
|---|---|---|
| 1 | You + 3–4 colleagues | No data loss; reminders arrive on time |
| 2 | Pilot group (15–25 users) | They install and update without your help |
| 3 | East Sub Division | Few crash reports; feedback is mostly feature requests |
| 4 | Dhanbad Division group | The permission/intimation position is clear |

## 2. Test checklist (tick on at least two phones, one of them low-end)

**Install and update**
- [ ] Fresh install from WhatsApp: "Install unknown apps" prompt, Play Protect warning, "Install anyway".
- [ ] *Settings → About* fingerprint matches the pinned one.
- [ ] Install the next version **over** the old one: all customers, ledger, PIN and reminders survive.
- [ ] *Settings → Check for updates* shows "latest version" (online) and "could not check" (airplane mode).

**Offline and language**
- [ ] Airplane mode: every screen works (only the update check fails, politely).
- [ ] Switch Hindi ↔ English: no English left on Hindi screens, no text cut off on a 5-inch phone.
- [ ] Amounts show as 1,00,000; dates as DD-MM-YYYY.

**Reminders**
- [ ] Notification permission asked once (Android 13+); the daily summary arrives around 7 am.
- [ ] On Xiaomi / Vivo / Oppo / Samsung: follow the in-app battery guide; the summary still arrives
      after the phone was idle overnight and after a restart.
- [ ] Customer names are hidden in the notification on the lock screen.
- [ ] Premium *Collected* adds a ledger entry; tapping twice does not add two.
- [ ] Maturity *Reinvested* opens Add business; *Not decided* comes back in 7 days.

**Data safety**
- [ ] PIN asked on start and after 2 minutes away; 5 wrong PINs → 30-second wait.
- [ ] Fingerprint unlock works where available.
- [ ] Backup: *Save* to Drive/phone and *Share* to WhatsApp "message yourself".
- [ ] Restore that file on a **second phone**: customers, ledger, rates and profile come back.
- [ ] Wrong backup password is refused with a clear message.
- [ ] Delete a test customer: they disappear everywhere; their commission stays as "(deleted customer)".

**Commission maths** (roadmap: at least 20 real cases per rule type, checked by hand)
- [ ] Enter the verified rates and order numbers in *Settings → Rates and rules* first.
- [ ] Fill in the sheet below from real, already-paid cases, and compare.

## 3. Commission hand-check sheet

One row per real case. No customer names, no policy numbers.

| # | Staff type | Product | Plan type (PLI) | Term (yrs) | Policy year | Premium / deposit (₹) | Date | Paid by DoP (₹) | App expected (₹) | Match? |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | GDS | PLI | Non-AEA | 20 | 1 | 12,000 | 05-08-2026 | | | |
| 2 | | | | | | | | | | |

Any mismatch means either the rate/rule in the app is wrong (fix it in *Rates and rules* with the
order number) or the matching logic is wrong (send the row to the developer; it becomes a test).

## 4. Feedback form (Google Form, 1 minute, monthly)

1. आप कौन-सा वर्शन इस्तेमाल कर रहे हैं? (*Settings → About*) / Which version do you have?
2. आप किस पद पर हैं? GDS BPM / ABPM / Dak Sevak / PA / SPM / Postman / MTS / अन्य
3. आपके फ़ोन का ब्रांड और मॉडल? / Phone brand and model?
4. ऐप में कितने ग्राहक जोड़े हैं? (Today screen) / How many customers have you added?
5. क्या रिमाइंडर समय पर आए? हमेशा / कभी-कभी / कभी नहीं
6. क्या इस महीने किसी रिन्यूअल या मैच्योरिटी पर ऐप की वजह से समय पर काम हुआ? कोई उदाहरण?
7. क्या ऐप का कमीशन आपको मिले कमीशन से मेल खाता है? हाँ / नहीं / पता नहीं
8. सबसे ज़्यादा क्या परेशान करता है? / What annoys you most?
9. अगला कौन-सा फ़ीचर सबसे ज़्यादा काम का होगा? / Which feature next?
10. क्या ऐप कभी बंद हुआ (crash)? अगर हाँ, तो *सेटिंग्स → एरर रिपोर्ट भेजें* से भेजें।

The form must not ask for customer details. Share its link only in the Agent Setu Users group.

## 5. Install guide video (Hindi, about 2 minutes)

Record the phone screen (a colleague's typical phone, Hindi set as phone language). Narration:

1. **(0:00)** "नमस्ते। यह एजेंट सेतु है - ग्राहक, रिन्यूअल, मैच्योरिटी और कमीशन याद रखने का एक
   मुफ़्त, निजी ऐप। यह इंडिया पोस्ट का आधिकारिक ऐप नहीं है।"
2. **(0:15)** "सिर्फ़ एजेंट सेतु ग्रुप से APK डाउनलोड करें। फ़ाइल पर टैप करें।"
3. **(0:30)** "'अनजान ऐप इंस्टॉल करने' की अनुमति माँगी जाए तो सेटिंग्स में WhatsApp या फ़ाइल मैनेजर
   के लिए चालू करें और वापस आएँ।"
4. **(0:50)** "Play Protect चेतावनी आ सकती है - 'More details' फिर 'Install anyway' दबाएँ।"
5. **(1:05)** "ऐप खोलें, भाषा चुनें, सूचना पढ़कर टिक करें, अपना पद चुनें और PIN रखें।"
6. **(1:25)** "आज वाली स्क्रीन पर 'अनुमति दें' दबाएँ ताकि रिमाइंडर आएँ। Xiaomi, Vivo, Oppo पर
   'रिमाइंडर नहीं आ रहे?' में बताई बैटरी सेटिंग ज़रूर करें।"
7. **(1:45)** "हर महीने सेटिंग्स → बैकअप और रीस्टोर से बैकअप लें और खुद को WhatsApp पर भेज दें।
   धन्यवाद।"

Pin the video and the official download link in the group description, next to the signing-key
fingerprint.

## 6. Success measures after 3 months (roadmap §16)

100+ active users · 80%+ on the latest version · 25+ customers per user · clear examples of
renewals/maturities handled on time · fewer than 1 crash report per 100 users per week.
