package com.example.data.model

enum class WidgetCategory(
    val displayName: String,
    val description: String
) {
    PRODUCTIVITY("Productivity", "Clocks, calendars, tasks, timers & habits"),
    SYSTEM_INFO("System Info", "Battery, storage, RAM, device & network stats"),
    WEATHER_TIME("Weather & Time", "Live weather, forecasts, world clocks & sun"),
    PERSONALIZATION("Personalization", "Custom text, quotes, photos & themed clocks"),
    UTILITIES("Utilities", "Flashlight, shortcuts, QR codes, RNG & reminders")
}

enum class WidgetSizePreset(
    val label: String,
    val cols: Int,
    val rows: Int,
    val aspectRatio: Float
) {
    SMALL_2X2("2×2", 2, 2, 1f),
    MEDIUM_4X2("4×2", 4, 2, 2f),
    LARGE_4X4("4×4", 4, 4, 1f),
    STRIP_4X1("4×1", 4, 1, 3.6f)
}

enum class WidgetFontFamily(
    val displayName: String,
    val key: String
) {
    PLUS_JAKARTA("Plus Jakarta Sans", "plus_jakarta"),
    INTER("Inter", "inter"),
    SPACE_GROTESK("Space Grotesk", "space_grotesk"),
    JETBRAINS_MONO("JetBrains Mono", "jetbrains_mono"),
    PLAYFAIR_DISPLAY("Playfair Display", "playfair_display"),
    OUTFIT("Outfit", "outfit"),
    SYSTEM_SANS("System Sans", "system_sans")
}

enum class WidgetFontWeight(val displayName: String, val weightValue: Int) {
    REGULAR("Regular", 400),
    MEDIUM("Medium", 500),
    SEMIBOLD("SemiBold", 600),
    BOLD("Bold", 700)
}

enum class WidgetAlignment(val displayName: String) {
    START("Left"),
    CENTER("Center"),
    END("Right")
}

enum class WidgetThemeOverride(val displayName: String) {
    CUSTOM("Custom"),
    LIGHT("Light"),
    DARK("Dark"),
    FOLLOW_SYSTEM("System")
}

