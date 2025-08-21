# Widget Error Handling Implementation

This document describes the comprehensive error handling system implemented for the QuickWorkTime Android widget.

## Overview

The error handling system provides robust error recovery, retry mechanisms, and graceful degradation for widget operations. It consists of three main components:

1. **WidgetErrorHandler** - Centralized error handling and retry logic
2. **WidgetStateCache** - Caching system for fallback states during errors
3. **Enhanced Widget Components** - Updated widget classes with error handling integration

## Components

### 1. WidgetErrorHandler

**Location**: `app/src/main/java/com/example/quickworktime/widget/WidgetErrorHandler.kt`

**Features**:
- Automatic retry with exponential backoff
- Error classification (Database, Network, Update, Unknown)
- Configurable retry policies
- Error state creation for UI display
- Intelligent cache usage decisions

**Key Methods**:
```kotlin
suspend fun <T> executeWithRetry(
    operation: suspend () -> T,
    maxRetries: Int = 3,
    retryDelay: Long = 1000L,
    useExponentialBackoff: Boolean = true
): WidgetResult<T>

fun createErrorWidgetState(error: WidgetError): WidgetDisplayState
fun shouldUseCachedState(error: WidgetError, cachedState: WidgetDisplayState?): Boolean
```

**Error Types**:
- `DatabaseError` - Database access failures (retryable)
- `NetworkError` - Network connectivity issues (retryable)
- `UpdateError` - Widget update failures (non-retryable)
- `UnknownError` - Unexpected errors (retryable)

### 2. WidgetStateCache

**Location**: `app/src/main/java/com/example/quickworktime/widget/WidgetStateCache.kt`

**Features**:
- Persistent state caching using SharedPreferences
- Automatic cache expiration (5 minutes)
- JSON serialization for complex state objects
- Graceful error handling for cache operations

**Key Methods**:
```kotlin
suspend fun saveLastGoodState(state: WidgetDisplayState)
suspend fun getLastGoodState(): WidgetDisplayState?
suspend fun clearCache()
suspend fun hasCachedState(): Boolean
suspend fun getCacheAge(): Long
```

### 3. Enhanced Widget Components

**Updated Files**:
- `WidgetRepository.kt` - Database operations with error handling
- `WorkTimeWidgetProvider.kt` - Widget display with error states
- `WidgetUpdateService.kt` - Background operations with error recovery

## Error Handling Flow

### 1. Database Operations
```kotlin
// Before (basic error handling)
suspend fun getTodayWorkInfo(): WorkInfo? {
    try {
        return workInfoDao.getWorkInfoByDate(today)
    } catch (e: Exception) {
        return null
    }
}

// After (comprehensive error handling)
suspend fun getTodayWorkInfo(): WidgetResult<WorkInfo?> {
    return errorHandler.executeWithRetry {
        val today = LocalDate.now().format(dateFormatter)
        workInfoDao.getWorkInfoByDate(today)
    }
}
```

### 2. Widget Updates
```kotlin
// Enhanced widget update with retry and caching
suspend fun getWidgetDisplayState(context: Context): WidgetResult<WidgetDisplayState> {
    val result = errorHandler.executeWithRetry {
        // Attempt to get fresh data
        val todayWorkInfo = getTodayWorkInfoOrNull()
        // ... create display state
    }
    
    return when (result) {
        is WidgetResult.Success -> {
            // Cache successful state
            stateCache.saveLastGoodState(result.data)
            result
        }
        is WidgetResult.Error -> {
            // Try to use cached state if appropriate
            val cachedState = stateCache.getLastGoodState()
            if (errorHandler.shouldUseCachedState(result.error, cachedState)) {
                WidgetResult.Success(cachedState!!)
            } else {
                result // Return error state
            }
        }
    }
}
```

### 3. Error State Display

The widget can display different error states:

- **Database Error**: Shows "DB エラー" with retry button
- **Network Error**: Shows "接続エラー" with retry button  
- **Update Error**: Shows "更新エラー" with retry button
- **Unknown Error**: Shows "エラー" with retry button

### 4. Retry Mechanism

