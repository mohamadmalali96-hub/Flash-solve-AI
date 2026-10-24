package com.example.model

import androidx.compose.ui.unit.LayoutDirection

enum class AppLanguage(val code: String, val displayName: String, val layoutDirection: LayoutDirection) {
    EN("en", "English", LayoutDirection.Ltr),
    AR("ar", "العربية", LayoutDirection.Rtl)
}

enum class PillMode {
    ACTIVE,
    STANDBY
}

enum class SolveStatus {
    IDLE,
    CAPTURING,
    SOLVING,
    RESULT,
    ERROR
}

data class SolveHistoryItem(
    val id: String,
    val timestampFormatted: String,
    val answerText: String,
    val latencyMs: Long,
    val sourceLabel: String,
    val isSuccess: Boolean = true
)

data class PracticeQuestion(
    val id: Int,
    val categoryEn: String,
    val categoryAr: String,
    val questionEn: String,
    val questionAr: String,
    val optionsEn: List<String>,
    val optionsAr: List<String>,
    val expectedBracket: String
)

data class LocalizedStrings(
    val appTitle: String,
    val appSubtitle: String,
    val masterSwitchTitle: String,
    val masterSwitchActiveSub: String,
    val masterSwitchStoppedSub: String,
    val startServiceBtn: String,
    val stopServiceBtn: String,
    val pillStateLabel: String,
    val pillActiveMode: String,
    val pillStandbyMode: String,
    val pillSolveBtn: String,
    val pillSolvingText: String,
    val pillReadyText: String,
    val pillStandbyHint: String,
    val apiKeyCardTitle: String,
    val apiKeyCardSub: String,
    val apiKeyPlaceholder: String,
    val apiKeySaveBtn: String,
    val apiKeySavedBadge: String,
    val apiKeyUsingBuildConfig: String,
    val apiKeyMissingWarning: String,
    val securityNotice: String,
    val permissionsCardTitle: String,
    val overlayPermTitle: String,
    val overlayPermGranted: String,
    val overlayPermGrantBtn: String,
    val capturePermTitle: String,
    val capturePermGranted: String,
    val capturePermFallback: String,
    val capturePermGrantBtn: String,
    val practiceArenaTitle: String,
    val practiceArenaSub: String,
    val nextQuestionBtn: String,
    val triggerSolveNowBtn: String,
    val customPromptLabel: String,
    val telemetryTitle: String,
    val avgLatencyLabel: String,
    val lastAnswerLabel: String,
    val targetSlaLabel: String,
    val historyTitle: String,
    val historyEmpty: String,
    val clearHistoryBtn: String,
    val languageToggleLabel: String
)

object FlashSolveLocalization {
    val english = LocalizedStrings(
        appTitle = "FlashSolve AI",
        appSubtitle = "Floating Pill Screen Solver • Gemini Flash REST",
        masterSwitchTitle = "Master Floating Service",
        masterSwitchActiveSub = "WindowManager Pill Active • Zero UI Freeze",
        masterSwitchStoppedSub = "Service Stopped • Toggle ON to launch Pill",
        startServiceBtn = "START PILL",
        stopServiceBtn = "STOP PILL",
        pillStateLabel = "Floating Pill State",
        pillActiveMode = "ACTIVE",
        pillStandbyMode = "STANDBY",
        pillSolveBtn = "Solve",
        pillSolvingText = "Solving…",
        pillReadyText = "Ready",
        pillStandbyHint = "Standby",
        apiKeyCardTitle = "Gemini Flash API Key",
        apiKeyCardSub = "Secure key input for OkHttp REST requests (gemini-flash-latest)",
        apiKeyPlaceholder = "Paste Gemini API Key (AIza...)",
        apiKeySaveBtn = "Save Key",
        apiKeySavedBadge = "Custom Key Active",
        apiKeyUsingBuildConfig = "Using AI Studio Secret (BuildConfig)",
        apiKeyMissingWarning = "Enter your Gemini API Key or set GEMINI_API_KEY in AI Studio Secrets",
        securityNotice = "Security Warning: Android APKs can be easily decompiled. Do not share this APK file publicly if it contains valid API keys.",
        permissionsCardTitle = "Overlay & Capture Engine",
        overlayPermTitle = "WindowManager Overlay",
        overlayPermGranted = "System Overlay Active (Draws over apps)",
        overlayPermGrantBtn = "Grant Overlay",
        capturePermTitle = "Screen Capture Pipeline",
        capturePermGranted = "Full System MediaProjection Active",
        capturePermFallback = "Live Window Capture Active (Grant System Capture for external apps)",
        capturePermGrantBtn = "Enable Full Screen",
        practiceArenaTitle = "Live Screen Test Arena",
        practiceArenaSub = "Tap 'Solve' on the Floating Pill to capture this screen and test sub-1.5s response",
        nextQuestionBtn = "Next Question",
        triggerSolveNowBtn = "⚡ Capture & Solve Screen Now",
        customPromptLabel = "Expected Output Format: Minimal Bracketed e.g. [B]",
        telemetryTitle = "Real-Time Speed Telemetry",
        avgLatencyLabel = "Last Latency",
        lastAnswerLabel = "Immediate Output",
        targetSlaLabel = "Target SLA < 1.5s",
        historyTitle = "Recent Screen Solves",
        historyEmpty = "No screen captures solved yet. Tap 'Solve' on the floating pill!",
        clearHistoryBtn = "Clear",
        languageToggleLabel = "العربية"
    )

