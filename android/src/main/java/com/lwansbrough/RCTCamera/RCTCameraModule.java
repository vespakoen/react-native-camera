/**
 * Created by Fabrice Armisen (farmisen@gmail.com) on 1/4/16.
 */

package com.lwansbrough.RCTCamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Surface;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Environment;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import com.facebook.react.bridge.*;

import javax.annotation.Nullable;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class RCTCameraModule extends ReactContextBaseJavaModule implements MediaRecorder.OnInfoListener {
    private static final String TAG = "RCTCameraModule";

    public static final int RCT_CAMERA_ASPECT_FILL = 0;
    public static final int RCT_CAMERA_ASPECT_FIT = 1;
    public static final int RCT_CAMERA_ASPECT_STRETCH = 2;
    public static final int RCT_CAMERA_CAPTURE_MODE_STILL = 0;
    public static final int RCT_CAMERA_CAPTURE_MODE_VIDEO = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_MEMORY = 0;
    public static final int RCT_CAMERA_CAPTURE_TARGET_DISK = 1;
    public static final int RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL = 2;
    public static final int RCT_CAMERA_CAPTURE_TARGET_TEMP = 3;
    public static final int RCT_CAMERA_ORIENTATION_AUTO = 0;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT = 1;
    public static final int RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT = 2;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT = 3;
    public static final int RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN = 4;
    public static final int RCT_CAMERA_TYPE_FRONT = 1;
    public static final int RCT_CAMERA_TYPE_BACK = 2;
    public static final int RCT_CAMERA_FLASH_MODE_OFF = 0;
    public static final int RCT_CAMERA_FLASH_MODE_ON = 1;
    public static final int RCT_CAMERA_FLASH_MODE_AUTO = 2;
    public static final int RCT_CAMERA_TORCH_MODE_OFF = 0;
    public static final int RCT_CAMERA_TORCH_MODE_ON = 1;
    public static final int RCT_CAMERA_TORCH_MODE_AUTO = 2;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private final ReactApplicationContext _reactContext;
    private MediaRecorder mMediaRecorder;
    private Promise mVideoPromise;
    private String mVideoDestinationUri;
    private Camera mCamera;

    public RCTCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RCTCameraModule";
    }

    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
            {
                put("Aspect", getAspectConstants());
                put("Type", getTypeConstants());
                put("CaptureQuality", getCaptureQualityConstants());
                put("CaptureMode", getCaptureModeConstants());
                put("CaptureTarget", getCaptureTargetConstants());
                put("Orientation", getOrientationConstants());
                put("FlashMode", getFlashModeConstants());
                put("TorchMode", getTorchModeConstants());
            }

            private Map<String, Object> getAspectConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("stretch", RCT_CAMERA_ASPECT_STRETCH);
                        put("fit", RCT_CAMERA_ASPECT_FIT);
                        put("fill", RCT_CAMERA_ASPECT_FILL);
                    }
                });
            }

            private Map<String, Object> getTypeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("front", RCT_CAMERA_TYPE_FRONT);
                        put("back", RCT_CAMERA_TYPE_BACK);
                    }
                });
            }

            private Map<String, Object> getCaptureQualityConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("low", "low");
                        put("medium", "medium");
                        put("high", "high");
                    }
                });
            }

            private Map<String, Object> getCaptureModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("still", RCT_CAMERA_CAPTURE_MODE_STILL);
                        put("video", RCT_CAMERA_CAPTURE_MODE_VIDEO);
                    }
                });
            }

            private Map<String, Object> getCaptureTargetConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("memory", RCT_CAMERA_CAPTURE_TARGET_MEMORY);
                        put("disk", RCT_CAMERA_CAPTURE_TARGET_DISK);
                        put("cameraRoll", RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL);
                        put("temp", RCT_CAMERA_CAPTURE_TARGET_TEMP);
                    }
                });
            }

            private Map<String, Object> getOrientationConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("auto", RCT_CAMERA_ORIENTATION_AUTO);
                        put("landscapeLeft", RCT_CAMERA_ORIENTATION_LANDSCAPE_LEFT);
                        put("landscapeRight", RCT_CAMERA_ORIENTATION_LANDSCAPE_RIGHT);
                        put("portrait", RCT_CAMERA_ORIENTATION_PORTRAIT);
                        put("portraitUpsideDown", RCT_CAMERA_ORIENTATION_PORTRAIT_UPSIDE_DOWN);
                    }
                });
            }

            private Map<String, Object> getFlashModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_FLASH_MODE_OFF);
                        put("on", RCT_CAMERA_FLASH_MODE_ON);
                        put("auto", RCT_CAMERA_FLASH_MODE_AUTO);
                    }
                });
            }

            private Map<String, Object> getTorchModeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("off", RCT_CAMERA_TORCH_MODE_OFF);
                        put("on", RCT_CAMERA_TORCH_MODE_ON);
                        put("auto", RCT_CAMERA_TORCH_MODE_AUTO);
                    }
                });
            }
        });
    }

    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (
            what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
            what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
        ) {
            Log.v(TAG, "MEDIAENCODER_RESOLVE");
            if (mVideoPromise != null) {
                mVideoPromise.resolve(mVideoDestinationUri);
                mVideoPromise = null;
            }
        }
    }

    @ReactMethod
    public void capture(final ReadableMap options, final Promise promise) {
        RCTCamera rCamera = RCTCamera.getInstance();
        Camera camera = rCamera.acquireCameraInstance(options.getInt("type"));
        if (null == camera) {
            promise.reject("No camera found.");
            return;
        }

        if (options.getInt("mode") == RCT_CAMERA_CAPTURE_MODE_VIDEO) {
            // Get the video destination (temp file only at the moment)
            final File destination = getTempMediaFile(MEDIA_TYPE_VIDEO);
            // Store promise, camera and file location for later
            mVideoPromise = promise;
            mCamera = camera;
            mVideoDestinationUri = Uri.fromFile(destination).toString();

            // Try a hack for samsung HQ, didn't help (http://stackoverflow.com/questions/7225571/camcorderprofile-quality-high-resolution-produces-green-flickering-video)
            // parameters.set("cam_mode", 1);
            // parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            // camera.setParameters(parameters);

            // Grab size from camera instance
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size previewSize = parameters.getPreviewSize();

            // Create a profile (HQ for now)
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            // Modify profile
            profile.fileFormat = MediaRecorder.OutputFormat.THREE_GPP;
            // profile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
            profile.audioCodec = MediaRecorder.AudioEncoder.AMR_NB;
            profile.videoCodec = MediaRecorder.VideoEncoder.H264;
            profile.videoBitRate = 15;
            profile.videoFrameRate = 30;
            profile.videoFrameWidth = 720; // previewSize.width;
            profile.videoFrameHeight = 480; // previewSize.height;

            // Setup media recorder (watch out with modifying these options, the order is important!)
            // @see http://developer.android.com/guide/topics/media/camera.html#capture-video
            mMediaRecorder = new MediaRecorder();
            camera.unlock();
            mMediaRecorder.setCamera(camera);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            mMediaRecorder.setProfile(profile);
            // Same as profile settings from above, do not use both at the same time
            // mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            // mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            // mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            // mMediaRecorder.setVideoEncodingBitRate(15);
            // mMediaRecorder.setVideoFrameRate(30);
            // mMediaRecorder.setVideoSize(previewSize.width, previewSize.height);
            // mMediaRecorder.setAudioEncodingBitRate(50000);

            mMediaRecorder.setOutputFile(destination.getAbsolutePath());

            // Attach callback to handle maxDuration (@see onInfo method in this file)
            mMediaRecorder.setOnInfoListener(this);

            // Set maxDuration when given as option, didn't seem to work when passed in to the profile
            // On my devices, maxDuration couln't be less than 4 seconds
            if (options.hasKey("totalSeconds")) {
                int totalSeconds = options.getInt("totalSeconds");
                // @todo not sure if this is the case on all platforms:
                // if (totalSeconds < 4) {
                //   promise.reject("Duration cannot be less than 4 seconds");
                // }
                mMediaRecorder.setMaxDuration(totalSeconds * 1000);
            }
            // @todo setPreviewDisplay is documented as "optional", but I suspect it of being the cause of the resulting video not being playable.
            // (on my Nexus 7 and Samsung Galaxy S4 Mini)
            // I have tried getting access to the SurfaceTexture, and passing it in a seen below, that didn't yield any results (compiled fine, no playable video).
            // mMediaRecorder.setPreviewDisplay(new Surface(rCamera.getPreviewSurfaceTexture()));

            try {
                // prepare and start recording
                mMediaRecorder.prepare();
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException: " + e.getMessage());
                _stopCapture();
                promise.reject("IllegalStateException: " + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e.getMessage());
                _stopCapture();
                promise.reject("IOException: " + e.getMessage());
            }
            // Start the recording in the background (@see RecordVideoTask down below)
            new RecordVideoTask().execute(null, null, null);
        }

        if (options.getInt("mode") == RCT_CAMERA_CAPTURE_MODE_STILL) {
            if (options.getBoolean("playSoundOnCapture")) {
                MediaActionSound sound = new MediaActionSound();
                sound.play(MediaActionSound.SHUTTER_CLICK);
            }

            rCamera.setCaptureQuality(options.getInt("type"), options.getString("quality"));
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    camera.stopPreview();
                    camera.startPreview();
                    switch (options.getInt("target")) {
                        case RCT_CAMERA_CAPTURE_TARGET_MEMORY:
                            String encoded = Base64.encodeToString(data, Base64.DEFAULT);
                            promise.resolve(encoded);
                            break;
                        case RCT_CAMERA_CAPTURE_TARGET_CAMERA_ROLL:
                            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, bitmapOptions);
                            String url = MediaStore.Images.Media.insertImage(
                                    _reactContext.getContentResolver(),
                                    bitmap, options.getString("title"),
                                    options.getString("description"));
                            promise.resolve(url);
                            break;
                        case RCT_CAMERA_CAPTURE_TARGET_DISK:
                            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                            if (pictureFile == null) {
                                promise.reject("Error creating media file.");
                                return;
                            }

                            try {
                                FileOutputStream fos = new FileOutputStream(pictureFile);
                                fos.write(data);
                                fos.close();
                            } catch (FileNotFoundException e) {
                                promise.reject("File not found: " + e.getMessage());
                            } catch (IOException e) {
                                promise.reject("Error accessing file: " + e.getMessage());
                            }
                            promise.resolve(Uri.fromFile(pictureFile).toString());
                            break;
                        case RCT_CAMERA_CAPTURE_TARGET_TEMP:
                            File tempFile = getTempMediaFile(MEDIA_TYPE_IMAGE);

                            if (tempFile == null) {
                                promise.reject("Error creating media file.");
                                return;
                            }

                            try {
                                FileOutputStream fos = new FileOutputStream(tempFile);
                                fos.write(data);
                                fos.close();
                            } catch (FileNotFoundException e) {
                                promise.reject("File not found: " + e.getMessage());
                            } catch (IOException e) {
                                promise.reject("Error accessing file: " + e.getMessage());
                            }
                            promise.resolve(Uri.fromFile(tempFile).toString());
                            break;
                    }
                }
            });
        }
    }

    class RecordVideoTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            mMediaRecorder.start();
            return true;
        }
    }

    public void _stopCapture() {
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnInfoListener(null);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mCamera != null) {
            mCamera.lock();
        }
        mVideoPromise.resolve(mVideoDestinationUri);
    }

    @ReactMethod
    public void stopCapture(final Promise promise) {
        _stopCapture();
        promise.resolve(null);
    }

    @ReactMethod
    public void hasFlash(ReadableMap options, final Promise promise) {
        Camera camera = RCTCamera.getInstance().acquireCameraInstance(options.getInt("type"));
        if (null == camera) {
            promise.reject("No camera found.");
            return;
        }
        List<String> flashModes = camera.getParameters().getSupportedFlashModes();
        promise.resolve(null != flashModes && !flashModes.isEmpty());
    }

    private File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "RCTCameraModule");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory:" + mediaStorageDir.getAbsolutePath());
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            Log.e(TAG, "Unsupported media type:" + type);
            return null;
        }
        return mediaFile;
    }


    private File getTempMediaFile(int type) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File outputDir = _reactContext.getCacheDir();
            File outputFile;

            if (type == MEDIA_TYPE_IMAGE) {
                outputFile = File.createTempFile("IMG_" + timeStamp, ".jpg", outputDir);
            } else if (type == MEDIA_TYPE_VIDEO) {
                outputFile = File.createTempFile("VID_" + timeStamp, ".mp4", outputDir);
            } else {
                Log.e(TAG, "Unsupported media type:" + type);
                return null;
            }
            return outputFile;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
}
