# Implementation Plan - Pinch-to-Zoom and Rotation UI Update for Profile Photos

Add pinch-to-zoom functionality to the full-screen profile photo viewer in `BiographyScreen.kt` and improve the rotation button UI as requested.

## Proposed Changes

### [BiographyScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/BiographyScreen.kt)

#### [MODIFY] [BiographyScreen.kt](file:///C:/Android_Projects/Reminera/app/src/main/java/com/rekluzlabs/reminera/ui/biography/BiographyScreen.kt)
- Add state variables for `scale`, `offsetX`, and `offsetY` to track zoom and pan state.
- Implement `detectTransformGestures` for pinch-to-zoom and panning.
- Implement `detectTapGestures` for double-tap to zoom in/reset.
- Apply these transformations using `graphicsLayer` on the `Image` component.
- Reposition the rotate button from `TopEnd` to `BottomCenter`.
- Add a "Rotate Image" label next to the rotation icon.
- Wrap the rotation controls in a styled `Row` (e.g., semi-transparent background) for better legibility.

## Verification Plan

### Manual Verification
1. Open a family member's biography.
2. Tap on their profile photo to open it in full screen.
3. **Pinch-to-zoom**: Verify that pinching on the image scales it up/down.
4. **Panning**: Verify that when zoomed in, you can drag the image to view different parts.
5. **Double-tap**: Verify that double-tapping zooms in to a preset level or resets to 1.0 scale.
6. **Rotation**:
    - Verify the rotate button is now at the bottom of the screen.
    - Verify the text "Rotate Image" is visible next to the icon.
    - Verify tapping the button rotates the image correctly.
7. **Close**: Verify that tapping the 'X' or the background still closes the full-screen view.
