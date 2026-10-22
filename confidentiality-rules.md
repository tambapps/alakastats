# Privacy Policy – Alakastats (Android/iOS/Web)

**Last updated: October 22, 2026**

---

## 1. Introduction

Alakastats is an application dedicated to Pokémon team and statistics analysis.  
Protecting your privacy is a priority.

This Privacy Policy transparently explains how the application handles user information.

---

## 2. Data Collection

**Alakastats does not collect any personal data.** The developer has no server and never receives your data.

Specifically:

- No personal identification data (name, email address, device identifiers)
- No location data
- No health data
- No financial information
- No usage data sent to external servers
- No advertising identifiers
- No cookies
- No tracking technologies

---

## 3. Data Storage

All data you enter into the application (teams, statistics, notes, analyses, etc.):

- Is stored **locally on your device** (in your browser for the web version)
- Is never sent to a server of the developer
- Is never shared with third parties
- Is accessible only by you, unless you share a team with other people (see section 4)

If you choose to enable the optional Google Drive sync (see section 4), a copy of this data is also stored in **your own** Google Drive.

You may delete the local data at any time by:

- Uninstalling the application
- Clearing the application's data through system settings (or the site's data in your browser)

---

## 4. Optional Google Drive Sync

Alakastats lets you sync your teams between your devices through your own Google Drive. **This feature is disabled by default** and only works once you connect your Google account in the application. Without it, the application works entirely locally.

When you connect your Google Drive:

- The application only asks for the `drive.appdata` permission: access to a **hidden folder of your Google Drive dedicated to Alakastats**. It cannot see, read or modify any other file of your Drive.
- Your teams (team, replays analyses, notes, game plans) are saved in this hidden folder, in your own Google account, and count towards your own storage quota.
- The data goes directly from your device to Google: it never goes through a server of the developer, and the developer has no access to it.
- Google's sign-in gives the application an access token to your Drive. This token stays on your device (in the system's secure storage on iOS, in the browser's session on the web) and is only used to sync your teams.
- The application reads the email address of the connected Google account, only to show you which account is connected. It's kept on your device only, never sent anywhere else, and deleted when you disconnect. The application doesn't read your name or any other information of your Google account.

### Shared teams

If you choose to, you can share a team with other people through Google Drive:

- The application then asks for the `drive.file` permission: access **only to the files Alakastats created, or that you picked in the application** (Google's file picker). It cannot see, read or modify any other file of your Drive.
- A team you share is moved to an "Alakastats" folder of your Google Drive. You invite people with the email address of their Google account: the application sends it to Google to share the file, and Google sends them an invitation email. The application doesn't store these addresses: it only shows the people the team is shared with, as Google gives them.
- The people you invite can see the team (and change it, if you made them editors), as with any file shared through Google Drive. They join it by picking it in the application. You can change their role, remove them or stop sharing at any time; they then keep a copy of the team as it was.
- A team shared with you stays in its owner's Google Drive. If the owner deletes it, stops sharing it or removes you, you keep a copy of it on your devices.

Alakastats' use and transfer of information received from Google APIs adheres to the [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy), including the Limited Use requirements.

You can stop the sync and delete the synced data at any time:

- **Disconnect** your Google Drive in the application (Sync screen): the sync stops and the application's access is revoked. Your teams stay on your device and in your Drive.
- **Delete the synced data** from Google Drive: Settings → Manage apps → Alakastats → Options → Delete hidden app data. The teams you shared are in the "Alakastats" folder of your Drive, and can be deleted like any file.
- **Revoke the access** from your Google account: Security → Your connections to third-party apps & services → Alakastats.

---

## 5. Advertising

Alakastats:

- Displays no advertisements
- Uses no advertising SDKs
- Shares no data with advertising networks

---

## 6. Third-Party Services

The application:

- Does not use analytics services
- Does not use tracking services
- Downloads public data (battle replays from Pokémon Showdown, Pokémon data from PokéAPI), without sending any personal data
- Only uses Google services (Google Sign-In and the Google Drive API) if you enable the Google Drive sync. Their use is governed by [Google's Privacy Policy](https://policies.google.com/privacy).

---

## 7. Permissions

Alakastats does not request any sensitive permissions.  
If standard system permissions are required by your device for basic functionality, they are never used to collect or transmit personal data.

---

## 8. Security

The developer has no server: no data is processed outside of your device, except by Google if you enable the Google Drive sync, in which case the data is sent to your Drive over encrypted (HTTPS) connections.  
The security of locally stored data depends on your device's protection mechanisms (screen lock, encryption, etc.), and the security of synced data on your Google account's protection.

---

## 9. Children's Privacy

The application does not collect any data and can be used without providing personal information.  
No data related to children is collected.

---

## 10. Changes to This Policy

If this Privacy Policy is updated, the "Last updated" date at the top of this document will be revised accordingly.

---

## 11. Contact

For any questions regarding this Privacy Policy:

- Email: tambapps@gmail.com
- GitHub issues: https://github.com/tambapps/alakastats/issues

---

### Summary

Alakastats is a local-first application, with no data collection and no advertising. If you choose to, your teams can be synced through a hidden folder of your own Google Drive, and shared with the people you invite through Google Drive. The developer never has access to any of it.
