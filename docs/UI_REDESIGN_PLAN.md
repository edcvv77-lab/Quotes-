# UI Redesign Plan

Date: 2026-08-31
Branch: `ui/pro-architecture-redesign`

## Design direction

The redesign keeps the app unmistakably a premium Arabic quotes application while giving the private area a separate, restrained secure-workspace identity.

Visual language:
- warm ivory paper surfaces for Quotes;
- deep evergreen ink as the primary brand color;
- muted brass accent used sparingly;
- large Arabic-first typography and generous spacing;
- soft layered cards rather than default Android buttons;
- private space alone uses a Hadeel-specific blush/lavender/cream identity with rose-gold accents;
- no neon "hacker" styling and no excessive technical labels.

## Quotes screen

Planned hierarchy:
1. compact premium masthead with title and short editorial subtitle;
2. quote count and contextual state as lightweight chips;
3. prominent search field;
4. primary add action and secondary favorites filter;
5. quote cards with a subtle editorial mark, strong readable quote text and compact actions;
6. tablet layout capped to a readable content width.

Interaction goals:
- 48dp+ touch targets;
- reduced visual noise;
- consistent button states;
- clear empty/search states;
- existing hidden-trigger behavior unchanged.

## Private-space screen

Planned hierarchy:
1. secure-space header with a small state indicator;
2. compact operational status card;
3. primary **نسخ تطبيق** action;
4. secondary **إضافة APK** and refresh actions;
5. guest app grid with larger icons and cleaner labels;
6. short, human-readable usage hint;
7. visually separated lock/return action.

The most common action (**نسخ تطبيق**) must be visually dominant because the target user should not need to understand APK files.

## Clone picker

Retain current functional improvements:
- real app icon;
- real app label;
- package name as secondary metadata;
- alphabetical ordering;
- search;
- visible result count.

Redesign:
- cleaner modal surface;
- rounded search control;
- more generous row spacing;
- reduced emphasis on package IDs;
- obvious tap affordance.

## Engineering constraints

- XML Views only for this phase.
- No targetSdk/minSdk/compileSdk changes.
- No package/application ID changes.
- No change to BlackBox engine behavior.
- No new dangerous permissions.
- phone and `sw600dp` resources must both be updated.
- all current view IDs used by `MainActivity` must be preserved unless code is updated in the same commit.
- CI must pass before an APK is handed out.

## Acceptance for this UI phase

- `:app:testDebugUnitTest` passes.
- host APK builds.
- split fixture checks still pass.
- merged manifest and permission checks still pass.
- phone layouts compile.
- sw600dp layouts compile.
- private clone picker compiles and retains icon/search behavior.
- no runtime claim is made until the tablet is tested.


## Implementation status

Status: **IMPLEMENTED AND BUILD-VERIFIED**

Latest verification: GitHub Actions run **#263** on code/config commit `ac455e76029c77957609a52f9d0c41e2b82c7153`.

All planned phone/tablet layout resources, design-system resources and clone-picker presentation changes compile and pass the existing CI checks.

Remaining acceptance is visual/runtime validation on the target Android 11 tablet. Build success is not treated as proof of target-device visual correctness or guest runtime success.


## Hadeel private-space personalization

The current private screen only is personalized for Hadeel. It uses blush pink, lavender, cream and rose-gold surfaces, a crown/name sticker, butterfly/flower/sparkle vector decorations, Hadeel guest-card signatures, matching clone-picker styling, private-only system-bar colors and subtle entrance/card motion. The Quotes home design is intentionally unchanged by this personalization.

The decorative animation is cancelled when leaving the private screen. This phase does not change BlackBox behavior, permissions, package IDs or Android API targets.
