# iOS App Store – Workflow to Publish a New Version

## 1. Create a new version in App Store Connect

- Go to **App Store Connect**
- Open your app
- Go to **Distribution**
- Click **+ Version or Platform**
- Enter the new version (e.g. `1.1`)
- Save

---

## 2. Update the version in Xcode

In **Xcode**:

`Target → General`

Set for example:

```
Version: 1.1
Build: 1
```

Rules:

- **Version** must match the version created in App Store Connect
- **Build** must be greater than the previous build number

---

## 3. Create the archive

In Xcode:

1. Select the device:

```
Any iOS Device (arm64)
```

2. Then:

```
Product → Archive
```

Xcode will open **Organizer**.

---

## 4. Upload the build

In **Organizer**:

```
Distribute App
→ App Store Connect
→ Upload
```

Follow the default steps until the upload completes.

---

## 5. Wait for processing

After uploading:

- Go to **App Store Connect**
- Open **TestFlight**

The build usually appears after **5–20 minutes**.

---

## 6. Attach the build to the version

In **App Store Connect**:

```
Distribution
→ Version 1.1
→ Build
→ Select a build
```

Choose the uploaded build.

---

## 7. Submit the version for review

Click:

```
Add for Review
```

or

```
Submit for Review
```

---

## 8. Wait for Apple review

Possible statuses:

```
Waiting for Review
In Review
Approved
Ready for Distribution
```

---

## 9. Release

If **automatic release** is enabled:

→ The app will be published automatically.

Otherwise:

```
Release this version
```

---

# Quick Workflow (Summary)

```
1. Create the version in App Store Connect
2. Update Version + Build in Xcode
3. Product → Archive
4. Distribute → Upload
5. Wait for processing
6. Select the build
7. Submit for Review
```