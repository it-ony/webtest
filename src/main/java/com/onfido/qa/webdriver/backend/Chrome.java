package com.onfido.qa.webdriver.backend;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.service.DriverService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.isEmpty;

class Chrome implements BrowserFactory {

    public static final int DEVICE_WIDTH = 360;
    public static final int DEVICE_HEIGHT = 640;
    public static final float DEVICE_PIXEL_RATIO = 3.0F;

    @Override
    public RemoteWebDriver createDriver(DriverService service, Capabilities capabilities) {
        return new ChromeDriver((ChromeDriverService) service, (ChromeOptions) capabilities);
    }

    @Override
    public MutableCapabilities getOptions(DesiredCapabilities capabilities, Config config, Properties properties) {
        var chromeOptions = new ChromeOptions();
        var filePathForFakeCapture = properties.getProperty("filePathForFakeCapture", "./");

        boolean allowOrigins = Boolean.parseBoolean(properties.getProperty("remoteAllowOrigins", "false"));
        if (allowOrigins){
            chromeOptions.addArguments("--remote-allow-origins=*");
        }

        chromeOptions.setAcceptInsecureCerts(config.acceptInsecureCertificates);

        chromeOptions.setHeadless(Boolean.parseBoolean(properties.getProperty("headless", "false")));

        if (config.enableMicrophoneCameraAccess) {
            chromeOptions.addArguments("--use-fake-device-for-media-stream");
            chromeOptions.addArguments("--use-fake-ui-for-media-stream");

            if (!isEmpty(config.fileForFakeAudioCapture)) {
                chromeOptions.addArguments("--use-file-for-fake-audio-capture=" + getCanonicalPath(filePathForFakeCapture + config.fileForFakeAudioCapture));
            }

            if (!isEmpty(config.fileForFakeVideoCapture)) {
                chromeOptions.addArguments("--use-file-for-fake-video-capture=" + getCanonicalPath(filePathForFakeCapture + config.fileForFakeVideoCapture));
            }
        }

        if (config.requiresMobileDevice) {

            if (!config.allowMobileEmulation()) {
                throw new RuntimeException("Mobile emulation is not allowed.");
            }

            setupMobileDevice(chromeOptions, config);
        }

        return chromeOptions.merge(capabilities);
    }

    private String getCanonicalPath(String filePath) {
        try {
            return isWindowsPath(filePath) ? new File(filePath).getPath() : new File(filePath).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isWindowsPath(String filePath) {
        return  filePath.matches(
                "([a-zA-Z]:|\\\\)" +
                "(\\\\[^<>:\"/\\\\|?*\u0000-\u001f]+)+")
                &&
                !filePath.matches(
                "(|.*\\\\)(?i:con|prn|aux|nul|com[1-9]|lpt[1-9])" +
                "(\\.[^.]*)?");
    }

    protected void setupMobileDevice(ChromeOptions chromeOptions, Config config) {
        var mobile = config.mobileDevice;

        var deviceMetrics = new HashMap<String, Object>();
        deviceMetrics.put("width", mobile.width() == 0 ? DEVICE_WIDTH : mobile.width());
        deviceMetrics.put("height", mobile.height() == 0 ? DEVICE_HEIGHT : mobile.height());
        deviceMetrics.put("pixelRatio", mobile.pixelRation() == 0 ? DEVICE_PIXEL_RATIO : mobile.pixelRation());

        var mobileEmulation = new HashMap<String, Object>();
        mobileEmulation.put("deviceMetrics", deviceMetrics);
        mobileEmulation.put("userAgent", "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36");

        chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
    }
}