**Retry Policies**:
- Database errors: Up to 3 retries with exponential backoff
- Network errors: Up to 3 retries with exponential backoff
- Update errors: No retries (permanent failures)
- Widget updates: Up to 2 retries (fewer for UI operations)

**Backoff Strategy**:
- Initial delay: 1000ms
- Exponential multiplier: 2x
- Example: 1s → 2s → 4s

## User Experience

### Normal Operation
1. Widget displays current work time data
2. User interactions work smoothly
3. Data updates automatically

### Error Scenarios

#### Temporary Database Error
1. Error occurs during data fetch
2. System retries automatically (up to 3 times)
3. If retries succeed: Normal display continues
4. If retries fail: Shows cached data if available, otherwise error state

#### Network Connectivity Issues
1. Error detected during network operation
2. System retries with exponential backoff
3. Falls back to cached state if available
4. Shows "接続エラー" if no cache available

#### Widget Update Failures
1. Error occurs during widget UI update
2. Limited retries (2 attempts max)
3. Falls back to previous widget state
4. User can manually retry via button

### Error Recovery Actions

#### Automatic Recovery
- Retry mechanisms for temporary errors
- Cached state fallback
- Graceful degradation

#### Manual Recovery
- Retry button in error states
- App launch for detailed troubleshooting
- Cache clearing on retry

## Configuration

### Retry Settings
```kotlin
// Default settings
const val MAX_RETRY_ATTEMPTS = 3
const val RETRY_DELAY_MS = 1000L
const val EXPONENTIAL_BACKOFF_MULTIPLIER = 2

// Widget-specific settings
val widgetUpdateRetries = 2  // Fewer retries for UI operations
val themeUpdateRetries = 1   // Single retry for theme changes
```

### Cache Settings
```kotlin
const val CACHE_VALIDITY_MS = 5 * 60 * 1000L  // 5 minutes
```

## Testing

### Error Handling Demo
Use `ErrorHandlingDemo.runErrorHandlingDemo(context)` to test:
- Successful operations
- Database errors with retry
- Permanent errors (no retry)
- Error state creation
- Cache usage decisions

### State Cache Demo
Use `ErrorHandlingDemo.runStateCacheDemo(context)` to test:
- State saving and retrieval
- Cache availability checks
- Cache age calculation
- Cache clearing

## Monitoring and Logging

### Log Tags
- `WidgetErrorHandler` - Error handling operations
- `WidgetRepository` - Repository-level errors
- `ErrorHandlingDemo` - Demo and testing logs

### Log Levels
- `INFO` - Successful operations, retry attempts
- `WARN` - Recoverable errors, cache usage
- `ERROR` - Permanent failures, critical errors

### Example Log Output
```
I/WidgetErrorHandler: Operation succeeded after 2 retries
W/WidgetRepository: Failed to get today's work info: データベースアクセスエラーが発生しました
I/WidgetRepository: Using cached state due to error: ネットワークエラーが発生しました
E/WidgetErrorHandler: Operation failed after 3 retries: データベースアクセスエラーが発生しました
```

## Best Practices

### Error Handling
1. Always use `executeWithRetry` for operations that might fail
2. Classify errors appropriately for correct retry behavior
3. Provide meaningful error messages in Japanese
4. Log errors for debugging but don't crash the widget

### Caching
1. Cache successful states immediately
2. Use cached states for temporary errors only
3. Clear cache when performing manual retries
4. Respect cache expiration times

### User Experience
1. Show error states clearly but not alarmingly
2. Provide retry options for recoverable errors
3. Maintain widget functionality even during errors
4. Use appropriate button text for different states

## Future Enhancements

### Potential Improvements
1. **Crash Reporting**: Integration with Firebase Crashlytics
2. **Metrics Collection**: Error rate and recovery success tracking
3. **Smart Retry**: Adaptive retry delays based on error patterns
4. **User Preferences**: Configurable retry behavior
5. **Offline Mode**: Enhanced offline functionality with local caching

### Monitoring Additions
1. **Error Rate Tracking**: Monitor error frequency over time
2. **Recovery Success Rate**: Track how often retries succeed
3. **Cache Hit Rate**: Monitor cache effectiveness
4. **User Impact Metrics**: Measure error impact on user experience