package com.stirlingpdf.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;
    private View errorView;
    private View splashView;
    private View networkIndicator;
    private ProgressBar refreshSpinner;

    private static final String DEFAULT_URL = "https://pdf.haison.work";
    private static final int FILE_CHOOSER_REQUEST = 1;

    private ValueCallback<Uri[]> filePathCallback;
    private Vibrator vibrator;
    private boolean isPageLoaded = false;
    private long lastBackPress = 0;
    private static final int BACK_PRESS_THRESHOLD = 2000;

    private boolean isPulling = false;
    private float pullStartY = 0;
    private float pullDistance = 0;
    private static final float PULL_THRESHOLD = 120f;
    private static final float PULL_RESISTANCE = 0.5f;

    // ─── Mobile CSS injected into every page load ────────────

    private static final String MOBILE_CSS =
        "(function(){" +
        "  if (document.getElementById('spdf-mobile-css')) return;" +
        "  var s = document.createElement('style');" +
        "  s.id = 'spdf-mobile-css';" +
        "  s.textContent = `" +
        // ── Viewport & Safe Area ──
        "    :root {" +
        "      --safe-area-top: env(safe-area-inset-top, 0px);" +
        "      --safe-area-bottom: env(safe-area-inset-bottom, 0px);" +
        "      --safe-area-left: env(safe-area-inset-left, 0px);" +
        "      --safe-area-right: env(safe-area-inset-right, 0px);" +
        "    }" +
        // ── Base mobile resets ──
        "    * { -webkit-tap-highlight-color: transparent !important; }" +
        "    img, a, button { -webkit-touch-callout: none; }" +
        "    a, button, [role=button] { touch-action: manipulation; }" +
        "    html { -webkit-text-size-adjust: 100%; }" +
        "    html, body { overscroll-behavior-y: contain; }" +
        "    input, textarea, select { font-size: 16px !important; }" +
        "    body { width: 100%; overflow-x: hidden; }" +
        // ── 100dvh fix ──
        "    .h-screen, .min-h-screen { height: 100dvh !important; min-height: 100dvh !important; }" +
        // ── Tool grid: 2 cols on mobile ──
        "    @media (max-width: 768px) {" +
        "      .tool-card, .tool-item, [class*=tool-grid] > div {" +
        "        min-width: calc(50% - 8px) !important;" +
        "        max-width: calc(50% - 8px) !important;" +
        "      }" +
        "      .tool-grid, .tools-grid {" +
        "        grid-template-columns: repeat(2, 1fr) !important;" +
        "        gap: 8px !important;" +
        "      }" +
        "    }" +
        // ── 3 cols on very small phones ──
        "    @media (max-width: 380px) {" +
        "      .tool-grid, .tools-grid {" +
        "        grid-template-columns: repeat(2, 1fr) !important;" +
        "        gap: 6px !important;" +
        "      }" +
        "    }" +
        // ── Card padding reduction ──
        "    @media (max-width: 768px) {" +
        "      .mantine-Card-root { padding: 12px !important; }" +
        "      .mantine-Paper-root { padding: 12px !important; }" +
        "      .portal-home { padding: 12px !important; gap: 12px !important; }" +
        "      .portal-home__greeting-title { font-size: 1.25rem !important; }" +
        "    }" +
        // ── Sidebar → full-width drawer on mobile ──
        "    @media (max-width: 768px) {" +
        "      .portal-sidebar {" +
        "        width: 85vw !important;" +
        "        max-width: 320px !important;" +
        "      }" +
        "      .portal-shell { height: 100dvh !important; }" +
        "      .portal-shell__main { width: 100% !important; }" +
        "    }" +
        // ── Modal → full screen on mobile ──
        "    @media (max-width: 768px) {" +
        "      .mantine-Modal-root .mantine-Modal-content {" +
        "        width: 100% !important;" +
        "        max-width: 100% !important;" +
        "        margin: 0 !important;" +
        "        border-radius: 0 !important;" +
        "        min-height: 100dvh !important;" +
        "      }" +
        "      .mantine-Modal-root .mantine-Modal-body {" +
        "        max-height: calc(100dvh - 60px) !important;" +
        "        overflow-y: auto !important;" +
        "      }" +
        "    }" +
        // ── Button touch targets ≥ 44px (Apple HIG) ──
        "    @media (max-width: 768px) {" +
        "      .mantine-Button-root {" +
        "        min-height: 44px !important;" +
        "        font-size: 14px !important;" +
        "      }" +
        "      .mantine-ActionIcon-root {" +
        "        min-width: 44px !important;" +
        "        min-height: 44px !important;" +
        "      }" +
        "    }" +
        // ── Navigation: bottom bar style on mobile ──
        "    @media (max-width: 768px) {" +
        "      .portal-shell__topbar {" +
        "        height: 52px !important;" +
        "        padding: 0 12px !important;" +
        "        padding-top: var(--safe-area-top) !important;" +
        "        background: var(--mantine-color-body) !important;" +
        "        border-bottom: 1px solid var(--mantine-color-default-border) !important;" +
        "        position: sticky !important;" +
        "        top: 0 !important;" +
        "        z-index: 100 !important;" +
        "      }" +
        "      .portal-shell__view {" +
        "        padding-bottom: var(--safe-area-bottom) !important;" +
        "      }" +
        "    }" +
        // ── Tables → horizontal scroll ──
        "    @media (max-width: 768px) {" +
        "      table { display: block; overflow-x: auto; white-space: nowrap; -webkit-overflow-scrolling: touch; }" +
        "      .mantine-Table-root { display: block; overflow-x: auto; }" +
        "    }" +
        // ── Dropzone → compact ──
        "    @media (max-width: 768px) {" +
        "      .dropzone-inner, [class*=dropzone] {" +
        "        padding: 20px 12px !important;" +
        "        min-height: 120px !important;" +
        "      }" +
        "      [class*=dropzone] h1, [class*=dropzone] h2, [class*=dropzone] h3 {" +
        "        font-size: 1rem !important;" +
        "      }" +
        "      [class*=dropzone] p {" +
        "        font-size: 0.8125rem !important;" +
        "      }" +
        "    }" +
        // ── Landing page hero → compact ──
        "    @media (max-width: 768px) {" +
        "      .landing-hero, [class*=landing-hero] {" +
        "        padding: 24px 16px !important;" +
        "      }" +
        "      .landing-hero h1, [class*=landing] h1 {" +
        "        font-size: 1.5rem !important;" +
        "      }" +
        "      .landing-hero p, [class*=landing] p {" +
        "        font-size: 0.875rem !important;" +
        "      }" +
        "      .landing-stack, [class*=landing-stack] {" +
        "        display: none !important;" +
        "      }" +
        "    }" +
        // ── Tabs → scrollable ──
        "    @media (max-width: 768px) {" +
        "      .mantine-Tabs-list {" +
        "        overflow-x: auto !important;" +
        "        flex-wrap: nowrap !important;" +
        "        -webkit-overflow-scrolling: touch !important;" +
        "        scrollbar-width: none !important;" +
        "      }" +
        "      .mantine-Tabs-list::-webkit-scrollbar { display: none !important; }" +
        "    }" +
        // ── Forms: full width ──
        "    @media (max-width: 768px) {" +
        "      .mantine-TextInput-root, .mantine-Textarea-root, .mantine-Select-root, .mantine-NumberInput-root {" +
        "        width: 100% !important;" +
        "      }" +
        "      .mantine-Grid-root {" +
        "        grid-template-columns: 1fr !important;" +
        "        gap: 8px !important;" +
        "      }" +
        "      .mantine-Grid-col { width: 100% !important; grid-column: span 12 !important; }" +
        "    }" +
        // ── Settings/API keys → stacked ──
        "    @media (max-width: 768px) {" +
        "      .api-keys-card, [class*=api-keys] {" +
        "        flex-direction: column !important;" +
        "        gap: 12px !important;" +
        "      }" +
        "    }" +
        // ── PDF viewer → full width ──
        "    @media (max-width: 768px) {" +
        "      [class*=pdf-viewer], [class*=editor-canvas], [class*=document-container] {" +
        "        width: 100% !important;" +
        "        max-width: 100% !important;" +
        "      }" +
        "    }" +
        // ── Accordion → compact ──
        "    @media (max-width: 768px) {" +
        "      .mantine-Accordion-item {" +
        "        margin-bottom: 4px !important;" +
        "      }" +
        "      .mantine-Accordion-control {" +
        "        padding: 10px 12px !important;" +
        "      }" +
        "    }" +
        // ── Footer → hidden on mobile ──
        "    @media (max-width: 768px) {" +
        "      footer, .footer, [class*=footer] {" +
        "        padding: 16px 12px !important;" +
        "        font-size: 0.75rem !important;" +
        "      }" +
        "    }" +
        // ── Search bar → full width ──
        "    @media (max-width: 768px) {" +
        "      .mantine-TextInput-root input[type=text], .mantine-TextInput-root input[type=search] {" +
        "        width: 100% !important;" +
        "      }" +
        "    }" +
        // ── Header → sticky with safe area ──
        "    @media (max-width: 768px) {" +
        "      header, .header, [class*=header]:not(.portal-shell__topbar) {" +
        "        position: sticky !important;" +
        "        top: 0 !important;" +
        "        z-index: 50 !important;" +
        "      }" +
        "    }" +
        // ── Bottom bar / footer: fix khuất bởi nav bar ──
        "    .mobile-bottom-bar," +
        "    .m_3840c879," +  // Mantine AppShell footer
        "    [class*=AppShellFooter]," +
        "    footer[class*=fixed]," +
        "    [class*=bottom-bar]:not(.file-sidebar-bottom-bar) {" +
        "      padding-bottom: env(safe-area-inset-bottom, 0px) !important;" +
        "      padding-bottom: max(env(safe-area-inset-bottom, 0px), 12px) !important;" +
        "    }" +
        // ── Main content: reserve space cho fixed bottom bar ──
        "    .m_8983817," +  // Mantine AppShell main
        "    [class*=AppShellMain]," +
        "    .portal-shell__view {" +
        "      padding-bottom: calc(var(--app-shell-footer-height, 56px) + env(safe-area-inset-bottom, 0px) + 12px) !important;" +
        "    }" +
        // ── Shell container: 100dvh ──
        "    .m_89ab340," +  // Mantine AppShell root
        "    [class*=AppShellRoot]," +
        "    .portal-shell {" +
        "      height: 100dvh !important;" +
        "      min-height: 100dvh !important;" +
        "    }" +
        // ── File sidebar bottom bar ──
        "    .file-sidebar-bottom-bar {" +
        "      padding-bottom: max(env(safe-area-inset-bottom, 0px), 8px) !important;" +
        "    }" +
        // ── Remove horizontal scroll ──
        "    body { max-width: 100vw !important; }" +
        "    div, section, main, article { max-width: 100% !important; }" +
        // ── Smooth scrolling ──
        "    @media (prefers-reduced-motion: no-preference) {" +
        "      html { scroll-behavior: smooth; }" +
        "    }" +
        // ── Dark mode status bar ──
        "    @media (max-width: 768px) {" +
        "      [data-mantine-color-scheme=dark] .portal-shell__topbar {" +
        "        background: var(--mantine-color-dark-7) !important;" +
        "        border-bottom-color: var(--mantine-color-dark-4) !important;" +
        "      }" +
        "    }" +
        // ── Bulk action panel → bottom sheet style ──
        "    @media (max-width: 768px) {" +
        "      [class*=bulk-panel], [class*=bulk-action] {" +
        "        position: fixed !important;" +
        "        bottom: 0 !important;" +
        "        left: 0 !important;" +
        "        right: 0 !important;" +
        "        border-radius: 16px 16px 0 0 !important;" +
        "        padding-bottom: var(--safe-area-bottom) !important;" +
        "        box-shadow: 0 -4px 24px rgba(0,0,0,0.15) !important;" +
        "      }" +
        "    }" +
        // ── Notifications dropdown → full width ──
        "    @media (max-width: 768px) {" +
        "      .mantine-Popover-dropdown, .mantine-Menu-dropdown {" +
        "        width: calc(100vw - 24px) !important;" +
        "        max-width: 360px !important;" +
        "      }" +
        "    }" +
        "    `;" +
        "  document.head.appendChild(s);" +
        "  console.log('[Stirling-PDF] Mobile CSS injected');" +
        "})();";

    // ─── Lifecycle ───────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#0F172A"));
        window.setNavigationBarColor(Color.parseColor("#0F172A"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        buildLayout();

        String serverUrl = getSharedPreferences("stirling", MODE_PRIVATE)
                .getString("server_url", DEFAULT_URL);

        showSplash();
        webView.loadUrl(serverUrl);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isPageLoaded && splashView.getVisibility() == View.VISIBLE) {
                showErrorView();
            }
        }, 15000);
    }

    // ─── Layout Building ─────────────────────────────────────

    private void buildLayout() {
        FrameLayout root = new FrameLayout(this);

        refreshSpinner = new ProgressBar(this);
        refreshSpinner.setIndeterminate(true);
        refreshSpinner.setProgressTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#3B82F6")));
        FrameLayout.LayoutParams spinnerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        spinnerParams.topMargin = 56;
        refreshSpinner.setLayoutParams(spinnerParams);
        refreshSpinner.setVisibility(View.GONE);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        FrameLayout.LayoutParams pbParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 6, Gravity.TOP);
        progressBar.setLayoutParams(pbParams);
        progressBar.setVisibility(View.GONE);
        progressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#3B82F6")));

        networkIndicator = buildNetworkIndicator();
        errorView = buildErrorView();
        splashView = buildSplashView();

        root.addView(webView);
        root.addView(refreshSpinner);
        root.addView(progressBar);
        root.addView(networkIndicator);
        root.addView(errorView);
        root.addView(splashView);

        setContentView(root);
        configureWebView();
    }

    private View buildNetworkIndicator() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        params.topMargin = 48;
        container.setLayoutParams(params);
        container.setVisibility(View.GONE);

        TextView text = new TextView(this);
        text.setText("⚠ No internet connection");
        text.setTextColor(Color.WHITE);
        text.setGravity(Gravity.CENTER);
        text.setPadding(32, 16, 32, 16);
        text.setBackgroundColor(Color.parseColor("#DC2626"));
        text.setTextSize(13);
        container.addView(text);
        return container;
    }

    private View buildErrorView() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        container.setLayoutParams(params);
        container.setBackgroundColor(Color.parseColor("#0F172A"));
        container.setPadding(64, 0, 64, 0);
        container.setVisibility(View.GONE);

        TextView icon = new TextView(this);
        icon.setText("📄");
        icon.setTextSize(64);
        icon.setGravity(Gravity.CENTER);
        container.addView(icon);

        View spacer = new View(this);
        container.addView(spacer, new LinearLayout.LayoutParams(1, 32));

        TextView title = new TextView(this);
        title.setText("Cannot connect to server");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        container.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Make sure Stirling-PDF server is running and your network is available.");
        subtitle.setTextColor(Color.parseColor("#94A3B8"));
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(16, 16, 16, 32);
        container.addView(subtitle);

        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setTextColor(Color.WHITE);
        retry.setBackgroundColor(Color.parseColor("#3B82F6"));
        retry.setPadding(64, 24, 64, 24);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);
        retry.setLayoutParams(btnParams);
        retry.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            showSplash();
            isPageLoaded = false;
            webView.reload();
            haptic(10);
        });
        container.addView(retry);

        View spacer2 = new View(this);
        container.addView(spacer2, new LinearLayout.LayoutParams(1, 16));

        Button settings = new Button(this);
        settings.setText("Change Server URL");
        settings.setTextColor(Color.parseColor("#94A3B8"));
        settings.setBackgroundColor(Color.TRANSPARENT);
        settings.setPadding(32, 16, 32, 16);
        settings.setOnClickListener(v -> showSettingsDialog());
        container.addView(settings);

        return container;
    }

    private View buildSplashView() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        container.setLayoutParams(params);
        container.setBackgroundColor(Color.parseColor("#0F172A"));

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(192, 192);
        icon.setLayoutParams(iconParams);
        container.addView(icon);

        TextView appName = new TextView(this);
        appName.setText("Stirling PDF");
        appName.setTextColor(Color.WHITE);
        appName.setTextSize(24);
        appName.setGravity(Gravity.CENTER);
        appName.setPadding(0, 24, 0, 8);
        container.addView(appName);

        TextView tagline = new TextView(this);
        tagline.setText("The open-source PDF platform");
        tagline.setTextColor(Color.parseColor("#64748B"));
        tagline.setTextSize(14);
        tagline.setGravity(Gravity.CENTER);
        container.addView(tagline);

        ProgressBar loading = new ProgressBar(this);
        loading.setIndeterminate(true);
        loading.setProgressTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#3B82F6")));
        LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingParams.topMargin = 48;
        loading.setLayoutParams(loadingParams);
        container.addView(loading);

        return container;
    }

    // ─── WebView Configuration ──────────────────────────────

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.setWebChromeClient(new StirlingChromeClient());
        webView.setWebViewClient(new StirlingWebViewClient());

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            haptic(10);
        });

        webView.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
            }
            return false;
        });

        webView.setOnTouchListener((v, event) -> {
            handlePullToRefresh(event);
            return false;
        });
    }

    // ─── Pull-to-Refresh ────────────────────────────────────

    private void handlePullToRefresh(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (webView.getScrollY() == 0) {
                    isPulling = true;
                    pullStartY = event.getRawY();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isPulling) {
                    pullDistance = (event.getRawY() - pullStartY) * PULL_RESISTANCE;
                    if (pullDistance > 0) {
                        refreshSpinner.setVisibility(View.VISIBLE);
                        refreshSpinner.setTranslationY(pullDistance);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isPulling) {
                    isPulling = false;
                    if (pullDistance >= PULL_THRESHOLD) {
                        haptic(15);
                        refreshSpinner.setVisibility(View.VISIBLE);
                        refreshSpinner.setTranslationY(0);
                        webView.reload();
                    } else {
                        refreshSpinner.setVisibility(View.GONE);
                    }
                    pullDistance = 0;
                }
                break;
        }
    }

    // ─── WebView Clients ─────────────────────────────────────

    private class StirlingWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost();
            if (host != null && (host.equals("localhost") || host.equals("127.0.0.1")
                    || host.contains("stirling") || host.contains("haison.work"))) {
                return false;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            isPageLoaded = true;
            hideSplash();
            refreshSpinner.setVisibility(View.GONE);

            // Inject mobile CSS + viewport fix
            view.evaluateJavascript(MOBILE_CSS, null);

            // Force viewport with safe-area
            view.evaluateJavascript(
                "(function(){" +
                "  var meta = document.querySelector('meta[name=viewport]');" +
                "  if(meta) { meta.setAttribute('content', 'width=device-width, initial-scale=1.0, maximum-scale=5.0, viewport-fit=cover'); }" +
                "})();",
                null);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                showErrorView();
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (request.isForMainFrame() && errorResponse.getStatusCode() >= 500) {
                showErrorView();
            }
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            // Re-inject CSS after dynamic content loads (SPA navigation)
            view.evaluateJavascript(
                "(function(){" +
                "  if (!document.getElementById('spdf-mobile-css')) {" +
                "    " + MOBILE_CSS +
                "  }" +
                "})();",
                null);
        }
    }

    private class StirlingChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress < 100) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback,
                                         FileChooserParams fileChooserParams) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(null);
            }
            filePathCallback = callback;

            Intent intent = fileChooserParams.createIntent();
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            if (acceptTypes != null && acceptTypes.length > 0 && !acceptTypes[0].equals("*/*")) {
                intent.putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes);
            }

            try {
                startActivityForResult(Intent.createChooser(intent, "Select file"), FILE_CHOOSER_REQUEST);
            } catch (android.content.ActivityNotFoundException e) {
                filePathCallback = null;
                Toast.makeText(MainActivity.this, "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                        GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            runOnUiThread(() -> request.grant(request.getResources()));
        }
    }

    // ─── File chooser result ─────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;

            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    // ─── Splash / Error show/hide ────────────────────────────

    private void showSplash() {
        splashView.setVisibility(View.VISIBLE);
        splashView.setAlpha(1f);
    }

    private void hideSplash() {
        if (splashView.getVisibility() == View.VISIBLE) {
            splashView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        splashView.setVisibility(View.GONE);
                        splashView.setAlpha(1f);
                    })
                    .start();
        }
    }

    private void showErrorView() {
        runOnUiThread(() -> {
            splashView.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
        });
    }

    private void showNetworkIndicator(boolean show) {
        runOnUiThread(() -> networkIndicator.setVisibility(show ? View.VISIBLE : View.GONE));
    }

    // ─── Haptic Feedback ─────────────────────────────────────

    private void haptic(int milliseconds) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(milliseconds);
        }
    }

    // ─── Network check ──────────────────────────────────────

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    // ─── Settings Dialog ────────────────────────────────────

    private void showSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 16);

        TextView label = new TextView(this);
        label.setText("Stirling-PDF Server URL");
        label.setTextColor(Color.parseColor("#64748B"));
        label.setTextSize(13);
        label.setPadding(0, 0, 0, 8);
        layout.addView(label);

        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(getSharedPreferences("stirling", MODE_PRIVATE)
                .getString("server_url", DEFAULT_URL));
        input.setSelection(input.getText().length());
        input.setTextSize(15);
        layout.addView(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(layout)
                .setPositiveButton("Save", (d, which) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) {
                        if (!url.startsWith("http")) {
                            url = "https://" + url;
                        }
                        getSharedPreferences("stirling", MODE_PRIVATE)
                                .edit().putString("server_url", url).apply();
                        Toast.makeText(this, "Server URL updated. Reloading…", Toast.LENGTH_SHORT).show();
                        isPageLoaded = false;
                        showSplash();
                        webView.loadUrl(url);
                        haptic(20);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset", (d, which) -> {
                    getSharedPreferences("stirling", MODE_PRIVATE)
                            .edit().remove("server_url").apply();
                    Toast.makeText(this, "Reset to default. Reloading…", Toast.LENGTH_SHORT).show();
                    isPageLoaded = false;
                    showSplash();
                    webView.loadUrl(DEFAULT_URL);
                })
                .create();

        dialog.show();
    }

    // ─── Back press: confirm exit ────────────────────────────

    @Override
    public void onBackPressed() {
        if (errorView.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
            return;
        }

        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            haptic(5);
        } else {
            if (lastBackPress + BACK_PRESS_THRESHOLD > System.currentTimeMillis()) {
                super.onBackPressed();
            } else {
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                lastBackPress = System.currentTimeMillis();
            }
        }
    }

    // ─── Lifecycle management ───────────────────────────────

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        showNetworkIndicator(!isNetworkAvailable());
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
