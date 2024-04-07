package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import com.fongmi.android.tv.utils.Notify;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.google.common.net.HttpHeaders;
import com.orhanobut.logger.Logger;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomWebView extends WebView {

    private static final String TAG = CustomWebView.class.getSimpleName();
    private static final String BLANK = "about:blank";

    private Map<String, String> headers;
    private WebResourceResponse empty;
    private ParseCallback callback;
    private AlertDialog dialog;
    private Runnable timer;
    private boolean detect;
    private String click;
    private String from;
    private String key;

    public static CustomWebView create(@NonNull Context context) {
        initTbs();
        return new CustomWebView(context);
    }

    public CustomWebView(@NonNull Context context) {
        super(context);
        initSettings();
        showTbs();
    }

    private static void initTbs() {
        if (Setting.getParseWebView() == 0) QbSdk.forceSysWebView();
        else QbSdk.unForceSysWebView();
    }

    private void showTbs() {
        if (this.getIsX5Core())  Notify.show(R.string.x5webview_parsing);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initSettings() {
        this.timer = () -> stop(true);
        this.empty = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        getSettings().setSupportZoom(true);
        getSettings().setUseWideViewPort(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setUserAgentString(Setting.getUa());
        getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        if (Build.VERSION.SDK_INT >= 17) getSettings().setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 21) CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        if (Build.VERSION.SDK_INT >= 21) getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        setWebViewClient(webViewClient());
        setWebChromeClient(webChromeClient());
    }

    public CustomWebView start(String key, String from, Map<String, String> headers, String url, String click, ParseCallback callback, boolean detect) {
        App.post(timer, Constant.TIMEOUT_PARSE_WEB);
        this.callback = callback;
        this.headers = headers;
        this.detect = detect;
        this.click = click;
        this.from = from;
        this.key = key;
        start(url, headers);
        return this;
    }

    private void start(String url, Map<String, String> headers) {
        checkHeader(url, headers);
        loadUrl(url, headers);
    }

    private void checkHeader(String url, Map<String, String> headers) {
        for (String key : headers.keySet()) {
            if (HttpHeaders.COOKIE.equalsIgnoreCase(key)) CookieManager.getInstance().setCookie(url, headers.get(key));
            if (HttpHeaders.USER_AGENT.equalsIgnoreCase(key)) getSettings().setUserAgentString(headers.get(key));
        }
    }

    private WebViewClient webViewClient() {
        return new WebViewClient() {
            @Override
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String host = request.getUrl().getHost();
                Map<String, String> headers = request.getRequestHeaders();
                if (TextUtils.isEmpty(host) || VodConfig.get().getAds().contains(host)) return empty;
                if (url.contains("challenges.cloudflare.com/cdn-cgi")) App.post(() -> showDialog());
                if (detect && url.contains("player/?url=")) onParseAdd(headers, url);
                else if (isVideoFormat(url)) interrupt(headers, url);
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                String host = UrlUtil.host(url);
                if (TextUtils.isEmpty(host) || VodConfig.get().getAds().contains(host)) return empty;
                if (host.equals("challenges.cloudflare.com")) App.post(() -> showDialog());
                if (detect && url.contains("player/?url=")) onParseAdd(headers, url);
                if (isVideoFormat(url)) interrupt(headers, url);
                return super.shouldInterceptRequest(view, url);
            }

            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (dialog != null) hideDialog();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.equals(BLANK)) return;
                evaluate(getScript(url));
            }

            @Override
            @SuppressLint("WebViewClientOnReceivedSslError")
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        };
    }

    private WebChromeClient webChromeClient() {
        return new WebChromeClient() {
            @Override
            public Bitmap getDefaultVideoPoster() {
                try {
                    return BitmapFactory.decodeResource(App.get().getResources(), R.drawable.ic_logo);
                } catch (Throwable e) {
                    return super.getDefaultVideoPoster();
                }
            }
        };
    }

    private void showDialog() {
        if (dialog != null || App.activity() == null) return;
        if (getParent() != null) ((ViewGroup) getParent()).removeView(this);
        dialog = new AlertDialog.Builder(App.activity()).setView(this).show();
    }

    private void hideDialog() {
        if (dialog != null) dialog.dismiss();
        dialog = null;
    }

    private List<String> getScript(String url) {
        List<String> script = new ArrayList<>(Sniffer.getScript(Uri.parse(url)));
        if (TextUtils.isEmpty(click) || script.contains(click)) return script;
        script.add(0, click);
        return script;
    }

    private void evaluate(List<String> script) {
        if (script.isEmpty()) return;
        if (TextUtils.isEmpty(script.get(0))) {
            evaluate(script.subList(1, script.size()));
        } else {
            loadUrl("javascript:" + script.get(0));
        }
    }

    private boolean isVideoFormat(String url) {
        try {
            Logger.t(TAG).d(url);
            Site site = VodConfig.get().getSite(key);
            Spider spider = VodConfig.get().getSpider(site);
            if (spider.manualVideoCheck()) return spider.isVideoFormat(url);
            return Sniffer.isVideoFormat(url);
        } catch (Exception ignored) {
            return Sniffer.isVideoFormat(url);
        }
    }

    private void interrupt(Map<String, String> headers, String url) {
        String cookie = CookieManager.getInstance().getCookie(url);
        if (cookie != null) headers.put(HttpHeaders.COOKIE, cookie);
        onParseSuccess(headers, url);
    }

    private void onParseAdd(Map<String, String> headers, String url) {
        App.post(() -> CustomWebView.create(App.get()).start(key, from, headers, url, click, callback, false));
    }

    private void onParseSuccess(Map<String, String> headers, String url) {
        if (callback != null) callback.onParseSuccess(headers, url, from);
        App.post(() -> stop(false));
        callback = null;
    }

    private void onParseError() {
        if (callback != null) callback.onParseError();
        callback = null;
    }

    public void stop(boolean error) {
        hideDialog();
        stopLoading();
        loadUrl(BLANK);
        App.removeCallbacks(timer);
        if (error) onParseError();
        else callback = null;
    }
}
