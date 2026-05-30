package btm.m.edgeflow.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

object AppLocale {
    @Composable
    fun isChinese(): Boolean {
        val lang = LocalConfiguration.current.locales[0]?.language ?: "en"
        return lang == "zh"
    }
    @Composable
    fun strings(): AppStrings = if (isChinese()) ChineseStrings else EnglishStrings
}

interface AppStrings {
    val edgeflow: String; val globalEdgeSidebar: String
    val permissionsService: String; val rootActive: String; val rootActiveDesc: String
    val shizukuConnected: String; val shizukuDesc: String; val noPrivilege: String; val noPrivilegeDesc: String
    val requestShizuku: String; val mediaNotificationRequired: String; val mediaNotificationDesc: String
    val grantAccess: String; val mediaNotificationGranted: String
    val overlayPermissionRequired: String; val overlayPermissionDesc: String; val grantPermission: String
    val startEdgeListener: String; val stopEdgeListener: String
    val editFrequentApps: String; val editFrequentAppsDesc: String
    val triggerInteraction: String; val triggerInteractionDesc: String
    val customQuickLinks: String; val customQuickLinksDesc: String
    val cardLayout: String; val cardLayoutDesc: String
    val globalPanel: String; val globalBlurRadius: String; val panelTransparency: String; val sidebarWidth: String
    val headerImage: String; val customHeaderImage: String; val tapToSelectImage: String; val imageSet: String
    val backgroundImage: String; val tapToSelectBg: String; val clearBackground: String
    val bgOpacity: String; val bgBlur: String; val language: String
    val serviceRunning: String; val serviceStopped: String; val resetToDefault: String; val swipeLeftOrTap: String
    val triggerMethod: String; val directSwipe: String; val doubleSwipe: String; val floatingBall: String
    val edgePosition: String; val leftEdge: String; val rightEdge: String
    val hapticFeedback: String; val fullscreenImmersive: String; val advancedAreaSetup: String
    val highlightTriggerZone: String; val highlightTriggerDesc: String
    val triggerZoneWidth: String; val zoneHeight: String; val yOffset: String
    val swipeSensitivity: String; val sensitivityDesc: String
    val customQuickLinksTitle: String; val noCustomLinksYet: String; val tapPlusToAdd: String
    val addQuickLink: String; val title: String; val url: String; val add: String; val cancel: String
    val deleted: String; val added: String; val addApp: String
    val about: String; val developedBy: String; val appDescription: String
    val githubPage: String; val officialSite: String; val btmMSite: String; val btmMBlog: String
    val appInfo: String; val links: String
}