    val arabic = LocalizedStrings(
        appTitle = "FlashSolve AI",
        appSubtitle = "حلال الشاشة الفوري • Gemini Flash REST",
        masterSwitchTitle = "المفتاح الرئيسي للكبسولة العائمة",
        masterSwitchActiveSub = "كبسولة WindowManager نشطة • بدون أي تجميد للواجهة",
        masterSwitchStoppedSub = "الخدمة متوقفة • قم بالتفعيل لإظهار الكبسولة",
        startServiceBtn = "تشغيل الكبسولة",
        stopServiceBtn = "إيقاف الكبسولة",
        pillStateLabel = "حالة الكبسولة العائمة",
        pillActiveMode = "نشط",
        pillStandbyMode = "انتظار",
        pillSolveBtn = "حل",
        pillSolvingText = "جاري الحل…",
        pillReadyText = "جاهز",
        pillStandbyHint = "انتظار",
        apiKeyCardTitle = "مفتاح Gemini Flash API",
        apiKeyCardSub = "حقل آمن لمفتاح API لطلبات OkHttp الفورية (gemini-flash-latest)",
        apiKeyPlaceholder = "أدخل مفتاح Gemini API (AIza...)",
        apiKeySaveBtn = "حفظ المفتاح",
        apiKeySavedBadge = "مفتاح مخصص نشط",
        apiKeyUsingBuildConfig = "يستخدم مفتاح AI Studio المدمج (BuildConfig)",
        apiKeyMissingWarning = "يرجى إدخال مفتاح Gemini API أو إضافته في لوحة أسرار AI Studio",
        securityNotice = "تحذير أمني: يمكن تفكيك ملفات APK بسهولة. لا تشارك ملف APK هذا علنياً لحماية مفتاح API الخاص بك.",
        permissionsCardTitle = "محرك الطبقات العائمة والتقاط الشاشة",
        overlayPermTitle = "طبقة WindowManager العائمة",
        overlayPermGranted = "الظهور فوق التطبيقات مفعّل",
        overlayPermGrantBtn = "منح الإذن",
        capturePermTitle = "محرك التقاط الشاشة",
        capturePermGranted = "التقاط النظام الكامل MediaProjection نشط",
        capturePermFallback = "التقاط النافذة المباشر نشط (فعّل التقاط النظام للتطبيقات الخارجية)",
        capturePermGrantBtn = "تفعيل للشاشة كاملة",
        practiceArenaTitle = "ساحة اختبار الشاشة المباشرة",
        practiceArenaSub = "اضغط على زر «حل» في الكبسولة العائمة لالتقاط الشاشة واختبار الاستجابة الأقل من 1.5 ثانية",
        nextQuestionBtn = "سؤال آخر",
        triggerSolveNowBtn = "⚡ التقط الشاشة وحل الآن",
        customPromptLabel = "صيغة الإخراج الفوري: رمز مختصر بين قوسين مثل [B]",
        telemetryTitle = "قياس سرعة الاستجابة الفورية",
        avgLatencyLabel = "زمن الاستجابة",
        lastAnswerLabel = "الإجابة الفورية",
        targetSlaLabel = "الهدف < 1.5 ثانية",
        historyTitle = "سجل الحلول الأخيرة",
        historyEmpty = "لا توجد عمليات حل حتى الآن. اضغط على «حل» في الكبسولة العائمة!",
        clearHistoryBtn = "مسح",
        languageToggleLabel = "English"
    )

