# Google Meet tenant integration

When creating a tenant, the platform administrator selects **Built-in live class** or **Google Meet** for the organization. Instructors confirm that mode for each class. For Google Meet, a tenant admin connects the organization's Google Calendar from the Schedule Live Class dialog. The LMS then creates a Calendar event on that account's primary calendar and requests a Google Meet conference.

## Google Cloud setup

1. Create or select a Google Cloud project and enable the Google Calendar API.
2. Configure the OAuth consent screen for the app and add the organization administrator accounts as test users while the app is in testing.
3. Create an OAuth client ID for a Web application. Register the backend callback URL as an authorized redirect URI: `https://<api-host>/api/v1/live-sessions/google/callback`.
4. Configure these backend environment variables:

   - `GOOGLE_OAUTH_CLIENT_ID`
   - `GOOGLE_OAUTH_CLIENT_SECRET`
   - `GOOGLE_OAUTH_REDIRECT_URI` (must exactly match the registered URI)
   - `FRONTEND_BASE_URL` (used to return the administrator to the LMS)
   - `TENANT_CREDENTIAL_ENCRYPTION_KEY` (base64-encoded 32-byte key; also used for tenant database credentials)

The LMS requests `openid`, `email`, and the narrow `https://www.googleapis.com/auth/calendar.events` scope. Offline OAuth access is required so the backend can create events without an administrator present. Refresh tokens are encrypted before persistence and never returned by the API. Google may require OAuth app verification before accounts outside the configured test users can connect.

## Session behavior

- The connected organization's primary calendar owns the events and Meet conferences.
- Each LMS schedule operation creates one Calendar event; the returned Meet link is stored on the live-session record and exposed to enrolled learners through the LMS.
- Joining through the LMS records the participant's join event, then opens Meet in a new tab. Google Meet itself remains responsible for the audio/video room and its participant controls.
- Calendar attendee invitations, event updates/deletion, and Google-side participant/leave webhooks are not implemented. LMS attendance is therefore based on LMS join and leave actions and the configured session end behavior.
- Meet conferencing must be available to the connected Google account. Google Calendar can return an event before the conference link is ready, so the backend briefly polls the event before reporting a scheduling error.
