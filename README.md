# Media Downloader — DVD v6.2 Share-to-DVD Media-Type Edition

**Dixit Pahwa © 2026 — Freeware**

This edition expands the downloader/player with the requested lawful, non-DRM SnapTube-style capabilities.

## Included


### Share-to-DVD download flow
- Android `SEND` / Share links are accepted by the app.
- When a supported public URL is shared to DVD, the app opens the download flow automatically instead of only filling the URL box.
- First choice: **Video** or **Music / Audio**.
- Video flow offers available public video variants and output formats such as MP4/WebM/MKV.
- Music flow prefers exposed audio variants and offers MP3/M4A/Opus/Auto output choices; MP3 conversion can be performed locally when the source is not already MP3.
- Quality, codec, FPS and bitrate metadata are shown when exposed by the source.
- `Best available` selects the best actually exposed stream for the chosen media type.
- The same Video/Music choice is also available when the user starts analysis from the DVD home screen.

### Advanced stream selection
- Public-stream analyzer for direct media URLs and HLS master playlists.
- Quality: 8K/4K/2K/1080p/720p/480p/360p when actually exposed by the source.
- FPS, codec, bitrate and format metadata where the source exposes it.
- Smart modes: Best available, Best compatibility, Data saver, Audio only and Video only.
- The app never invents a stream that the source does not expose.

### Download manager
- Persistent queue.
- Up to four simultaneous jobs.
- Priority ordering.
- Pause/resume/cancel/retry.
- Automatic retry.
- HTTP byte-range resume for progressive files.
- Speed limiter.
- Wi-Fi-only option.
- Duplicate policy: rename, skip or replace.
- Crash/restart recovery.
- Foreground-service background downloading.
- Notification pause/cancel controls.

### Adaptive/public streaming
- HLS master-playlist inspection.
- HLS variant selection metadata.
- Public HLS input can be exported to MP4 using Media3 Transformer.
- No DRM, signed-stream bypass, authentication bypass or paywall bypass.

### Audio
- AAC track extraction to M4A without re-encoding.
- AAC/M4A conversion through AndroidX Media3 Transformer.
- Audio-only download profile.
- Music categorisation and background playback.

### Video conversion
- H.264/AAC MP4 export.
- 1080p, 720p and 480p scaling.
- Original-resolution export.
- Media3 Transformer asynchronous progress/error handling.
- Hardware codecs are used when available through Android MediaCodec.

### Organisation/history
- Movies and Music directories.
- Safe filenames.
- Duplicate-safe naming.
- User-selected SAF download folder.
- Persistent download history up to 500 records.
- Completed/paused/cancelled/failed history states.
- Local playback from history.

### Player
- Media3 playback.
- Background audio playback.
- Picture-in-picture.
- Multiple audio tracks.
- Playback speed.
- Gesture seeking/volume/brightness and other existing player features in the project.

## Explicit exclusions requested by the project owner
- Built-in SnapTube-style browser: excluded.
- Subtitles: excluded.
- DRM/protected-stream bypass: excluded.
- Authentication/paywall/access-control bypass: excluded.

## Important support boundary
SnapTube advertises very broad multi-platform support, playlist/batch downloading, creator-link recognition and automatic lyrics. This project does **not** falsely claim universal website support. It processes direct/public media and supported public stream formats. A website that hides media behind authentication, DRM, anti-bot controls, signed access, or a private API is not bypassed.

## Conversion limitation
AndroidX Media3 Transformer supports transcoding/transmuxing and formats such as MP4, WebM, AAC, Ogg and WAV through the appropriate muxers, but it is not a universal MP3 encoder. Therefore this source package includes true local MP3 conversion using the MIT-licensed AME/LAME Android encoder; the encoder is configured for selectable audio bitrates.

## Build status
This is a source package. The included GitHub Actions workflow builds a debug APK on a runner with Android SDK/Gradle. The current environment does not contain the Android SDK or Gradle executable, so no local APK build is claimed.

## MP3 conversion and codec/bitrate selection

- MP3 is supported as a direct download when the source exposes an MP3 stream.
- MP3 conversion is a real local decode-to-PCM + MP3 encoding pipeline using AME/LAME.
- Audio bitrate presets: 64/96/128/192/256/320 kbps.
- Download profile exposes audio codec and video bitrate selection. Actual stream choice remains source-dependent: the app only uses variants that the source exposes and never fabricates a codec/bitrate.
- Video bitrate presets: 500 kbps/1/2/4/8/12 Mbps.

## v5.2 activation pass
- Requested stream profiles are now resolved through `StreamSelector` against variants actually exposed by `StreamCatalog`.
- HLS variants expose separate video/audio codec metadata and audio groups; self-contained variants can be selected for requested quality/bitrate/FPS.
- A requested MP3/M4A/MP4 output is no longer implemented by filename renaming: when conversion is required, the source is moved to a temporary `.source` file and `ConversionService` performs the actual conversion before marking the queue item completed.
- Media3 encoder settings are used for requested AAC audio bitrate and H.264 video bitrate during conversion.
- True MP3 conversion preserves the decoded sample rate/channel count and uses the selected MP3 bitrate.
- Player frame stepping now supports previous/next and 5-frame jumps.
- Player audio effects now expose the device Equalizer bands and LoudnessEnhancer gain instead of only fixed presets.
- Library now persists Favorite and Watched/Unwatched state.
- Subtitle UI and subtitle loading are removed from this edition, as explicitly requested.

## v6.0 public media discovery
- Site detection for YouTube, Facebook, Instagram, TikTok, X, Vimeo, Dailymotion, Reddit, Twitch and SoundCloud.
- Public creator/profile/channel URL recognition where the URL pattern is publicly identifiable.
- Public-page extraction only: HTML5 source links, OpenGraph media, JSON-LD media URLs and direct media links already exposed in the page.
- Discover Batch finds multiple publicly exposed media items on a page and queues selected items together.
- No browser, DRM bypass, login bypass, paywall bypass or private/internal API access is implemented.


## MX-style player/library upgrade
This revision adds richer local library filtering (favorites, watched, unwatched, continue watching), folder-aware sorting/search, rename/delete actions, persistent A/V-sync preference, and settings-backed volume boost. Subtitles and network playback remain intentionally excluded.
