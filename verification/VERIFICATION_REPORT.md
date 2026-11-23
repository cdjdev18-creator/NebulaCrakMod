# NebulaCrakMod — Verification Report (LoadingOverlayMixin definitive diagnosis)

**Date:** 2026-11-23  
**Project:** C:\Users\seven\Guithub\NebulaCrakMod  
**MC/Fabric/Loom:** 1.21.11 / Fabric Loader 0.18.2 / Loom 1.18.4 / Gradle 9.7.1  
**Mappings:** Official Mojang (loom.officialMojangMappings())

## Phase 1 — Fresh run evidence

### Stale/active processes identified (before new confirmation)
| PID | Start | Command | Action |
|---|---:|---|---|
| 8572 | 2026-11-23 11:08:11 | VS Code Red Hat Java JDT LS | **Not touched** (VS Code) |
| 8104 | 2026-11-23 13:19:12 | gradlew.bat runClient --no-daemon wrapper | Observed; belongs to earlier runClient |
| 20952 | 2026-11-23 13:19:15 | Gradle 9.7.1 daemon/bootstrap for runClient | Observed |
| 15280 | 2026-11-23 13:27:10 | C:\Users\seven\.jdks\ms-21.0.10\bin\java.exe -Dfabric.dli.config=...\loom-cache\launch.cfg -Dfabric.dli.env=client ... net.fabricmc.devlaunchinjector.Main | **Current Minecraft runClient process**; terminated after evidence capture (PID 15280 only) |

### Log freshness
- %APPDATA%\.minecraft\logs\latest.log before diagnosis: **LastWriteTime 2026-11-23 13:07:44, Length 31086** (stale crash log; did not update during relaunch).
- Loom runClient active log: un\logs\latest.log — **LastWriteTime 2026-11-23 13:29:50, Length 110746** during run; copied to erification/logs/runClient-after-fill-fix.log (LastWriteTime 2026-11-23 13:30:09).
- Evidence: erification/logs/runClient-after-fill-fix.log contains full session including NebulaCrakMod init, no crash, game reached in-world/chat state.

## Phase 2 — Real LoadingOverlay/GuiGraphics target investigation

| Item | Finding |
|---|---|
| LoadingOverlay render method | public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) (LoadingOverlay.java:63) |
| Real background fill calls in ender | graphics.fill(0,0,width,height,replaceAlpha(...)) at lines 83,94 — **5-int** GuiGraphics.fill(int,int,int,int,int) |
| Progress bar fill calls | drawProgressBar(...) uses 5-int fills at lines 150-154 — also 5-int, different semantic target (not full-screen background) |
| GuiGraphics fill overloads (MC 1.21.11 source) | ill(int,int,int,int,int) line 180; ill(RenderPipeline,int,int,int,int,int) line 184; ill(RenderPipeline,TextureSetup,int,int,int,int) line 204 |
| Owner/context | Calls are on GuiGraphics instance parameter inside LoadingOverlay.render(...) / drawProgressBar(...) |
| Mixin redirect before fix | @Redirect(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/GuiGraphics;fill(IIIIII)V")) — **wrong descriptor (6 ints)** |
| Mixin redirect after fix | @Redirect(method="render", at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V")) — matches actual 5-int calls |

**Root cause:** The redirect targeted a non-existent GuiGraphics.fill 6-int descriptor. At runtime (dev, Mojang names mapped to intermediary in error context) Mixin scanned 0 targets and failed injection check: Redirector nebulacrak(Lnet/minecraft/class_332;IIIII)V ... (0/1) succeeded. Scanned 0 target(s). No refMap loaded. The descriptor mismatch is the primary cause.

## Phase 3 — Refmap investigation

| Item | Finding |
|---|---|
| src/main/resources/nebulacrakmod.mixins.json | No efmap field declared. Contains client: [ChatListenerFilterMixin, TitleScreenMixin, LoadingOverlayMixin]. |
| uild/resources/main/nebulacrakmod.mixins.json | Same, no refmap. |
| Jar uild/libs/nebulacrakmod-1.0.0.jar | Contains 
ebulacrakmod.mixins.json; **no *refmap*.json entry**. |
| Build/loom-cache refmap files | **None found** (uild/**/*refmap*, uild/loom-cache/**/*refmap*). |
| Relevance | "No refMap loaded" appears in Mixin injection failure message; in this Loom+Mojang-mappings dev/runClient environment, named targets are used directly and no refmap is generated/required for these class/method targets. The crash was caused by descriptor/target mismatch, not missing refmap. After fixing descriptor, client boots with no mixin injection errors. **Refmap not the blocking cause here.** |

## Phase 4 — Fix applied

**File:** src/main/java/com/nebulacrak/mixin/LoadingOverlayMixin.java

- Changed redirect target: GuiGraphics.fill(IIIIII)V -> GuiGraphics.fill(IIIII)V.
- Preserved: ORIGINAL/COLOR/TRANSPARENT/IMAGE modes; alpha handling via 
ebulacrak redirect; no cancellation of LoadingOverlay.render(); full-screen detection (x1<=0,y1<=0,x2>=guiScaledWidth,y2>=guiScaledHeight) so progress-bar fills are passed through; logo/progress/animation remain vanilla-rendered.

## Phase 5 — Validation

| Command | Result |
|---|---|
| .\gradlew.bat compileJava --no-daemon --stacktrace | **BUILD SUCCESSFUL** |
| .\gradlew.bat build --no-daemon --stacktrace | **BUILD SUCCESSFUL** |
| .\gradlew.bat runClient --no-daemon --stacktrace | **FUNCIONA (arranque/utilizable)** — no crash, no LoadingOverlayMixin injection error. un/logs/latest.log updated to this run. Game reached in-world state ([CHAT] badOdreams, you are now logged in! ..., Meteor saving). |

### Mixin/application checks
- LoadingOverlayMixin injection crash: **FUNCIONA / RESUELTO** (not present in new log).
- TitleScreenMixin runtime: **NO VERIFICADO** (no explicit error; not visually confirmed).
- ChatListenerFilterMixin runtime: **NO VERIFICADO** (no explicit error; not functionally exercised).

## Remaining in-game verification (not performed)
- Loading screen ORIGINAL/COLOR/TRANSPARENT/IMAGE, real transparency, logo/progress/animation: **NO VERIFICADO** (requires controlled visual test during loading overlay).
- NebulaGUI, HUD, ModuleBrowserScreen, selectors, keybinds, persistence: **NO VERIFICADO**.

## Status legend
- **FUNCIONA**: confirmed by executed evidence.
- **FALLA**: confirmed failure with evidence.
- **NO VERIFICADO**: not executed/confirmed.