enum class WidgetType(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: WidgetCategory,
    val defaultSize: WidgetSizePreset,
    val isFeatured: Boolean = false,
    val isPopular: Boolean = false,
    val supportsBgImage: Boolean = true,
    val hasInteractiveActions: Boolean = false,
    val platformNote: String? = null
) {
    // --- PRODUCTIVITY (11) ---
    DIGITAL_CLOCK(
        id = "digital_clock",
        title = "Digital Clock",
        subtitle = "Clean tabular time & date display",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        isFeatured = true,
        isPopular = true
    ),
    ANALOG_CLOCK(
        id = "analog_clock",
        title = "Analog Clock",
        subtitle = "Precision vector watch dial",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.SMALL_2X2,
        isFeatured = true
    ),
    CALENDAR(
        id = "calendar",
        title = "Calendar",
        subtitle = "Monthly grid with today highlight",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.LARGE_4X4,
        isPopular = true
    ),
    DAILY_AGENDA(
        id = "daily_agenda",
        title = "Daily Agenda",
        subtitle = "Today's scheduled blocks & focus",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.MEDIUM_4X2
    ),
    TODO_LIST(
        id = "todo_list",
        title = "To-do List",
        subtitle = "Interactive task checklist on home screen",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.LARGE_4X4,
        isFeatured = true,
        isPopular = true,
        hasInteractiveActions = true
    ),
    NOTES(
        id = "notes",
        title = "Notes",
        subtitle = "Pinned quick sticky note card",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        isPopular = true
    ),
    COUNTDOWN(
        id = "countdown",
        title = "Countdown",
        subtitle = "Days & hours remaining to your event",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.SMALL_2X2
    ),
    STOPWATCH(
        id = "stopwatch",
        title = "Stopwatch",
        subtitle = "Start, pause & reset timer from widget",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        hasInteractiveActions = true
    ),
    POMODORO_TIMER(
        id = "pomodoro_timer",
        title = "Pomodoro Timer",
        subtitle = "25/5 focus sprint timer with controls",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        isFeatured = true,
        hasInteractiveActions = true
    ),
    DAILY_GOALS(
        id = "daily_goals",
        title = "Daily Goals",
        subtitle = "Visual progress bar with +/- controls",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        hasInteractiveActions = true
    ),
    HABIT_TRACKER(
        id = "habit_tracker",
        title = "Habit Tracker",
        subtitle = "Daily habit check-ins & streak count",
        category = WidgetCategory.PRODUCTIVITY,
        defaultSize = WidgetSizePreset.LARGE_4X4,
        isPopular = true,
        hasInteractiveActions = true
    ),

    // --- SYSTEM INFORMATION (7) ---
    BATTERY(
        id = "battery",
        title = "Battery Percentage",
        subtitle = "Charge level, charging state & temp",
        category = WidgetCategory.SYSTEM_INFO,
        defaultSize = WidgetSizePreset.SMALL_2X2,
        isPopular = true
    ),
    STORAGE(
        id = "storage",
        title = "Storage Information",
        subtitle = "Internal storage used & available GB",
        category = WidgetCategory.SYSTEM_INFO,
        defaultSize = WidgetSizePreset.MEDIUM_4X2
    ),
    RAM(
        id = "ram",
        title = "RAM Information",
        subtitle = "Live device memory usage & free MB",
        category = WidgetCategory.SYSTEM_INFO,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        platformNote = "Uses Android ActivityManager.MemoryInfo (system-wide available/total RAM without restricted permissions)."
    ),
    DEVICE_INFO(
        id = "device_info",
        title = "Device Information",
        subtitle = "Model, Android version, API & uptime",
        category = WidgetCategory.SYSTEM_INFO,
        defaultSize = WidgetSizePreset.MEDIUM_4X2
    ),
    INTERNET_STATUS(
        id = "internet_status",
        title = "Internet Connection",
        subtitle = "Online status, transport & metered state",
        category = WidgetCategory.SYSTEM_INFO,
        defaultSize = WidgetSizePreset.STRIP_4X1
    ),
    WIFI_INFO(
        id = "wifi_info",
        title = "Wi-Fi Information",
        subtitle = "Signal strength, band & link speed",
        category = WidgetCategory.SYSTEM_INFO,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        platformNote = "Android 10+ hides Wi-Fi SSID unless location permission is granted; WidgetLite avoids unnecessary location prompts and shows signal/link speed privately."
    ),
    NETWORK_SPEED(
        id = "network_speed",
        title = "Network Speed",
        subtitle = "Link bandwidth & data counter",
        category = WidgetCategory.SYSTEM_INFO,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        platformNote = "Displays reported downstream/upstream link capabilities and TrafficStats session totals."
    ),

    // --- WEATHER & TIME (6) ---
    WEATHER(
        id = "weather",
        title = "Weather Widget",
        subtitle = "Live conditions, feels-like & wind",
        category = WidgetCategory.WEATHER_TIME,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        isFeatured = true,
        isPopular = true,
        hasInteractiveActions = true,
        platformNote = "Powered by Open-Meteo (free, no API key, no tracking). Network used only when fetching weather."
    ),
    TEMPERATURE(
        id = "temperature",
        title = "Temperature",
        subtitle = "Minimalist oversized current temp",
        category = WidgetCategory.WEATHER_TIME,
        defaultSize = WidgetSizePreset.SMALL_2X2,
        hasInteractiveActions = true
    ),
    WEATHER_FORECAST(
        id = "weather_forecast",
        title = "Weather Forecast",
        subtitle = "Multi-day high/low outlook card",
        category = WidgetCategory.WEATHER_TIME,
        defaultSize = WidgetSizePreset.LARGE_4X4,
        hasInteractiveActions = true
    ),
    MULTI_TIMEZONE(
        id = "multi_timezone",
        title = "Multiple Time Zones",
        subtitle = "Compare 3 global time zones at once",
        category = WidgetCategory.WEATHER_TIME,
        defaultSize = WidgetSizePreset.MEDIUM_4X2
    ),
    WORLD_CLOCK(
        id = "world_clock",
        title = "World Clock",
        subtitle = "Local + city clocks with day offset",
        category = WidgetCategory.WEATHER_TIME,
        defaultSize = WidgetSizePreset.LARGE_4X4
    ),
    SUNRISE_SUNSET(
        id = "sunrise_sunset",
        title = "Sunrise & Sunset",
        subtitle = "Solar arc, dawn, dusk & daylight span",
        category = WidgetCategory.WEATHER_TIME,
        defaultSize = WidgetSizePreset.MEDIUM_4X2
    ),

    // --- PERSONALIZATION (7) ---
    CUSTOM_TEXT(
        id = "custom_text",
        title = "Custom Text",
        subtitle = "Your personal mantra or label in any font",
        category = WidgetCategory.PERSONALIZATION,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        isPopular = true
    ),
    QUOTES(
        id = "quotes",
        title = "Inspirational Quotes",
        subtitle = "Curated minimal quotes, tap to cycle",
        category = WidgetCategory.PERSONALIZATION,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        hasInteractiveActions = true
    ),
    PHOTO(
        id = "photo",
        title = "Photo Widget",
        subtitle = "Zero-permission gallery or preset photo",
        category = WidgetCategory.PERSONALIZATION,
        defaultSize = WidgetSizePreset.SMALL_2X2,
        isFeatured = true,
        isPopular = true
    ),
    MINIMALIST_CLOCK(
        id = "minimalist_clock",
        title = "Minimalist Clock",
        subtitle = "Ultra-clean typographic time card",
        category = WidgetCategory.PERSONALIZATION,
        defaultSize = WidgetSizePreset.SMALL_2X2
    ),
    CUSTOM_COLOR(
        id = "custom_color",
        title = "Custom Color Widget",
        subtitle = "Pure color block or mood swatch",
        category = WidgetCategory.PERSONALIZATION,
        defaultSize = WidgetSizePreset.SMALL_2X2
    ),
    THEMED_CALENDAR(
        id = "themed_calendar",
        title = "Themed Calendar",
        subtitle = "Styled date & month showcase",
        category = WidgetCategory.PERSONALIZATION,
        defaultSize = WidgetSizePreset.MEDIUM_4X2
    ),
    CUSTOM_ICON_SHORTCUT(
        id = "custom_icon_shortcut",
        title = "Custom Icon Shortcut",
        subtitle = "Minimalist icon tile to launch an app",
        category = WidgetCategory.PERSONALIZATION,
        defaultSize = WidgetSizePreset.SMALL_2X2
    ),

    // --- UTILITIES (8) ---
    QUICK_SETTINGS(
        id = "quick_settings",
        title = "Quick Settings",
        subtitle = "Direct jumps to Wi-Fi, BT & Display",
        category = WidgetCategory.UTILITIES,
        defaultSize = WidgetSizePreset.STRIP_4X1,
        hasInteractiveActions = true,
        platformNote = "Android prohibits third-party apps from toggling system radios silently; tapping opens the exact system settings panel directly."
    ),
    FLASHLIGHT(
        id = "flashlight",
        title = "Flashlight Shortcut",
        subtitle = "Instant camera torch toggle",
        category = WidgetCategory.UTILITIES,
        defaultSize = WidgetSizePreset.SMALL_2X2,
        isPopular = true,
        hasInteractiveActions = true
    ),
    CALCULATOR(
        id = "calculator",
        title = "Calculator Shortcut",
        subtitle = "One-tap system calculator launcher",
        category = WidgetCategory.UTILITIES,
        defaultSize = WidgetSizePreset.SMALL_2X2
    ),
    WEBSITE(
        id = "website",
        title = "Website Shortcut",
        subtitle = "Pin any URL directly to your home screen",
        category = WidgetCategory.UTILITIES,
        defaultSize = WidgetSizePreset.SMALL_2X2
    ),
    APP_SHORTCUT(
        id = "app_shortcut",
        title = "App Shortcut",
        subtitle = "Clean typographic app launcher tile",
        category = WidgetCategory.UTILITIES,
        defaultSize = WidgetSizePreset.SMALL_2X2
    ),
    QR_CODE(
        id = "qr_code",
        title = "QR Code Widget",
        subtitle = "Scannable QR code for Wi-Fi, link or text",
        category = WidgetCategory.UTILITIES,
        defaultSize = WidgetSizePreset.SMALL_2X2,
        isFeatured = true
    ),
    RANDOM_NUMBER(
        id = "random_number",
        title = "Random Number",
        subtitle = "Tap widget to roll within custom range",
        category = WidgetCategory.UTILITIES,
        defaultSize = WidgetSizePreset.SMALL_2X2,
        hasInteractiveActions = true
    ),
    DAILY_REMINDER(
        id = "daily_reminder",
        title = "Daily Reminder",
        subtitle = "Persistent daily check-off card",
        category = WidgetCategory.UTILITIES,
        defaultSize = WidgetSizePreset.MEDIUM_4X2,
        hasInteractiveActions = true
    );

    companion object {
        fun fromId(id: String): WidgetType = entries.firstOrNull { it.id == id } ?: DIGITAL_CLOCK
    }
}
