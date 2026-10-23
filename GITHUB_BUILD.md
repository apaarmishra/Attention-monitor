# Build without Android Studio

1. Sign in to github.com (personal account; check workplace policy if on a managed device).
2. Create a new **private** repository, e.g. `attention-monitor`.
3. Upload the **contents of the AttentionMonitor folder**, including `.github/workflows/build-apk.yml` (GitHub's web uploader may omit hidden folders if you select a folder; make sure `.github` appears in your repo). Commit to the `main` branch.
4. Open your repository's **Actions** tab, select **Build Android APK**, and click **Run workflow**. It also runs on pushes to `main`.
5. When successful, click the workflow run, then download the `AttentionMonitor-debug-apk` artifact under **Artifacts**. Extract the ZIP to get `app-debug.apk`.
6. Open the APK from your Pixel Downloads folder, permit installation from that source if prompted, and follow the README to enable the Accessibility service.

No Android Studio on the laptop. Builds happen on GitHub's Linux runner. This workflow has not been executed here; the first cloud build may reveal compilation issues. This produces a **debug** APK for personal testing, not a release signed for Play Store distribution.

If uploading through the GitHub browser is awkward, use a personal device to upload. Do not bypass your organization's policies.