object EnglishStrings : AppStrings {
    override val edgeflow = "EdgeFlow"
    override val globalEdgeSidebar = "Global edge sidebar launcher"
    override val permissionsService = "Permissions & Service"
    override val rootActive = "Root Active"
    override val rootActiveDesc = "Superuser privileges active. System controls are fully functional."
    override val shizukuConnected = "Shizuku Connected"
    override val shizukuDesc = "ADB-level privileges via Shizuku."
    override val noPrivilege = "No Privilege"
    override val noPrivilegeDesc = "System controls (Wi-Fi, Bluetooth, etc.) require Root or Shizuku."
    override val requestShizuku = "Request Shizuku Authorization"
    override val mediaNotificationRequired = "Media notification access required"
    override val mediaNotificationDesc = "Required to display Now Playing info in sidebar"
    override val grantAccess = "Grant Access"
    override val mediaNotificationGranted = "Media notification access granted"
    override val overlayPermissionRequired = "Overlay permission required"
    override val overlayPermissionDesc = "EdgeFlow needs draw-over-other-apps permission."
    override val grantPermission = "Grant Permission"
    override val startEdgeListener = "Start EdgeFlow Service"
    override val stopEdgeListener = "Stop EdgeFlow Service"
    override val editFrequentApps = "Edit Frequent Apps"
    override val editFrequentAppsDesc = "Choose which apps appear in the sidebar grid"
    override val triggerInteraction = "Trigger & Interaction"
    override val triggerInteractionDesc = "Edge swipe method, haptics, position, immersive mode"
    override val customQuickLinks = "Custom Quick Links"
    override val customQuickLinksDesc = "Add custom shortcuts (URLs, app schemes) for the sidebar"
    override val cardLayout = "Card Layout Manager"
    override val cardLayoutDesc = "Reorder or hide sidebar cards. Header is always fixed at top."
    override val globalPanel = "Global Panel"
    override val globalBlurRadius = "Global Blur Radius"
    override val panelTransparency = "Panel Transparency"
    override val sidebarWidth = "Sidebar Width"
    override val headerImage = "Header Image"
    override val customHeaderImage = "Custom Header Image"
    override val tapToSelectImage = "Tap to select an image"
    override val imageSet = "Image set ✓ — tap to change"
    override val backgroundImage = "Background Image"
    override val tapToSelectBg = "Tap to select a background image"
    override val clearBackground = "Clear Background Image"
    override val bgOpacity = "Background Image Opacity"
    override val bgBlur = "Background Image Blur"
    override val language = "Language"
    override val serviceRunning = "EdgeFlow service is running. Swipe from the screen edge to open the sidebar."
    override val serviceStopped = "Tap 'Start EdgeFlow Service' above to activate the gesture."
    override val resetToDefault = "Reset to Default"
    override val swipeLeftOrTap = "Swipe left or tap right to close"
    override val triggerMethod = "Trigger Method"
    override val directSwipe = "Direct Swipe"
    override val doubleSwipe = "Double Swipe (anti-mistouch)"
    override val floatingBall = "Floating Ball"
    override val edgePosition = "Edge Position"
    override val leftEdge = "Left Edge"
    override val rightEdge = "Right Edge"
    override val hapticFeedback = "Haptic feedback on trigger"
    override val fullscreenImmersive = "100% fullscreen immersive"
    override val advancedAreaSetup = "Advanced Area Setup"
    override val highlightTriggerZone = "Highlight trigger zone (debug)"
    override val highlightTriggerDesc = "The edge trigger strip is now highlighted in semi-transparent red."
    override val triggerZoneWidth = "Trigger Zone Width"
    override val zoneHeight = "Zone Height"
    override val yOffset = "Y Offset"
    override val swipeSensitivity = "Swipe Sensitivity"
    override val sensitivityDesc = "Lower = more sensitive. 40px is recommended."
    override val customQuickLinksTitle = "Custom Quick Links"
    override val noCustomLinksYet = "No custom links yet"
    override val tapPlusToAdd = "Tap + to add a quick link"
    override val addQuickLink = "Add Quick Link"
    override val title = "Title"
    override val url = "URL (http:// or app scheme)"
    override val add = "Add"
    override val cancel = "Cancel"
    override val deleted = "Deleted"
    override val added = "Added"
    override val addApp = "Add App"
    override val about = "About"
    override val developedBy = "Developed by btm_m"
    override val appDescription = "A full-screen negative screen alternative built with Jetpack Compose + Kotlin + Material Design 3, utilizing Shizuku/Root for elevated privileges. Open-sourced under the GNU GPL v3.0 License."
    override val githubPage = "GitHub Repository"
    override val officialSite = "Official Website"
    override val btmMSite = "btm_m's Official Site"
    override val btmMBlog = "btm_m's Blog"
    override val appInfo = "App & System Info"
    override val links = "Developer Links"
}

