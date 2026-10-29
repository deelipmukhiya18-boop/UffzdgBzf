# Mamta AI Assistant (Android)

Mamta is an intelligent Android AI companion with voice interaction, Neural Lens vision, device control, documents, and utility workflows.

## GitHub Actions Automated APK Build

This repository includes a GitHub Actions CI/CD workflow (`.github/workflows/build-apk.yml`) that automatically builds an installable Android APK on every push, pull request, or manual trigger.

### How to Get Your APK on GitHub:

1. **Push this repository to GitHub**:
   - In AI Studio, use the settings menu to export or push your repository directly to GitHub.
2. **Go to GitHub Actions**:
   - Open your repository on GitHub and click on the **Actions** tab.
   - You will see the **Build Android APK** workflow running.
   - You can also click **Run workflow** anytime to trigger a build manually.
3. **Download the APK**:
   - Once the build succeeds (green checkmark), click on the completed workflow run.
   - Scroll down to the **Artifacts** section at the bottom.
   - Click on **`Mamta-debug-apk`** to download your zip file containing `Mamta-debug.apk`.
   - Transfer and install `Mamta-debug.apk` directly onto your Android device!
