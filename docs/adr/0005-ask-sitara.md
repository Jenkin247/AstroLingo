# ADR 0005: Ask Sitara through a Cloud Function, no personal data, daily cap

- Status: accepted
- Date: 2026-10-17

## Context
Sitara answers questions about lessons, with 3 free questions a day (PO decision). The PO chose Claude Haiku 5.5, EU processing, an "Explain my placement" entry point, and a cap of $50 a month on AI spend.

## Decision
- **No key in the app.** The app calls one Firebase Cloud Function (2nd gen, `europe-west`), which requires a Firebase ID token and an App Check token.
- **The request carries only** the question text, the current lesson id and the app language. It never carries birth details, chart positions or the deity choice. "Explain my placement" sends a generic concept ("Moon in the 10th house"), not the user's chart.
- **The function** adds the Guru's guardrail prompt and the lesson text, calls Claude Haiku 5.5 with a hard `max_tokens`, and returns the answer. The model id and prompt are in versioned config (ADR 0006).
- **Provider route:** Claude on Google Cloud Vertex AI in an EU region is preferred, so data stays in the EU under Google Cloud terms. The Anthropic API is the fallback. Both are checked before launch.
- **Caps:** 3 questions per UID per day, counted in Firestore by UID and date only. A global monthly budget guard stops calls at $50 and shows "Sitara is resting". A Cloud Billing budget alert is set at 50%, 90% and 100%.
- **Storage:** no question or answer text is stored or logged by us. Logs hold only the UID hash, date, token counts and outcome.
- **Guardrails:** decline questions about death, illness, money, marriage outcomes and paid remedies, and point to a lesson or a professional instead.

## To verify before launch (owner: Architect)
- That the chosen route does not use our inputs for training, and its retention period. Anthropic's privacy center says API data is deleted within 30 days by default. Confirm this in the DPA, or in the Vertex AI terms if we use Vertex.
- That Haiku 5.5 is available in the chosen EU region.

## Consequences
- Needs the Firebase Blaze plan and an Anthropic or Vertex account. The PO action is due 6 November.
- At about $0.0004 a question, 1,000 users asking 3 a day costs about $34 a month, under the cap.