object ChineseStrings : AppStrings {
    override val edgeflow = "EdgeFlow"
    override val globalEdgeSidebar = "全局边缘侧边栏启动器"
    override val permissionsService = "权限与服务"
    override val rootActive = "Root 已激活"
    override val rootActiveDesc = "超级用户权限已激活，系统控制功能完全可用。"
    override val shizukuConnected = "Shizuku 已连接"
    override val shizukuDesc = "通过 Shizuku 获得 ADB 级权限。"
    override val noPrivilege = "无权限"
    override val noPrivilegeDesc = "系统控制（Wi-Fi、蓝牙等）需要 Root 或 Shizuku 权限。"
    override val requestShizuku = "请求 Shizuku 授权"
    override val mediaNotificationRequired = "需要媒体通知权限"
    override val mediaNotificationDesc = "需要此权限以在侧边栏显示正在播放的音乐"
    override val grantAccess = "授权"
    override val mediaNotificationGranted = "媒体通知权限已授予"
    override val overlayPermissionRequired = "需要悬浮窗权限"
    override val overlayPermissionDesc = "EdgeFlow 需要显示在其他应用上方的权限。"
    override val grantPermission = "授予权限"
    override val startEdgeListener = "启动 EdgeFlow 服务"
    override val stopEdgeListener = "停止 EdgeFlow 服务"
    override val editFrequentApps = "编辑常用应用"
    override val editFrequentAppsDesc = "选择在侧边栏网格中显示的应用"
    override val triggerInteraction = "触发与交互设置"
    override val triggerInteractionDesc = "边缘滑动方式、触觉反馈、位置、沉浸模式"
    override val customQuickLinks = "自定义快捷链接"
    override val customQuickLinksDesc = "添加自定义快捷方式（URL、应用 Scheme）到侧边栏"
    override val cardLayout = "卡片布局管理"
    override val cardLayoutDesc = "重排或隐藏侧边栏卡片。头部图片始终固定在顶部。"
    override val globalPanel = "全局面板"
    override val globalBlurRadius = "全局模糊半径"
    override val panelTransparency = "面板透明度"
    override val sidebarWidth = "侧边栏宽度"
    override val headerImage = "头部图片"
    override val customHeaderImage = "自定义头部图片"
    override val tapToSelectImage = "点击选择图片"
    override val imageSet = "已设置图片 ✓ — 点击更换"
    override val backgroundImage = "背景图片"
    override val tapToSelectBg = "点击选择背景图片"
    override val clearBackground = "清除背景图片"
    override val bgOpacity = "背景图片透明度"
    override val bgBlur = "背景图片模糊度"
    override val language = "语言"
    override val serviceRunning = "EdgeFlow 服务运行中。从屏幕边缘滑动即可打开侧边栏。"
    override val serviceStopped = "点击上方「启动 EdgeFlow 服务」以激活手势。"
    override val resetToDefault = "恢复默认"
    override val swipeLeftOrTap = "左滑或点击右侧关闭"
    override val triggerMethod = "触发方式"
    override val directSwipe = "直接滑动"
    override val doubleSwipe = "二次滑动（防误触）"
    override val floatingBall = "悬浮球"
    override val edgePosition = "边缘位置"
    override val leftEdge = "左侧边缘"
    override val rightEdge = "右侧边缘"
    override val hapticFeedback = "触发时震动反馈"
    override val fullscreenImmersive = "100% 全屏沉浸"
    override val advancedAreaSetup = "高级区域调整"
    override val highlightTriggerZone = "高亮触发区域（调试）"
    override val highlightTriggerDesc = "边缘触发区域已用半透明红色高亮显示。"
    override val triggerZoneWidth = "触发区域宽度"
    override val zoneHeight = "区域高度"
    override val yOffset = "Y轴偏移"
    override val swipeSensitivity = "滑动灵敏度"
    override val sensitivityDesc = "数值越低越灵敏。推荐 40px。"
    override val customQuickLinksTitle = "自定义快捷链接"
    override val noCustomLinksYet = "暂无自定义链接"
    override val tapPlusToAdd = "点击 + 添加快捷链接"
    override val addQuickLink = "添加快捷链接"
    override val title = "标题"
    override val url = "URL（http:// 或应用 Scheme）"
    override val add = "添加"
    override val cancel = "取消"
    override val deleted = "已删除"
    override val added = "已添加"
    override val addApp = "添加应用"
    override val about = "关于"
    override val developedBy = "由 btm_m 开发"
    override val appDescription = "一个基于 Jetpack Compose + Kotlin + Material Design 3，利用 Shizuku 提权 / Root 权限实现类全界面负一屏的应用。本项目基于 GNU GPL v3.0 协议开源。"
    override val githubPage = "本项目的 GitHub 页面"
    override val officialSite = "应用官网"
    override val btmMSite = "btm_m 官方网站"
    override val btmMBlog = "btm_m 的博客"
    override val appInfo = "应用与系统信息"
    override val links = "开发者链接"
}
