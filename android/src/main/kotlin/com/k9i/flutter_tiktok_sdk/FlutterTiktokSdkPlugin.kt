package com.k9i.flutter_tiktok_sdk

import android.app.Activity
import android.content.Intent
import androidx.annotation.NonNull
import com.bytedance.sdk.open.tiktok.TikTokOpenApiFactory
import com.bytedance.sdk.open.tiktok.TikTokOpenConfig
import com.bytedance.sdk.open.tiktok.api.TikTokOpenApi
import com.bytedance.sdk.open.tiktok.authorize.model.Authorization
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import com.tiktok.open.sdk.auth.AuthApi
import com.tiktok.open.sdk.auth.AuthRequest
import com.tiktok.open.sdk.auth.utils.PKCEUtils


/** FlutterTiktokSdkPlugin */
class FlutterTiktokSdkPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.NewIntentListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var tikTokOpenApi: AuthApi

  var activity: Activity? = null
  private var activityPluginBinding: ActivityPluginBinding? = null
  private var loginResult: Result? = null
  private var clientKey: String? = null

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.k9i/flutter_tiktok_sdk")
    channel.setMethodCallHandler(this)
  }


  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    println(call.method);
    when (call.method) {
      "setup" -> {
        val activity = activity
        if (activity == null) {
          result.error(
                  "no_activity_found",
                  "There is no valid Activity found to present TikTok SDK Login screen.",
                  null
          )
          return
        }

        clientKey = call.argument<String?>("clientKey")
        tikTokOpenApi = AuthApi(activity = activity)
        result.success(null)
      }

      "login" -> {
        val scope = call.argument<String>("scope")
        val state = call.argument<String>("state")
        val redirectUrl = call.argument<String>("redirectUri") ?: ""
        var browserAuthEnabled = call.argument<Boolean>("browserAuthEnabled")

        val codeVerifier = PKCEUtils.generateCodeVerifier()

        val request = AuthRequest(
                clientKey = clientKey ?: "",
                scope = scope ?: "",
                redirectUri = redirectUrl,
                state = state,
                codeVerifier = codeVerifier,
        )
//        val authType = if (browserAuthEnabled == true) {
//          AuthApi.AuthMethod.ChromeTab
//        } else {
//          AuthApi.AuthMethod.TikTokApp
//        }
        var authType = AuthApi.AuthMethod.TikTokApp
        //request.callerLocalEntry = "com.k9i.flutter_tiktok_sdk.TikTokEntryActivity"

        tikTokOpenApi.authorize(request, authType)
        loginResult = result
      }

      else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    bindActivityBinding(binding)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    unbindActivityBinding()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    bindActivityBinding(binding)
  }

  override fun onDetachedFromActivity() {
    unbindActivityBinding()
  }

  private fun bindActivityBinding(binding: ActivityPluginBinding) {
    activity = binding.activity
    activityPluginBinding = binding
    binding.addOnNewIntentListener(this);
  }

  private fun unbindActivityBinding() {
    activityPluginBinding?.removeOnNewIntentListener(this)
    activity = null
    activityPluginBinding = null
  }

  override fun onNewIntent(intent: Intent): Boolean {
    val isSuccess = intent.getBooleanExtra(TikTokEntryActivity.TIKTOK_LOGIN_RESULT_SUCCESS, false)
    if (isSuccess) {
      // Returns an authentication code upon successful authentication
      val resultMap = mapOf(
        "authCode" to intent.getStringExtra(TikTokEntryActivity.TIKTOK_LOGIN_RESULT_AUTH_CODE),
        "state" to intent.getStringExtra(TikTokEntryActivity.TIKTOK_LOGIN_RESULT_STATE),
        "grantedPermissions" to intent.getStringExtra(TikTokEntryActivity.TIKTOK_LOGIN_RESULT_GRANTED_PERMISSIONS),
      )
      loginResult?.success(resultMap)
    } else {
      // Returns an error if authentication fails
      val errorCode = intent.getIntExtra(TikTokEntryActivity.TIKTOK_LOGIN_RESULT_ERROR_CODE, -999)
      val errorMessage = intent.getStringExtra(TikTokEntryActivity.TIKTOK_LOGIN_RESULT_ERROR_MSG);
      loginResult?.error(
              errorCode.toString(),
              errorMessage,
        null,
      )
    }
    loginResult = null
    return true
  }
}