    fun forLanguage(language: AppLanguage): LocalizedStrings =
        when (language) {
            AppLanguage.EN -> english
            AppLanguage.AR -> arabic
        }

    val sampleQuestions = listOf(
        PracticeQuestion(
            id = 1,
            categoryEn = "Computer Science • Algorithms",
            categoryAr = "علوم الحاسب • الخوارزميات",
            questionEn = "What is the worst-case time complexity of searching for an element in a balanced Binary Search Tree (AVL Tree) with n nodes?",
            questionAr = "ما هو التعقيد الزمني في أسوأ حالة للبحث عن عنصر في شجرة بحث ثنائية متوازنة (AVL) تحتوي على n عقدة؟",
            optionsEn = listOf(
                "[A] O(1)",
                "[B] O(log n)",
                "[C] O(n)",
                "[D] O(n log n)"
            ),
            optionsAr = listOf(
                "[A] O(1)",
                "[B] O(log n)",
                "[C] O(n)",
                "[D] O(n log n)"
            ),
            expectedBracket = "[B]"
        ),
        PracticeQuestion(
            id = 2,
            categoryEn = "Mathematics • Calculus",
            categoryAr = "الرياضيات • التفاضل والتكامل",
            questionEn = "Evaluate the derivative f'(x) of the function f(x) = 3x² + 8x - 5 at x = 2:",
            questionAr = "احسب قيمة المشتقة f'(x) للدالة f(x) = 3x² + 8x - 5 عند النقطة x = 2:",
            optionsEn = listOf(
                "[A] 12",
                "[B] 15",
                "[C] 20",
                "[D] 24"
            ),
            optionsAr = listOf(
                "[A] 12",
                "[B] 15",
                "[C] 20",
                "[D] 24"
            ),
            expectedBracket = "[C]"
        ),
        PracticeQuestion(
            id = 3,
            categoryEn = "Physics • Electromagnetism",
            categoryAr = "الفيزياء • الكهرومغناطيسية",
            questionEn = "Which SI unit measures magnetic flux density (Magnetic Field B)?",
            questionAr = "ما هي وحدة النظام الدولي (SI) لقياس كثافة الفيض المغناطيسي (شدة المجال المغناطيسي B)؟",
            optionsEn = listOf(
                "[A] Weber (Wb)",
                "[B] Tesla (T)",
                "[C] Henry (H)",
                "[D] Farad (F)"
            ),
            optionsAr = listOf(
                "[A] ويبر (Wb)",
                "[B] تسلا (T)",
                "[C] هنري (H)",
                "[D] فاراد (F)"
            ),
            expectedBracket = "[B]"
        ),
        PracticeQuestion(
            id = 4,
            categoryEn = "Chemistry • Atomic Structure",
            categoryAr = "الكيمياء • البنية الذرية",
            questionEn = "Which element has the highest electronegativity on the Pauling scale?",
            questionAr = "أي عنصر كيميائي يمتلك أعلى كهروسلبية على مقياس باولنج؟",
            optionsEn = listOf(
                "[A] Oxygen (O)",
                "[B] Chlorine (Cl)",
                "[C] Nitrogen (N)",
                "[D] Fluorine (F)"
            ),
            optionsAr = listOf(
                "[A] الأكسجين (O)",
                "[B] الكلور (Cl)",
                "[C] النيتروجين (N)",
                "[D] الفلور (F)"
            ),
            expectedBracket = "[D]"
        )
    )
}
