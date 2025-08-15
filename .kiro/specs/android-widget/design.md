# Design Document

## Overview

QuickWorkTimeアプリにAndroidホーム画面ウィジェット機能を追加します。ウィジェットはApp Widget Providerパターンを使用して実装し、既存のRoomデータベースと連携して勤務時間の記録・表示を行います。ウィジェットは退勤時間の入力に特化し、現在時刻から最適な退勤時間を自動計算してデータベースに記録する機能を提供します。UIは2x1または3x1サイズのコンパクトなレイアウトで、退勤ボタンと終了時刻表示を含みます。

## Architecture

### Component Structure
```
Widget Components:
├── WorkTimeWidgetProvider (AppWidgetProvider)
├── WorkTimeWidgetService (RemoteViewsService) 
├── WorkTimeWidgetConfigureActivity (Configuration Activity)
├── WidgetRepository (Data Access Layer)
└── WidgetUpdateReceiver (Broadcast Receiver)
```

### Data Flow
1. ウィジェットプロバイダーがシステムからの更新要求を受信
2. WidgetRepositoryを通じてRoomデータベースから最新の勤務データを取得
3. RemoteViewsを使用してウィジェットUIを構築・更新
4. ユーザーアクションはPendingIntentを通じてアプリまたはサービスに送信

## Components and Interfaces

### 1. WorkTimeWidgetProvider
```kotlin
class WorkTimeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray)
    override fun onReceive(context: Context, intent: Intent?)
    override fun onEnabled(context: Context)
    override fun onDisabled(context: Context)
}
```

**責任:**
- システムからのウィジェット更新要求の処理
- ユーザーアクション（退勤ボタン）の処理
- ウィジェットの有効化・無効化の管理

### 2. WidgetRepository
```kotlin
class WidgetRepository(private val workInfoDao: WorkInfoDao) {
    suspend fun getTodayWorkInfo(): WorkInfo?
    suspend fun calculateOptimalClockOutTime(): String
    suspend fun adjustTime(currentTime: String, hourDelta: Int, minuteDelta: Int): String
    suspend fun recordClockOut(time: String)
    suspend fun updateWidget(context: Context)
}
```

**責任:**
- 既存のRoomデータベースとの連携
- 現在時刻から5分単位切り下げの退勤時間計算
- フリック操作による時間調整処理
- 退勤データの記録・更新
- ウィジェット更新のトリガー

### 3. WidgetUpdateService
```kotlin
class WidgetUpdateService : IntentService("WidgetUpdateService") {
    override fun onHandleIntent(intent: Intent?)
}
```

**責任:**
- バックグラウンドでの退勤データ更新処理
- 最適な退勤時間の計算処理
- データベース操作の非同期実行

### 4. Widget Layout Components
- **widget_layout.xml**: メインウィジェットレイアウト（3x1サイズ）
- **widget_layout_small.xml**: 小サイズウィジェットレイアウト（2x1サイズ）

### 5. Time Adjustment Handler
```kotlin
class TimeAdjustmentHandler {
    fun handleFlickGesture(direction: FlickDirection, timeComponent: TimeComponent): String
    fun calculateRoundedTime(currentTime: LocalTime): String
}
```

**責任:**
- フリック操作の検出と処理
- 時間・分の調整ロジック
- 5分単位での時間丸め処理

## Data Models

### WidgetData
```kotlin
data class WidgetData(
    val date: String,
    val endTime: String?,
    val hasRecord: Boolean,
    val displayText: String,
    val calculatedEndTime: String?,
    val isAdjusted: Boolean = false
)
```

### Time Calculation Logic
```kotlin
// 現在時刻を5分単位で切り下げ
// 例: 18:17 → 18:15, 18:24 → 18:20
fun roundDownToFiveMinutes(time: LocalTime): LocalTime {
    val minutes = time.minute
    val roundedMinutes = (minutes / 5) * 5
    return time.withMinute(roundedMinutes).withSecond(0).withNano(0)
}
```

### Widget State Management
- **NO_RECORD**: 本日の記録なし（計算された退勤時間を表示）
- **HAS_RECORD**: 本日の退勤記録あり（記録済み時刻を表示）
- **ADJUSTING**: ユーザーが時間を調整中（調整後の時間を表示）

### Flick Gesture Handling
```kotlin
enum class FlickDirection { UP, DOWN }
enum class TimeComponent { HOUR, MINUTE }

// フリック操作による時間調整
// 分: ±5分, 時: ±1時間
fun adjustTime(baseTime: String, component: TimeComponent, direction: FlickDirection): String
```

## Error Handling

### Database Access Errors
- データベース接続エラー時はキャッシュされた最後の状態を表示
- エラー状態を示すUI表示（「データ取得エラー」など）

### Widget Update Failures
- 更新失敗時は前回の表示状態を維持
- リトライ機構の実装（最大3回）

### Permission Errors
- 必要な権限が不足している場合の適切なエラーハンドリング
- ユーザーに権限要求を促すメッセージ表示

## Testing Strategy

### Unit Tests
- `WidgetRepository`のデータアクセスロジック
- 時間計算ロジック
- ウィジェット状態管理ロジック

### Integration Tests
- ウィジェットプロバイダーとデータベースの連携
- PendingIntentの動作確認
- RemoteViews更新の検証

### UI Tests
- ウィジェット配置・削除のテスト
- 退勤ボタンタップ動作の確認
- フリック操作による時間調整の確認
- 異なる画面サイズでの表示テスト
- 時間計算ロジックの検証（5分単位切り下げ）

## Implementation Details

### File Structure
```
app/src/main/java/com/example/quickworktime/
├── widget/
│   ├── WorkTimeWidgetProvider.kt
│   ├── WidgetRepository.kt
│   ├── WidgetUpdateService.kt
│   ├── TimeAdjustmentHandler.kt
│   └── WidgetConfigureActivity.kt
├── res/
│   ├── layout/
│   │   ├── widget_layout.xml
│   │   └── widget_layout_small.xml
│   ├── xml/
│   │   └── work_time_widget_info.xml
│   └── drawable/
│       └── widget_background.xml
```

### AndroidManifest.xml Updates
- AppWidgetProviderのレシーバー登録
- 必要な権限の追加
- ウィジェット設定アクティビティの登録

### Dependencies
- 既存のRoom データベース（WorkInfo, WorkInfoDao）
- 既存のテーマとカラーリソース
- AndroidX Widget ライブラリ

### Performance Considerations
- ウィジェット更新頻度の最適化（必要時のみ更新）
- バックグラウンド処理の効率化
- メモリ使用量の最小化

### Security Considerations
- PendingIntentのセキュリティフラグ設定
- データベースアクセスの適切な権限管理
- 外部からの不正なIntent受信の防止