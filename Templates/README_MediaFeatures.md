# Media Framework Features - دليل الميزات المتقدمة

## نظرة عامة
هذه الميزات مستوحاة من Android AOSP Media Framework وتوفر وصولاً متقدماً لمعالجة الوسائط.

---

## 1. MediaCodec Bridge (100% متوافق)
**الملف:** `MediaCodecBridge.template`

### الاستخدام في JavaScript:
```javascript
// تحويل الفيديو
MediaBridge.transcodeVideo("input.mp4", "output.mp4", 2000000, 1280, 720, "onProgress");

// ضغط الفيديو
MediaBridge.compressVideo("video.mp4", "medium", "onComplete");
// quality: "low", "medium", "high"

// استخراج الصوت من فيديو
MediaBridge.extractAudio("video.mp4", "audio.m4a", "onComplete");

// معلومات الملف
var info = JSON.parse(MediaBridge.getVideoInfo("video.mp4"));
console.log(info.tracks[0].width, info.tracks[0].height);

// الـ Codecs المدعومة
var codecs = JSON.parse(MediaBridge.getSupportedVideoCodecs());

// التحكم
MediaBridge.cancelOperation();
var progress = MediaBridge.getProgress();

// Callback
function onProgress(event, data) {
    if (event === "progress") console.log(data + "%");
    if (event === "success") console.log("Done: " + data);
    if (event === "error") console.log("Error: " + data);
}
```

---

## 2. Camera2 Helper (100% متوافق)
**الملف:** `Camera2Helper.template`

### الاستخدام في JavaScript:
```javascript
// قائمة الكاميرات
var cameras = JSON.parse(Camera2.getCameraList());
// [{id: "0", facing: "back"}, {id: "1", facing: "front"}]

// فتح الكاميرا
Camera2.openCamera("0", "onCameraEvent");

// التقاط صورة
Camera2.takePicture("onCapture");
Camera2.takePictureWithSettings(1920, 1080, 95, "onCapture");

// التحكم اليدوي
Camera2.setManualFocus(0.5);      // 0.0 (بعيد) إلى 1.0 (قريب)
Camera2.setAutoFocus();
Camera2.setExposureCompensation(1.0);  // -2.0 إلى 2.0
Camera2.setISO(400);
Camera2.setShutterSpeed(16666666);     // nanoseconds (1/60 sec)
Camera2.setWhiteBalance("daylight");   // auto, daylight, cloudy, tungsten, fluorescent
Camera2.setZoom(2.0);

// الفلاش
Camera2.enableFlash("auto");  // off, on, auto, torch

// كشف الوجوه
Camera2.enableFaceDetection("onFaceDetected");
Camera2.disableFaceDetection();

// معلومات
var caps = JSON.parse(Camera2.getCapabilities("0"));
var resolutions = JSON.parse(Camera2.getSupportedResolutions("0"));
var settings = JSON.parse(Camera2.getCurrentSettings());

// إغلاق
Camera2.closeCamera();
Camera2.switchCamera();

// Callbacks
function onCameraEvent(event, data) {
    if (event === "opened") console.log("Camera opened: " + data);
    if (event === "error") console.log("Error: " + data);
}

function onCapture(event, data) {
    if (event === "success") {
        console.log("Photo saved: " + data);
        // data = مسار الملف
    }
}
```

---

## 3. Audio Processor (90% متوافق)
**الملف:** `AudioProcessor.template`

### الاستخدام في JavaScript:
```javascript
// ربط بمشغل صوت (audioSessionId من MediaPlayer)
AudioFX.attachToPlayer(audioSessionId);

// === المعادل الصوتي (Equalizer) ===
AudioFX.enableEqualizer(true);
AudioFX.setEqualizerPreset(3);  // رقم الـ preset
AudioFX.setEqualizerBand(0, 500);  // band: 0-4, level: -1500 to 1500

var presets = JSON.parse(AudioFX.getEqualizerPresets());
// [{index: 0, name: "Normal"}, {index: 1, name: "Classical"}, ...]

var bands = JSON.parse(AudioFX.getEqualizerBands());
// {minLevel: -1500, maxLevel: 1500, bands: [...]}

// === تعزيز الباس ===
AudioFX.enableBassBoost(true);
AudioFX.setBassBoostStrength(800);  // 0-1000

// === الصوت المحيطي (Virtualizer) ===
AudioFX.enableVirtualizer(true);
AudioFX.setVirtualizerStrength(500);  // 0-1000

// === الصدى (Reverb) ===
AudioFX.enableReverb(true);
AudioFX.setReverbPreset("largehall");
// none, smallroom, mediumroom, largeroom, mediumhall, largehall, plate

// === التصور الصوتي (Visualization) ===
AudioFX.startVisualization(audioSessionId, "onVisualization");
AudioFX.stopVisualization();

var waveform = JSON.parse(AudioFX.getWaveform());  // مصفوفة 0-255
var fft = JSON.parse(AudioFX.getFFT());  // طيف الترددات

function onVisualization(type) {
    if (type === "waveform") {
        var data = JSON.parse(AudioFX.getWaveform());
        drawWaveform(data);
    }
    if (type === "fft") {
        var data = JSON.parse(AudioFX.getFFT());
        drawSpectrum(data);
    }
}

// === التسجيل مع التأثيرات ===
AudioFX.startRecording("recording.wav", true, true);
// المعاملات: filename, enableNoiseSuppression, enableEchoCancellation

var path = AudioFX.stopRecording();
var isRec = AudioFX.isRecording();

// === تحليل الصوت ===
var level = AudioFX.getAudioLevel();
var peak = AudioFX.getPeakLevel();

// === معلومات ===
var caps = JSON.parse(AudioFX.getAudioCapabilities());
var supported = JSON.parse(AudioFX.isEffectSupported("noisesuppressor"));
```

---

## التفعيل في build_pro.bat

```batch
:: لتفعيل MediaCodec
set "MEDIA_CODEC_ENABLED=true"

:: لتفعيل Camera2
set "CAMERA2_ENABLED=true"
set "PERM_CAMERA=true"

:: لتفعيل Audio FX
set "AUDIO_FX_ENABLED=true"
set "PERM_MICROPHONE=true"
```

---

## الأذونات المطلوبة

| الميزة | الأذونات |
|--------|----------|
| MediaCodec | `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` |
| Camera2 | `CAMERA` |
| Audio FX | `RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS` |

---

## ملاحظات تقنية

1. **MediaCodec**: يستخدم hardware acceleration عند توفره
2. **Camera2**: يتطلب Android 5.0+ (API 21)
3. **Audio FX**: بعض التأثيرات تعتمد على الجهاز

---

## المصدر
هذه الميزات مستوحاة من:
- `media/libstagefright/MediaCodec.cpp`
- `camera/camera2/ICameraDeviceUser.cpp`
- `services/audioflinger/AudioFlinger.cpp`
