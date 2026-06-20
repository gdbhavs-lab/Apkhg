package {{PACKAGE}};

import android.util.Log;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * أمثلة عملية لاستخدام Hardware Device Manager
 * 
 * هذا الملف يوضح كيفية استخدام نظام التحكم بالأجهزة في تطبيقك
 */
public class HardwareUsageExample {
    
    private static final String TAG = "HardwareExample";
    private HardwareDeviceManager hardwareManager;
    
    public HardwareUsageExample(HardwareDeviceManager hardwareManager) {
        this.hardwareManager = hardwareManager;
    }
    
    /**
     * مثال 1: الحصول على معلومات جميع الأجهزة
     */
    public void printAllDevices() {
        try {
            String devicesJson = hardwareManager.getAllDevices();
            JSONArray devices = new JSONArray(devicesJson);
            
            Log.d(TAG, "Total devices: " + devices.length());
            
            for (int i = 0; i < devices.length(); i++) {
                JSONObject device = devices.getJSONObject(i);
                Log.d(TAG, "Device: " + device.getString("name") + 
                      " - Available: " + device.getBoolean("available"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error printing devices", e);
        }
    }
    
    /**
     * مثال 2: الحصول على معلومات جهاز معين
     */
    public void printDeviceInfo(String deviceName) {
        try {
            String deviceJson = hardwareManager.getDeviceInfo(deviceName);
            JSONObject device = new JSONObject(deviceJson);
            
            Log.d(TAG, "Device: " + device.getString("name"));
            Log.d(TAG, "Version: " + device.getString("version"));
            Log.d(TAG, "Description: " + device.getString("description"));
            Log.d(TAG, "Available: " + device.getBoolean("available"));
        } catch (JSONException e) {
            Log.e(TAG, "Error printing device info", e);
        }
    }
    
    /**
     * مثال 3: الحصول على حالة جهاز
     */
    public void printDeviceStatus(String deviceName) {
        try {
            String statusJson = hardwareManager.getDeviceStatus(deviceName);
            JSONObject status = new JSONObject(statusJson);
            
            Log.d(TAG, "Device: " + status.getString("device"));
            Log.d(TAG, "Enabled: " + status.getBoolean("enabled"));
            Log.d(TAG, "Timestamp: " + status.getLong("timestamp"));
        } catch (JSONException e) {
            Log.e(TAG, "Error printing device status", e);
        }
    }
    
    /**
     * مثال 4: تفعيل الكاميرا
     */
    public void enableCamera() {
        hardwareManager.enableDevice("camera", true);
        Log.d(TAG, "Camera enabled");
    }
    
    /**
     * مثال 5: تعطيل الكاميرا
     */
    public void disableCamera() {
        hardwareManager.enableDevice("camera", false);
        Log.d(TAG, "Camera disabled");
    }
    
    /**
     * مثال 6: تفعيل الميكروفون
     */
    public void enableMicrophone() {
        hardwareManager.enableDevice("microphone", true);
        Log.d(TAG, "Microphone enabled");
    }
    
    /**
     * مثال 7: تعطيل الميكروفون
     */
    public void disableMicrophone() {
        hardwareManager.enableDevice("microphone", false);
        Log.d(TAG, "Microphone disabled");
    }
    
    /**
     * مثال 8: الحصول على معلومات الكاميرا
     */
    public void printCameraInfo() {
        try {
            String cameraJson = hardwareManager.getCameraInfo();
            JSONObject camera = new JSONObject(cameraJson);
            
            int cameraCount = camera.getInt("camera_count");
            Log.d(TAG, "Number of cameras: " + cameraCount);
            
            JSONArray cameras = camera.getJSONArray("cameras");
            for (int i = 0; i < cameras.length(); i++) {
                JSONObject cam = cameras.getJSONObject(i);
                Log.d(TAG, "Camera " + cam.getInt("id") + 
                      ": " + cam.getString("facing") + 
                      " - Orientation: " + cam.getInt("orientation"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error printing camera info", e);
        }
    }
    
    /**
     * مثال 9: الحصول على معلومات الصوت
     */
    public void printAudioInfo() {
        try {
            String audioJson = hardwareManager.getAudioInfo();
            JSONObject audio = new JSONObject(audioJson);
            
            Log.d(TAG, "Music volume: " + audio.getInt("music_volume") + 
                  "/" + audio.getInt("music_max_volume"));
            Log.d(TAG, "Ringer mode: " + audio.getInt("ringer_mode"));
            Log.d(TAG, "Speaker on: " + audio.getBoolean("speaker_on"));
            Log.d(TAG, "Music active: " + audio.getBoolean("music_active"));
        } catch (JSONException e) {
            Log.e(TAG, "Error printing audio info", e);
        }
    }
    
    /**
     * مثال 10: ضبط مستوى الصوت
     */
    public void setVolume(int volume) {
        hardwareManager.setVolume(volume);
        Log.d(TAG, "Volume set to: " + volume);
    }
    
    /**
     * مثال 11: الحصول على معلومات الجهاز
     */
    public void printDeviceHardwareInfo() {
        try {
            String hwJson = hardwareManager.getDeviceHardwareInfo();
            JSONObject hw = new JSONObject(hwJson);
            
            Log.d(TAG, "Device: " + hw.getString("device_name"));
            Log.d(TAG, "Manufacturer: " + hw.getString("manufacturer"));
            Log.d(TAG, "Model: " + hw.getString("model"));
            Log.d(TAG, "Android Version: " + hw.getInt("android_version"));
            Log.d(TAG, "Release: " + hw.getString("release"));
            Log.d(TAG, "ID: " + hw.getString("id"));
        } catch (JSONException e) {
            Log.e(TAG, "Error printing hardware info", e);
        }
    }
    
    /**
     * مثال 12: الحصول على معلومات المستشعرات
     */
    public void printSensorInfo() {
        try {
            String sensorJson = hardwareManager.getSensorInfo();
            JSONObject sensors = new JSONObject(sensorJson);
            
            Log.d(TAG, "Accelerometer: " + sensors.getString("accelerometer"));
            Log.d(TAG, "Compass: " + sensors.getString("compass"));
            Log.d(TAG, "Gyroscope: " + sensors.getString("gyroscope"));
            Log.d(TAG, "Proximity: " + sensors.getString("proximity"));
            Log.d(TAG, "Light: " + sensors.getString("light"));
        } catch (JSONException e) {
            Log.e(TAG, "Error printing sensor info", e);
        }
    }
    
    /**
     * مثال 13: تفعيل البلوتوث
     */
    public void enableBluetooth() {
        hardwareManager.enableDevice("bluetooth", true);
        Log.d(TAG, "Bluetooth enabled");
    }
    
    /**
     * مثال 14: تفعيل WiFi
     */
    public void enableWiFi() {
        hardwareManager.enableDevice("wifi", true);
        Log.d(TAG, "WiFi enabled");
    }
    
    /**
     * مثال 15: تفعيل GPS
     */
    public void enableGPS() {
        hardwareManager.enableDevice("gps", true);
        Log.d(TAG, "GPS enabled");
    }
    
    /**
     * مثال 16: تفعيل NFC
     */
    public void enableNFC() {
        hardwareManager.enableDevice("nfc", true);
        Log.d(TAG, "NFC enabled");
    }
    
    /**
     * مثال 17: تفعيل الاهتزاز
     */
    public void enableVibrator() {
        hardwareManager.enableDevice("vibrator", true);
        Log.d(TAG, "Vibrator enabled");
    }
    
    /**
     * مثال 18: التحقق من توفر جهاز معين
     */
    public boolean isDeviceAvailable(String deviceName) {
        try {
            String deviceJson = hardwareManager.getDeviceInfo(deviceName);
            JSONObject device = new JSONObject(deviceJson);
            return device.getBoolean("available");
        } catch (JSONException e) {
            Log.e(TAG, "Error checking device availability", e);
            return false;
        }
    }
    
    /**
     * مثال 19: تفعيل/تعطيل عدة أجهزة
     */
    public void configureDevices(boolean enableCamera, boolean enableMic, 
                                 boolean enableBluetooth, boolean enableWiFi) {
        hardwareManager.enableDevice("camera", enableCamera);
        hardwareManager.enableDevice("microphone", enableMic);
        hardwareManager.enableDevice("bluetooth", enableBluetooth);
        hardwareManager.enableDevice("wifi", enableWiFi);
        
        Log.d(TAG, "Devices configured");
    }
    
    /**
     * مثال 20: سيناريو كامل - تطبيق مكالمات فيديو
     */
    public void setupVideoCallApp() {
        // تفعيل الأجهزة المطلوبة
        enableCamera();
        enableMicrophone();
        enableBluetooth();
        enableWiFi();
        
        // ضبط الصوت
        setVolume(10);
        
        // الحصول على معلومات الكاميرا
        printCameraInfo();
        
        // الحصول على معلومات الصوت
        printAudioInfo();
        
        Log.d(TAG, "Video call app setup complete");
    }
    
    /**
     * مثال 21: سيناريو كامل - تطبيق الملاحة
     */
    public void setupNavigationApp() {
        // تفعيل الأجهزة المطلوبة
        enableGPS();
        enableWiFi();
        
        // الحصول على معلومات المستشعرات
        printSensorInfo();
        
        // الحصول على معلومات الجهاز
        printDeviceHardwareInfo();
        
        Log.d(TAG, "Navigation app setup complete");
    }
    
    /**
     * مثال 22: سيناريو كامل - تطبيق التسجيل الصوتي
     */
    public void setupAudioRecorderApp() {
        // تفعيل الأجهزة المطلوبة
        enableMicrophone();
        
        // ضبط الصوت
        setVolume(12);
        
        // الحصول على معلومات الصوت
        printAudioInfo();
        
        Log.d(TAG, "Audio recorder app setup complete");
    }
    
    /**
     * مثال 23: سيناريو كامل - تطبيق الكاميرا
     */
    public void setupCameraApp() {
        // تفعيل الأجهزة المطلوبة
        enableCamera();
        
        // الحصول على معلومات الكاميرا
        printCameraInfo();
        
        // تفعيل الاهتزاز للتنبيهات
        enableVibrator();
        
        Log.d(TAG, "Camera app setup complete");
    }
    
    /**
     * مثال 24: تنظيف الموارد
     */
    public void cleanup() {
        // تعطيل جميع الأجهزة
        hardwareManager.enableDevice("camera", false);
        hardwareManager.enableDevice("microphone", false);
        hardwareManager.enableDevice("bluetooth", false);
        hardwareManager.enableDevice("wifi", false);
        hardwareManager.enableDevice("gps", false);
        
        // تنظيف الموارد
        hardwareManager.shutdown();
        
        Log.d(TAG, "Cleanup complete");
    }
}
