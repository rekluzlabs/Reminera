# Refine YouTube Embed Detection and Fix False Positives

Improve the reliability of the YouTube embedding error detection and investigate the impact of the `Referer` header and excessive logging.

## User Review Required

> [!IMPORTANT]
> We will be testing with a known-good video (`dQw4w9WgXcQ`) and potentially removing the `Referer` header if it's found to be the cause of the restrictions.

## Proposed Changes

### UI Components

#### [MODIFY] [BiographyScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/BiographyScreen.kt)

- **Improve Detection Logic**: Update the JavaScript check to be more specific. Instead of just checking if `.ytp-embed-error` exists, check if it's visible or has specific error text (e.g., "Video unavailable").
- **Referer Investigation**: Temporarily remove or simplify the `Referer` header to see if it's causing YouTube to block the embed.
- **Cleanup Logs**: Remove the `GlobalLayoutListener` and excessive "Update called" logs to reduce noise.
- **Test with dQw4w9WgXcQ**: Update the test path or manually verify with this ID to ensure the logic works for videos that *do* allow embedding.

## Verification Plan

### Manual Verification
- Test with `dQw4w9WgXcQ` (Rickroll) to ensure it plays correctly without triggering the error UI.
- Test with `XsoHj5UNoWo` to ensure the error UI still appears if embedding is truly disabled.
- Observe logs to ensure recomposition noise is reduced.
